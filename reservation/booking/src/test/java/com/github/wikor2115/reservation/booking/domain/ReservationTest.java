package com.github.wikor2115.reservation.booking.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class ReservationTest {

    private static final Clock CREATED_CLOCK = Clock.fixed(
            Instant.parse("2026-06-01T10:00:00Z"),
            ZoneOffset.UTC);
    private static final Clock CANCELLED_CLOCK = Clock.fixed(
            Instant.parse("2026-06-01T11:00:00Z"),
            ZoneOffset.UTC);

    private static final Long AVAILABILITY_SLOT_ID = 10L;
    private static final Long OFFER_ID = 1L;
    private static final String CUSTOMER_NAME = "Jan Kowalski";
    private static final String CUSTOMER_EMAIL = "jan@example.com";
    private static final int PARTY_SIZE = 2;
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 6, 1, 10, 0);
    private static final LocalDateTime CANCELLED_AT = LocalDateTime.of(2026, 6, 1, 11, 0);

    @Test
    void create_validData_createsConfirmedReservation() {
        Reservation reservation = sampleReservation();

        assertAll(
                () -> assertNull(reservation.getId()),
                () -> assertEquals(AVAILABILITY_SLOT_ID, reservation.getAvailabilitySlotId()),
                () -> assertEquals(OFFER_ID, reservation.getOfferId()),
                () -> assertEquals(CUSTOMER_NAME, reservation.getCustomerName()),
                () -> assertEquals(CUSTOMER_EMAIL, reservation.getCustomerEmail()),
                () -> assertEquals(PARTY_SIZE, reservation.getPartySize()),
                () -> assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus()),
                () -> assertEquals(CREATED_AT, reservation.getCreatedAt()),
                () -> assertNull(reservation.getCancelledAt()));
    }

    @Test
    void create_trimsCustomerNameAndEmail() {
        Reservation reservation = Reservation.create(
                AVAILABILITY_SLOT_ID,
                OFFER_ID,
                "  Jan Kowalski  ",
                "  jan@example.com  ",
                PARTY_SIZE,
                CREATED_CLOCK);

        assertEquals(CUSTOMER_NAME, reservation.getCustomerName());
        assertEquals(CUSTOMER_EMAIL, reservation.getCustomerEmail());
    }

    @Test
    void create_whenAvailabilitySlotIdNull_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> Reservation.create(null, OFFER_ID, CUSTOMER_NAME, CUSTOMER_EMAIL, PARTY_SIZE, CREATED_CLOCK));
    }

    @Test
    void create_whenAvailabilitySlotIdZero_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> Reservation.create(0L, OFFER_ID, CUSTOMER_NAME, CUSTOMER_EMAIL, PARTY_SIZE, CREATED_CLOCK));
    }

    @Test
    void create_whenAvailabilitySlotIdNegative_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> Reservation.create(-1L, OFFER_ID, CUSTOMER_NAME, CUSTOMER_EMAIL, PARTY_SIZE, CREATED_CLOCK));
    }

    @Test
    void create_whenOfferIdNull_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> Reservation.create(AVAILABILITY_SLOT_ID, null, CUSTOMER_NAME, CUSTOMER_EMAIL, PARTY_SIZE,
                        CREATED_CLOCK));
    }

    @Test
    void create_whenOfferIdZero_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> Reservation.create(AVAILABILITY_SLOT_ID, 0L, CUSTOMER_NAME, CUSTOMER_EMAIL, PARTY_SIZE,
                        CREATED_CLOCK));
    }

    @Test
    void create_whenOfferIdNegative_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> Reservation.create(AVAILABILITY_SLOT_ID, -1L, CUSTOMER_NAME, CUSTOMER_EMAIL, PARTY_SIZE,
                        CREATED_CLOCK));
    }

    @Test
    void create_whenCustomerNameNull_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> Reservation.create(AVAILABILITY_SLOT_ID, OFFER_ID, null, CUSTOMER_EMAIL, PARTY_SIZE,
                        CREATED_CLOCK));
    }

    @Test
    void create_whenCustomerNameBlank_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> Reservation.create(AVAILABILITY_SLOT_ID, OFFER_ID, "   ", CUSTOMER_EMAIL, PARTY_SIZE,
                        CREATED_CLOCK));
    }

    @Test
    void create_whenCustomerNameTooLong_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> Reservation.create(AVAILABILITY_SLOT_ID, OFFER_ID, "a".repeat(256), CUSTOMER_EMAIL, PARTY_SIZE,
                        CREATED_CLOCK));
    }

    @Test
    void create_whenCustomerEmailNull_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> Reservation.create(AVAILABILITY_SLOT_ID, OFFER_ID, CUSTOMER_NAME, null, PARTY_SIZE,
                        CREATED_CLOCK));
    }

    @Test
    void create_whenCustomerEmailBlank_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> Reservation.create(AVAILABILITY_SLOT_ID, OFFER_ID, CUSTOMER_NAME, "   ", PARTY_SIZE,
                        CREATED_CLOCK));
    }

    @Test
    void create_whenCustomerEmailTooLong_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> Reservation.create(AVAILABILITY_SLOT_ID, OFFER_ID, CUSTOMER_NAME,
                        "a".repeat(245) + "@example.com", PARTY_SIZE, CREATED_CLOCK));
    }

    @Test
    void create_whenCustomerEmailInvalid_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> Reservation.create(AVAILABILITY_SLOT_ID, OFFER_ID, CUSTOMER_NAME, "jan.example.com", PARTY_SIZE,
                        CREATED_CLOCK));
    }

    @Test
    void create_whenCustomerEmailWithoutDomainSuffix_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> Reservation.create(AVAILABILITY_SLOT_ID, OFFER_ID, CUSTOMER_NAME, "jan@example", PARTY_SIZE,
                        CREATED_CLOCK));
    }

    @Test
    void create_whenCustomerEmailContainsWhitespace_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> Reservation.create(AVAILABILITY_SLOT_ID, OFFER_ID, CUSTOMER_NAME, "jan@ example.com",
                        PARTY_SIZE, CREATED_CLOCK));
    }

    @Test
    void create_whenPartySizeZero_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> Reservation.create(AVAILABILITY_SLOT_ID, OFFER_ID, CUSTOMER_NAME, CUSTOMER_EMAIL, 0,
                        CREATED_CLOCK));
    }

    @Test
    void create_whenPartySizeNegative_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> Reservation.create(AVAILABILITY_SLOT_ID, OFFER_ID, CUSTOMER_NAME, CUSTOMER_EMAIL, -1,
                        CREATED_CLOCK));
    }

    @Test
    void create_whenClockNull_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> Reservation.create(AVAILABILITY_SLOT_ID, OFFER_ID, CUSTOMER_NAME, CUSTOMER_EMAIL, PARTY_SIZE,
                        null));
    }

    @Test
    void cancel_whenConfirmed_changesStatusToCancelledAndSetsCancelledAt() {
        Reservation reservation = sampleReservation();

        reservation.cancel(CANCELLED_CLOCK);

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(CANCELLED_AT, reservation.getCancelledAt());
        assertEquals(CREATED_AT, reservation.getCreatedAt());
    }

    @Test
    void cancel_whenAlreadyCancelled_throwsIllegalStateException() {
        Reservation reservation = sampleReservation();
        reservation.cancel(CANCELLED_CLOCK);

        assertThrows(IllegalStateException.class, () -> reservation.cancel(CANCELLED_CLOCK));
        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(CANCELLED_AT, reservation.getCancelledAt());
    }

    @Test
    void cancel_whenClockNull_throwsIllegalArgumentException() {
        Reservation reservation = sampleReservation();

        assertThrows(IllegalArgumentException.class, () -> reservation.cancel(null));
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertNull(reservation.getCancelledAt());
    }

    private static Reservation sampleReservation() {
        return Reservation.create(
                AVAILABILITY_SLOT_ID,
                OFFER_ID,
                CUSTOMER_NAME,
                CUSTOMER_EMAIL,
                PARTY_SIZE,
                CREATED_CLOCK);
    }
}
