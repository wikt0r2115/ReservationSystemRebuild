package com.github.wikor2115.reservation.booking.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.github.wikor2115.reservation.booking.domain.Reservation;
import com.github.wikor2115.reservation.booking.domain.ReservationStatus;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReservationRepositoryTest {

    private static final Long AVAILABILITY_SLOT_ID = 10L;
    private static final Long OTHER_AVAILABILITY_SLOT_ID = 20L;
    private static final Long OFFER_ID = 1L;
    private static final String CUSTOMER_EMAIL = "jan@example.com";
    private static final String OTHER_CUSTOMER_EMAIL = "anna@example.com";

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    void save_persistsReservation() {
        Reservation saved = reservationRepository.save(sampleReservation(
                AVAILABILITY_SLOT_ID,
                CUSTOMER_EMAIL,
                "2026-06-01T10:00:00Z"));

        Reservation found = reservationRepository.findById(saved.getId()).orElseThrow();

        assertEquals(saved.getId(), found.getId());
        assertEquals(AVAILABILITY_SLOT_ID, found.getAvailabilitySlotId());
        assertEquals(OFFER_ID, found.getOfferId());
        assertEquals(CUSTOMER_EMAIL, found.getCustomerEmail());
        assertEquals(ReservationStatus.CONFIRMED, found.getStatus());
    }

    @Test
    void findById_whenMissing_returnsEmpty() {
        assertTrue(reservationRepository.findById(999L).isEmpty());
    }

    @Test
    void findAllByOrderByCreatedAtDesc_returnsReservationsSortedByCreationTimeDescending() {
        Reservation earlier = sampleReservation(AVAILABILITY_SLOT_ID, CUSTOMER_EMAIL, "2026-06-01T10:00:00Z");
        Reservation later = sampleReservation(OTHER_AVAILABILITY_SLOT_ID, OTHER_CUSTOMER_EMAIL,
                "2026-06-01T11:00:00Z");

        reservationRepository.saveAll(List.of(earlier, later));

        List<Reservation> result = reservationRepository.findAllByOrderByCreatedAtDesc();

        assertEquals(2, result.size());
        assertEquals(later.getCreatedAt(), result.get(0).getCreatedAt());
        assertEquals(earlier.getCreatedAt(), result.get(1).getCreatedAt());
    }

    @Test
    void findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc_returnsMatchingReservationsSortedDescending() {
        Reservation earlier = sampleReservation(AVAILABILITY_SLOT_ID, CUSTOMER_EMAIL, "2026-06-01T10:00:00Z");
        Reservation later = sampleReservation(OTHER_AVAILABILITY_SLOT_ID, CUSTOMER_EMAIL, "2026-06-01T11:00:00Z");
        Reservation otherCustomer = sampleReservation(AVAILABILITY_SLOT_ID, OTHER_CUSTOMER_EMAIL,
                "2026-06-01T12:00:00Z");

        reservationRepository.saveAll(List.of(earlier, later, otherCustomer));

        List<Reservation> result = reservationRepository.findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(
                "JAN@EXAMPLE.COM");

        assertEquals(2, result.size());
        assertEquals(later.getCreatedAt(), result.get(0).getCreatedAt());
        assertEquals(earlier.getCreatedAt(), result.get(1).getCreatedAt());
        assertTrue(result.stream().allMatch(reservation -> CUSTOMER_EMAIL.equals(reservation.getCustomerEmail())));
    }

    @Test
    void findByAvailabilitySlotIdOrderByCreatedAtDesc_returnsMatchingReservationsSortedDescending() {
        Reservation earlier = sampleReservation(AVAILABILITY_SLOT_ID, CUSTOMER_EMAIL, "2026-06-01T10:00:00Z");
        Reservation later = sampleReservation(AVAILABILITY_SLOT_ID, OTHER_CUSTOMER_EMAIL, "2026-06-01T11:00:00Z");
        Reservation otherSlot = sampleReservation(OTHER_AVAILABILITY_SLOT_ID, CUSTOMER_EMAIL,
                "2026-06-01T12:00:00Z");

        reservationRepository.saveAll(List.of(earlier, later, otherSlot));

        List<Reservation> result = reservationRepository.findByAvailabilitySlotIdOrderByCreatedAtDesc(
                AVAILABILITY_SLOT_ID);

        assertEquals(2, result.size());
        assertEquals(later.getCreatedAt(), result.get(0).getCreatedAt());
        assertEquals(earlier.getCreatedAt(), result.get(1).getCreatedAt());
        assertTrue(result.stream()
                .allMatch(reservation -> AVAILABILITY_SLOT_ID.equals(reservation.getAvailabilitySlotId())));
    }

    private static Reservation sampleReservation(Long availabilitySlotId, String customerEmail, String instant) {
        return Reservation.create(
                availabilitySlotId,
                OFFER_ID,
                "Jan Kowalski",
                customerEmail,
                2,
                Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }
}
