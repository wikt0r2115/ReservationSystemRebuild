package com.github.wikor2115.reservation.booking.service;

public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException(Long reservationId) {
        super("Reservation with id " + reservationId + " not found");
    }
}
