package com.github.wikor2115.reservation.booking.api;

import java.time.LocalDateTime;

import com.github.wikor2115.reservation.booking.domain.ReservationStatus;

public record ReservationResponse(
        Long id,
        Long availabilitySlotId,
        Long offerId,
        String customerName,
        String customerEmail,
        int partySize,
        ReservationStatus status,
        LocalDateTime createdAt,
        LocalDateTime cancelledAt
) {}
