package com.github.wikor2115.reservation.availability.domain;

public class AvailabilityCapacityExceededException extends IllegalArgumentException {
    public AvailabilityCapacityExceededException(int capacity) {
        super("Reservation would exceed capacity of " + capacity);
    }
}
