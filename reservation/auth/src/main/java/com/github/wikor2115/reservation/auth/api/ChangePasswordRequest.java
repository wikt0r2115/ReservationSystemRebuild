package com.github.wikor2115.reservation.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "currentPassword must not be blank")
        String currentPassword,

        @NotBlank(message = "newPassword must not be blank")
        @Size(min = 8, max = 72, message = "newPassword length must be between 8 and 72")
        String newPassword
) {}
