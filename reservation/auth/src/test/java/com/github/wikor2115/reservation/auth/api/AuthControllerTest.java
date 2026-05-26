package com.github.wikor2115.reservation.auth.api;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.github.wikor2115.reservation.auth.domain.UserAccount;
import com.github.wikor2115.reservation.auth.service.AuthService;
import com.github.wikor2115.reservation.auth.service.DuplicateUserAccountException;
import com.github.wikor2115.reservation.security.AuthenticatedUser;
import com.github.wikor2115.reservation.security.UserRole;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registerCustomer_validRequest_returnsCreatedCustomer() throws Exception {
        when(authService.registerCustomer("jan@example.com", "Jan Kowalski", "customer123"))
                .thenReturn(UserAccount.registerCustomer("jan@example.com", "Jan Kowalski", "{bcrypt}hash", CLOCK));

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "displayName": "Jan Kowalski",
                          "email": "jan@example.com",
                          "password": "customer123"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("jan@example.com"))
                .andExpect(jsonPath("$.displayName").value("Jan Kowalski"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void registerCustomer_invalidRequest_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "displayName": "",
                          "email": "invalid-email",
                          "password": "short"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", not(emptyOrNullString())));

        verifyNoInteractions(authService);
    }

    @Test
    void registerCustomer_whenEmailExists_returnsConflict() throws Exception {
        when(authService.registerCustomer("jan@example.com", "Jan Kowalski", "customer123"))
                .thenThrow(new DuplicateUserAccountException("jan@example.com"));

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "displayName": "Jan Kowalski",
                          "email": "jan@example.com",
                          "password": "customer123"
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_ACCOUNT_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.details[0].field").value("email"));
    }

    @Test
    void login_validRequest_returnsBearerToken() throws Exception {
        when(authService.login("jan@example.com", "customer123"))
                .thenReturn(AuthTokenResponse.bearer("jwt-token", 7200));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "jan@example.com",
                          "password": "customer123"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(7200));
    }

    @Test
    void changePassword_authenticatedUser_returnsNoContent() throws Exception {
        mockMvc.perform(post("/api/v1/auth/change-password")
                .with(authenticatedCustomer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "currentPassword": "customer123",
                          "newPassword": "customer456"
                        }
                        """))
                .andExpect(status().isNoContent());

        verify(authService).changePassword(
                new AuthenticatedUser(10L, "jan@example.com", UserRole.CUSTOMER),
                "customer123",
                "customer456");
    }

    @Test
    void changePassword_withoutAuthentication_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "currentPassword": "customer123",
                          "newPassword": "customer456"
                        }
                        """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.details[0].field").value("authorization"));

        verifyNoInteractions(authService);
    }

    private static RequestPostProcessor authenticatedCustomer() {
        return request -> {
            request.setUserPrincipal(new UsernamePasswordAuthenticationToken(
                    new AuthenticatedUser(10L, "jan@example.com", UserRole.CUSTOMER),
                    null));
            return request;
        };
    }
}
