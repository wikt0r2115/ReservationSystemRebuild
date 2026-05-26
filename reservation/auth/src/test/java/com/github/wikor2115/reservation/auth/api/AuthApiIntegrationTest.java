package com.github.wikor2115.reservation.auth.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.github.wikor2115.reservation.auth.repository.UserAccountRepository;
import com.github.wikor2115.reservation.security.UserRole;
import com.github.wikor2115.reservation.security.jwt.JwtTokenService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        userAccountRepository.deleteAll();
    }

    @Test
    void registerAndLogin_createsCustomerAndReturnsJwt() throws Exception {
        registerCustomer("JAN@example.com", "customer123")
                .andExpect(jsonPath("$.email").value("jan@example.com"))
                .andExpect(jsonPath("$.displayName").value("Jan Kowalski"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        String token = loginAndExtractToken("jan@example.com", "customer123");
        var authenticatedUser = jwtTokenService.parseToken(token);
        assertEquals("jan@example.com", authenticatedUser.email());
        assertEquals(UserRole.CUSTOMER, authenticatedUser.role());
    }

    @Test
    void login_whenPasswordInvalid_returnsUnauthorized() throws Exception {
        registerCustomer("jan@example.com", "customer123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "jan@example.com",
                          "password": "wrong-password"
                        }
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void changePassword_withValidToken_updatesPassword() throws Exception {
        registerCustomer("jan@example.com", "customer123");
        String token = loginAndExtractToken("jan@example.com", "customer123");

        mockMvc.perform(post("/api/v1/auth/change-password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "currentPassword": "customer123",
                          "newPassword": "customer456"
                        }
                        """))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "jan@example.com",
                          "password": "customer123"
                        }
                        """))
                .andExpect(status().isUnauthorized());

        loginAndExtractToken("jan@example.com", "customer456");
    }

    @Test
    void changePassword_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "currentPassword": "customer123",
                          "newPassword": "customer456"
                        }
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePassword_withInvalidToken_returnsJsonUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/change-password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "currentPassword": "customer123",
                          "newPassword": "customer456"
                        }
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_BEARER_TOKEN"))
                .andExpect(jsonPath("$.details[0].field").value("authorization"));
    }

    private org.springframework.test.web.servlet.ResultActions registerCustomer(String email, String password)
            throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "displayName": "Jan Kowalski",
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(email, password)))
                .andExpect(status().isCreated());
    }

    private String loginAndExtractToken(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return response.split("\"token\":\"")[1].split("\"")[0];
    }
}
