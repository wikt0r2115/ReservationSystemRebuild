package com.github.wikor2115.reservation.offer.api;

import java.math.BigDecimal;

public record OfferResponse(
    Long id,
    String name,
    String imageUrl,
    String description,
    BigDecimal price,
    boolean archived
) {}