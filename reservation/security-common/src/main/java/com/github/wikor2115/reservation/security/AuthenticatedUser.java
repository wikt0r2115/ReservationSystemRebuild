package com.github.wikor2115.reservation.security;

import java.util.Objects;

public record AuthenticatedUser(Long id, String email, UserRole role) {
    public AuthenticatedUser {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(role, "role must not be null");
        if (email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
    }
}
