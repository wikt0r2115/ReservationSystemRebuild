package com.github.wikor2115.reservation.offer.api;

import java.util.List;

public record ApiErrorResponse(
        String code,
        List<ApiFieldError> details
) {}
