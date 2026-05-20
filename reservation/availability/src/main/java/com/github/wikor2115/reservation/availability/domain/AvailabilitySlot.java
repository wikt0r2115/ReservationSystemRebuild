package com.github.wikor2115.reservation.availability.domain;

import java.time.Clock;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

@Entity
public class AvailabilitySlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Long offerId;

    @NotNull
    private LocalDateTime startsAt;

    @NotNull
    private LocalDateTime endsAt;

    @Positive
    private int capacity;

    @PositiveOrZero
    private int reservedCount;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AvailabilityStatus status;

    protected AvailabilitySlot() {
    }

    public static AvailabilitySlot create(Long offerId, LocalDateTime startsAt, LocalDateTime endsAt, int capacity) {
        return create(offerId, startsAt, endsAt, capacity, Clock.systemDefaultZone());
    }

    public static AvailabilitySlot create(
            Long offerId,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            int capacity,
            Clock clock
    ) {
        AvailabilitySlot availabilitySlot = new AvailabilitySlot();
        validateOfferId(offerId);
        validateTime(startsAt, endsAt, clock);
        validateCapacity(capacity);

        availabilitySlot.offerId = offerId;
        availabilitySlot.startsAt = startsAt;
        availabilitySlot.endsAt = endsAt;
        availabilitySlot.capacity = capacity;
        availabilitySlot.reservedCount = 0;
        availabilitySlot.status = AvailabilityStatus.OPEN;
        return availabilitySlot;
    }

    public void changeTime(LocalDateTime startsAt, LocalDateTime endsAt) {
        changeTime(startsAt, endsAt, Clock.systemDefaultZone());
    }

    public void changeTime(LocalDateTime startsAt, LocalDateTime endsAt, Clock clock) {
        ensureNotCancelled();
        validateTime(startsAt, endsAt, clock);
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public void changeCapacity(int capacity) {
        ensureNotCancelled();
        validateCapacity(capacity);
        validateCapacityCanHoldReservedCount(capacity);
        this.capacity = capacity;
    }

    public void reserve(int partySize) {
        ensureOpen();
        validatePartySize(partySize);
        if (this.reservedCount + partySize > capacity)
            throw new IllegalArgumentException("Reservation would exceed capacity of " + this.capacity);
        reservedCount += partySize;
    }

    public void release(int partySize) {
        ensureNotCancelled();
        validatePartySize(partySize);
        if (reservedCount - partySize < 0)
            throw new IllegalArgumentException("Party size must not exceed reserved count");
        reservedCount -= partySize;
    }

    public void close() {
        ensureOpen();
        this.status = AvailabilityStatus.CLOSED;
    }

    public void reopen() {
        ensureClosed();
        this.status = AvailabilityStatus.OPEN;
    }

    public void cancel() {
        ensureNotCancelled();
        this.status = AvailabilityStatus.CANCELLED;
    }

    private static void validateOfferId(Long offerId) {
        if (offerId == null)
            throw new IllegalArgumentException("Offer id must not be null");
    }

    private static void validateTime(LocalDateTime startsAt, LocalDateTime endsAt, Clock clock) {
        validateClock(clock);
        if (startsAt == null)
            throw new IllegalArgumentException("Start date must not be null");
        if (endsAt == null)
            throw new IllegalArgumentException("End date must not be null");
        if (startsAt.isBefore(LocalDateTime.now(clock)))
            throw new IllegalArgumentException("Start date must not be in the past");
        if (!endsAt.isAfter(startsAt))
            throw new IllegalArgumentException("End date must be after start date");
    }

    private static void validateClock(Clock clock) {
        if (clock == null)
            throw new IllegalArgumentException("Clock must not be null");
    }

    private static void validateCapacity(int capacity) {
        if (capacity <= 0)
            throw new IllegalArgumentException("Capacity must be greater than 0");
    }

    private void validateCapacityCanHoldReservedCount(int capacity) {
        if (capacity < this.reservedCount)
            throw new IllegalArgumentException("Capacity must not be lower than reserved count");
    }

    private static void validatePartySize(int partySize) {
        if (partySize <= 0)
            throw new IllegalArgumentException("Party size must be greater than 0");
    }

    private void ensureNotCancelled() {
        if (this.status == AvailabilityStatus.CANCELLED)
            throw new IllegalStateException("Availability slot is cancelled");
    }

    private void ensureOpen() {
        if (this.status != AvailabilityStatus.OPEN)
            throw new IllegalStateException("Availability slot is not open");
    }

    private void ensureClosed() {
        if (this.status != AvailabilityStatus.CLOSED)
            throw new IllegalStateException("Only closed availability slot can be reopened");
    }

    public Long getId() {
        return this.id;
    }

    public Long getOfferId() {
        return this.offerId;
    }

    public LocalDateTime getStartsAt() {
        return this.startsAt;
    }

    public LocalDateTime getEndsAt() {
        return this.endsAt;
    }

    public int getCapacity() {
        return this.capacity;
    }

    public int getReservedCount() {
        return this.reservedCount;
    }

    public AvailabilityStatus getStatus() {
        return this.status;
    }
}
