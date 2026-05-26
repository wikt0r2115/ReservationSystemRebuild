package com.github.wikor2115.reservation.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.github.wikor2115.reservation.auth.api.AuthTokenResponse;
import com.github.wikor2115.reservation.auth.domain.UserAccount;
import com.github.wikor2115.reservation.auth.repository.UserAccountRepository;
import com.github.wikor2115.reservation.security.UserRole;
import com.github.wikor2115.reservation.security.jwt.JwtProperties;
import com.github.wikor2115.reservation.security.jwt.JwtTokenService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);
    private static final JwtProperties JWT_PROPERTIES = new JwtProperties(
            "test-secret-that-is-long-enough-for-hmac",
            "reservation-auth-test",
            Duration.ofHours(1));

    @Mock
    private UserAccountRepository userAccountRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userAccountRepository,
                passwordEncoder,
                new JwtTokenService(JWT_PROPERTIES, CLOCK),
                JWT_PROPERTIES,
                CLOCK);
    }

    @Test
    void registerCustomer_hashesPasswordAndSavesCustomer() {
        when(userAccountRepository.existsByEmailIgnoreCase("jan@example.com")).thenReturn(false);
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserAccount account = authService.registerCustomer(
                " JAN@example.COM ",
                " Jan Kowalski ",
                "customer123");

        assertEquals("jan@example.com", account.getEmail());
        assertEquals("Jan Kowalski", account.getDisplayName());
        assertEquals(UserRole.CUSTOMER, account.getRole());
        assertNotEquals("customer123", account.getPasswordHash());

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(captor.capture());
        assertEquals("jan@example.com", captor.getValue().getEmail());
    }

    @Test
    void registerCustomer_whenEmailExists_throwsDuplicateUserAccountException() {
        when(userAccountRepository.existsByEmailIgnoreCase("jan@example.com")).thenReturn(true);

        assertThrows(DuplicateUserAccountException.class,
                () -> authService.registerCustomer("jan@example.com", "Jan", "customer123"));

        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void login_whenPasswordMatches_returnsBearerToken() {
        UserAccount account = UserAccount.registerCustomer(
                "jan@example.com",
                "Jan",
                passwordEncoder.encode("customer123"),
                CLOCK);
        setId(account, 10L);
        when(userAccountRepository.findByEmailIgnoreCase("jan@example.com")).thenReturn(Optional.of(account));

        AuthTokenResponse response = authService.login(" JAN@example.COM ", "customer123");

        assertEquals("Bearer", response.tokenType());
        assertEquals(3600, response.expiresInSeconds());
    }

    @Test
    void login_whenPasswordDoesNotMatch_throwsInvalidCredentialsException() {
        UserAccount account = UserAccount.registerCustomer(
                "jan@example.com",
                "Jan",
                passwordEncoder.encode("customer123"),
                CLOCK);
        when(userAccountRepository.findByEmailIgnoreCase("jan@example.com")).thenReturn(Optional.of(account));

        assertThrows(InvalidCredentialsException.class, () -> authService.login("jan@example.com", "wrong-password"));
    }

    @Test
    void login_whenAccountDisabled_throwsInvalidCredentialsException() {
        UserAccount account = UserAccount.registerCustomer(
                "jan@example.com",
                "Jan",
                passwordEncoder.encode("customer123"),
                CLOCK);
        account.disable();
        when(userAccountRepository.findByEmailIgnoreCase("jan@example.com")).thenReturn(Optional.of(account));

        assertThrows(InvalidCredentialsException.class, () -> authService.login("jan@example.com", "customer123"));
    }

    private static void setId(UserAccount account, Long id) {
        try {
            var field = UserAccount.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(account, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
