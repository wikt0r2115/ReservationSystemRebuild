package com.github.wikor2115.reservation.auth.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

import com.github.wikor2115.reservation.security.UserRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "auth_user",
        uniqueConstraints = @UniqueConstraint(name = "uq_auth_user_email", columnNames = "email")
)
public class UserAccount {
    private static final int MAX_DISPLAY_NAME_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "display_name", nullable = false, length = MAX_DISPLAY_NAME_LENGTH)
    private String displayName;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserAccountStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserAccount() {
    }

    private UserAccount(
            String email,
            String displayName,
            String passwordHash,
            UserRole role,
            Clock clock
    ) {
        this.email = normalizeEmail(email);
        this.displayName = normalizeDisplayName(displayName);
        this.passwordHash = requireText(passwordHash, "passwordHash");
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.status = UserAccountStatus.ACTIVE;
        this.createdAt = Instant.now(Objects.requireNonNull(clock, "clock must not be null"));
    }

    public static UserAccount registerCustomer(
            String email,
            String displayName,
            String passwordHash,
            Clock clock
    ) {
        return new UserAccount(email, displayName, passwordHash, UserRole.CUSTOMER, clock);
    }

    public static UserAccount registerAdmin(
            String email,
            String displayName,
            String passwordHash,
            Clock clock
    ) {
        return new UserAccount(email, displayName, passwordHash, UserRole.ADMIN, clock);
    }

    public void disable() {
        if (status == UserAccountStatus.DISABLED) {
            throw new IllegalStateException("User account is already disabled");
        }
        status = UserAccountStatus.DISABLED;
    }

    public boolean isActive() {
        return status == UserAccountStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public UserAccountStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static String normalizeEmail(String value) {
        String email = requireText(value, "email").toLowerCase(Locale.ROOT);
        if (!email.contains("@")) {
            throw new IllegalArgumentException("email must be valid");
        }
        return email;
    }

    private static String normalizeDisplayName(String value) {
        String displayName = requireText(value, "displayName");
        if (displayName.length() < 2 || displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new IllegalArgumentException("displayName length must be between 2 and 100");
        }
        return displayName;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
