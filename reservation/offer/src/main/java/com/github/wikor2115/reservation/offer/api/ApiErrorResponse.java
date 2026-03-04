package com.github.wikor2115.reservation.offer.api;

public record ApiErrorResponse(
        String error,
        String message
) {
}
