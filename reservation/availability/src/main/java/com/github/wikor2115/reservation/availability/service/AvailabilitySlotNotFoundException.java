package com.github.wikor2115.reservation.availability.service;

public class AvailabilitySlotNotFoundException extends RuntimeException {
    public AvailabilitySlotNotFoundException(Long slotId) {
        super("Slot with id " + slotId + " not found");
    }
}
