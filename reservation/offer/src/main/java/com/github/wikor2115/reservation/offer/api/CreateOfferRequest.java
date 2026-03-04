package com.github.wikor2115.reservation.offer.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;

public record CreateOfferRequest(
	@NotBlank @Size(max = 255) String name,
	@NotBlank @Size(max = 2048) @URL String imageUrl,
	@NotBlank @Size(max = 2048) String description,
	@NotNull @Positive BigDecimal price 
) {}
