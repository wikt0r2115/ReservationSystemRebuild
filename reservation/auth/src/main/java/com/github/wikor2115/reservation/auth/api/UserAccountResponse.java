package com.github.wikor2115.reservation.auth.api;

import com.github.wikor2115.reservation.auth.domain.UserAccount;
import com.github.wikor2115.reservation.auth.domain.UserAccountStatus;
import com.github.wikor2115.reservation.security.UserRole;

public record UserAccountResponse(
        Long id,
        String email,
        String displayName,
        UserRole role,
        UserAccountStatus status
) {
    public static UserAccountResponse from(UserAccount account) {
        return new UserAccountResponse(
                account.getId(),
                account.getEmail(),
                account.getDisplayName(),
                account.getRole(),
                account.getStatus());
    }
}
