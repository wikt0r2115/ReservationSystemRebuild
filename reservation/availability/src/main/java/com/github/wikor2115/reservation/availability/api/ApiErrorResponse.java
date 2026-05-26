package com.github.wikor2115.reservation.availability.api;

import java.util.List;

public record ApiErrorResponse(
        String code,
        String message,
        List<ApiFieldError> details
) {}
