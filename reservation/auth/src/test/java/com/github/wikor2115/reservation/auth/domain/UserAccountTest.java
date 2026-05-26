package com.github.wikor2115.reservation.auth.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.github.wikor2115.reservation.security.UserRole;

class UserAccountTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void registerCustomer_createsActiveCustomerAccount() {
        UserAccount account = UserAccount.registerCustomer(
                " JAN@example.COM ",
                " Jan Kowalski ",
                "{bcrypt}hash",
                CLOCK);

        assertEquals("jan@example.com", account.getEmail());
        assertEquals("Jan Kowalski", account.getDisplayName());
        assertEquals("{bcrypt}hash", account.getPasswordHash());
        assertEquals(UserRole.CUSTOMER, account.getRole());
        assertEquals(UserAccountStatus.ACTIVE, account.getStatus());
        assertEquals(Instant.parse("2026-06-01T10:00:00Z"), account.getCreatedAt());
        assertTrue(account.isActive());
    }

    @Test
    void registerAdmin_createsActiveAdminAccount() {
        UserAccount account = UserAccount.registerAdmin(
                "admin@example.com",
                "Admin",
                "{bcrypt}hash",
                CLOCK);

        assertEquals(UserRole.ADMIN, account.getRole());
        assertEquals(UserAccountStatus.ACTIVE, account.getStatus());
    }

    @Test
    void registerCustomer_whenEmailInvalid_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> UserAccount.registerCustomer("invalid-email", "Jan", "{bcrypt}hash", CLOCK));

        assertEquals("email must be valid", exception.getMessage());
    }

    @Test
    void disable_whenActive_disablesAccount() {
        UserAccount account = UserAccount.registerCustomer(
                "jan@example.com",
                "Jan",
                "{bcrypt}hash",
                CLOCK);

        account.disable();

        assertEquals(UserAccountStatus.DISABLED, account.getStatus());
    }

    @Test
    void disable_whenAlreadyDisabled_throwsException() {
        UserAccount account = UserAccount.registerCustomer(
                "jan@example.com",
                "Jan",
                "{bcrypt}hash",
                CLOCK);
        account.disable();

        IllegalStateException exception = assertThrows(IllegalStateException.class, account::disable);

        assertEquals("User account is already disabled", exception.getMessage());
    }
}
