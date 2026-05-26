package com.github.wikor2115.reservation.offer.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;

public record UpdateOfferRequest(
        @Size(max = 255) String name,
        @Size(max = 2048) @URL String imageUrl,
        @Size(max = 2048) String description,
        @Positive @Digits(integer = 5, fraction = 2) BigDecimal price
) {
    @AssertTrue(message = "At least one field must be provided")
    public boolean hasAnyFieldProvided() {
        return name != null || imageUrl != null || description != null || price != null;
    }
}
