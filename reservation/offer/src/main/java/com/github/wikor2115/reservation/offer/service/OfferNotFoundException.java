package com.github.wikor2115.reservation.offer.service;

public class OfferNotFoundException extends RuntimeException {
    public OfferNotFoundException(Long offerId) {
        super("Offer with id " + offerId + " not found");
    }
}
