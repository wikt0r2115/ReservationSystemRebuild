package com.github.wikor2115.reservation.availability.api;

import java.time.LocalDateTime;

import com.github.wikor2115.reservation.availability.domain.AvailabilityStatus;

public record AvailabilitySlotResponse(
        Long id,
        Long offerId,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        int capacity,
        int reservedCount,
        AvailabilityStatus status
) {}
