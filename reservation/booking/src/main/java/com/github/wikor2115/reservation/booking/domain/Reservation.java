package com.github.wikor2115.reservation.booking.domain;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Entity
public class Reservation {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Long availabilitySlotId;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Long offerId;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String customerName;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String customerEmail;

    @Positive
    @Column(nullable = false)
    private int partySize;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime cancelledAt;

    protected Reservation() {}

    public static Reservation create(
            Long availabilitySlotId,
            Long offerId,
            String customerName,
            String customerEmail,
            int partySize
    ) {
        return create(availabilitySlotId, offerId, customerName, customerEmail, partySize, Clock.systemDefaultZone());
    }

    public static Reservation create(
            Long availabilitySlotId,
            Long offerId,
            String customerName,
            String customerEmail,
            int partySize,
            Clock clock
    ) {
        validateAvailabilitySlotId(availabilitySlotId);
        validateOfferId(offerId);
        validateCustomerName(customerName);
        validateCustomerEmail(customerEmail);
        validatePartySize(partySize);
        validateClock(clock);

        Reservation reservation = new Reservation();
        reservation.availabilitySlotId = availabilitySlotId;
        reservation.offerId = offerId;
        reservation.customerName = customerName.trim();
        reservation.customerEmail = customerEmail.trim();
        reservation.partySize = partySize;
        reservation.createdAt = LocalDateTime.now(clock);
        reservation.status = ReservationStatus.PENDING;
        reservation.cancelledAt = null;
        return reservation;
    }

    public void confirm() {
        ensurePending("Only pending reservation can be confirmed");
        this.status = ReservationStatus.CONFIRMED;
    }

    public void reject() {
        ensurePending("Only pending reservation can be rejected");
        this.status = ReservationStatus.REJECTED;
    }

    public void cancel() {
        cancel(Clock.systemDefaultZone());
    }

    public void cancel(Clock clock) {
        ensureCanBeCancelled();
        validateClock(clock);
        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now(clock);
    }

    private static void validateAvailabilitySlotId(Long availabilitySlotId) {
        if (availabilitySlotId == null)
            throw new IllegalArgumentException("Availability slot id must not be null");
        if (availabilitySlotId <= 0)
            throw new IllegalArgumentException("Availability slot id must be greater than 0");
    }

    private static void validateOfferId(Long offerId) {
        if (offerId == null)
            throw new IllegalArgumentException("Offer id must not be null");
        if (offerId <= 0)
            throw new IllegalArgumentException("Offer id must be greater than 0");
    }

    private static void validateCustomerName(String customerName) {
        if (customerName == null)
            throw new IllegalArgumentException("Customer name must not be null");
        if (customerName.trim().isEmpty())
            throw new IllegalArgumentException("Customer name must not be blank");
        if (customerName.trim().length() > 255)
            throw new IllegalArgumentException("Customer name must not exceed 255 characters");
    }

    private static void validateCustomerEmail(String customerEmail) {
        if (customerEmail == null)
            throw new IllegalArgumentException("Customer email must not be null");
        String trimmedEmail = customerEmail.trim();
        if (trimmedEmail.isEmpty())
            throw new IllegalArgumentException("Customer email must not be blank");
        if (trimmedEmail.length() > 255)
            throw new IllegalArgumentException("Customer email must not exceed 255 characters");
        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches())
            throw new IllegalArgumentException("Customer email must be valid");
    }

    private static void validatePartySize(int partySize) {
        if (partySize <= 0)
            throw new IllegalArgumentException("Party size must be greater than 0");
    }

    private static void validateClock(Clock clock) {
        if (clock == null)
            throw new IllegalArgumentException("Clock must not be null");
    }

    private void ensurePending(String message) {
        if (this.status != ReservationStatus.PENDING)
            throw new IllegalStateException(message);
    }

    private void ensureCanBeCancelled() {
        if (this.status == ReservationStatus.CANCELLED)
            throw new IllegalStateException("Reservation is cancelled");
        if (this.status == ReservationStatus.REJECTED)
            throw new IllegalStateException("Reservation is rejected");
    }

    public Long getId() {
        return this.id;
    }

    public Long getAvailabilitySlotId() {
        return this.availabilitySlotId;
    }

    public Long getOfferId() {
        return this.offerId;
    }

    public String getCustomerName() {
        return this.customerName;
    }

    public String getCustomerEmail() {
        return this.customerEmail;
    }

    public int getPartySize() {
        return this.partySize;
    }

    public ReservationStatus getStatus() {
        return this.status;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getCancelledAt() {
        return this.cancelledAt;
    }
}
