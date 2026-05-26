package com.github.wikor2115.reservation.availability.service;

public class DuplicateAvailabilitySlotException extends RuntimeException {
    public DuplicateAvailabilitySlotException() {
        super("Availability slot already exists for this offer and time range");
    }
}
