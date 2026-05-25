package com.github.wikor2115.reservation.availability.api;

import java.time.LocalDateTime;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;

public record UpdateAvailabilitySlotRequest(
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        @Positive Integer capacity
) {
    @AssertTrue(message = "At least one field must be provided")
    public boolean hasAnyFieldProvided() {
        return startsAt != null || endsAt != null || capacity != null;
    }
}
