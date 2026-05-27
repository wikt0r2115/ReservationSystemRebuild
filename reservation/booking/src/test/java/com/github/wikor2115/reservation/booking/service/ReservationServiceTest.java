package com.github.wikor2115.reservation.booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.wikor2115.reservation.availability.domain.AvailabilitySlot;
import com.github.wikor2115.reservation.availability.repository.AvailabilitySlotRepository;
import com.github.wikor2115.reservation.availability.service.AvailabilitySlotNotFoundException;
import com.github.wikor2115.reservation.booking.domain.Reservation;
import com.github.wikor2115.reservation.booking.domain.ReservationStatus;
import com.github.wikor2115.reservation.booking.repository.ReservationRepository;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-01T10:00:00Z"),
            ZoneOffset.UTC);

    private static final Long RESERVATION_ID = 100L;
    private static final Long AVAILABILITY_SLOT_ID = 10L;
    private static final Long OFFER_ID = 1L;
    private static final String CUSTOMER_NAME = "Jan Kowalski";
    private static final String CUSTOMER_EMAIL = "jan@example.com";
    private static final int PARTY_SIZE = 2;
    private static final LocalDateTime STARTS_AT = LocalDateTime.of(2026, 6, 2, 10, 0);
    private static final LocalDateTime ENDS_AT = LocalDateTime.of(2026, 6, 2, 12, 0);

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private AvailabilitySlotRepository availabilitySlotRepository;

    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(reservationRepository, availabilitySlotRepository, CLOCK);
    }

    @Test
    void createReservation_reservesAvailabilitySlotAndSavesReservation() {
        AvailabilitySlot slot = sampleSlot(2);
        when(availabilitySlotRepository.findById(AVAILABILITY_SLOT_ID)).thenReturn(Optional.of(slot));
        when(availabilitySlotRepository.save(slot)).thenReturn(slot);
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reservation reservation = reservationService.createReservation(
                AVAILABILITY_SLOT_ID,
                CUSTOMER_NAME,
                CUSTOMER_EMAIL,
                PARTY_SIZE);

        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        assertEquals(AVAILABILITY_SLOT_ID, reservation.getAvailabilitySlotId());
        assertEquals(OFFER_ID, reservation.getOfferId());
        assertEquals(PARTY_SIZE, reservation.getPartySize());
        assertEquals(PARTY_SIZE, slot.getReservedCount());

        verify(availabilitySlotRepository).save(slot);
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertEquals(CUSTOMER_EMAIL, captor.getValue().getCustomerEmail());
    }

    @Test
    void createReservation_whenAvailabilitySlotMissing_throwsAvailabilitySlotNotFoundException() {
        when(availabilitySlotRepository.findById(AVAILABILITY_SLOT_ID)).thenReturn(Optional.empty());

        assertThrows(AvailabilitySlotNotFoundException.class, () -> reservationService.createReservation(
                AVAILABILITY_SLOT_ID,
                CUSTOMER_NAME,
                CUSTOMER_EMAIL,
                PARTY_SIZE));

        verify(availabilitySlotRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_whenCapacityExceeded_throwsIllegalArgumentException() {
        AvailabilitySlot slot = sampleSlot(1);
        when(availabilitySlotRepository.findById(AVAILABILITY_SLOT_ID)).thenReturn(Optional.of(slot));

        assertThrows(IllegalArgumentException.class, () -> reservationService.createReservation(
                AVAILABILITY_SLOT_ID,
                CUSTOMER_NAME,
                CUSTOMER_EMAIL,
                PARTY_SIZE));

        verify(availabilitySlotRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void findReservationById_returnsReservation() {
        Reservation reservation = sampleReservation();
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

        Reservation result = reservationService.findReservationById(RESERVATION_ID);

        assertEquals(reservation, result);
    }

    @Test
    void findReservationById_whenMissing_throwsReservationNotFoundException() {
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class,
                () -> reservationService.findReservationById(RESERVATION_ID));
    }

    @Test
    void findAllReservations_returnsRepositoryResult() {
        Reservation reservation = sampleReservation();
        when(reservationRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(reservation));

        List<Reservation> result = reservationService.findAllReservations();

        assertEquals(List.of(reservation), result);
        verify(reservationRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void findReservationsByCustomerEmail_trimsEmailAndReturnsRepositoryResult() {
        Reservation reservation = sampleReservation();
        when(reservationRepository.findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(CUSTOMER_EMAIL))
                .thenReturn(List.of(reservation));

        List<Reservation> result = reservationService.findReservationsByCustomerEmail("  jan@example.com  ");

        assertEquals(List.of(reservation), result);
        verify(reservationRepository).findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(CUSTOMER_EMAIL);
    }

    @Test
    void findReservationsByCustomerEmail_whenBlank_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> reservationService.findReservationsByCustomerEmail(" "));
    }

    @Test
    void findReservationsByAvailabilitySlotId_returnsRepositoryResult() {
        Reservation reservation = sampleReservation();
        when(reservationRepository.findByAvailabilitySlotIdOrderByCreatedAtDesc(AVAILABILITY_SLOT_ID))
                .thenReturn(List.of(reservation));

        List<Reservation> result = reservationService.findReservationsByAvailabilitySlotId(AVAILABILITY_SLOT_ID);

        assertEquals(List.of(reservation), result);
        verify(reservationRepository).findByAvailabilitySlotIdOrderByCreatedAtDesc(AVAILABILITY_SLOT_ID);
    }

    @Test
    void cancelReservation_cancelsReservationReleasesAvailabilitySlotAndSavesBoth() {
        Reservation reservation = sampleReservation();
        AvailabilitySlot slot = sampleSlot(10);
        slot.reserve(PARTY_SIZE);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(availabilitySlotRepository.findById(AVAILABILITY_SLOT_ID)).thenReturn(Optional.of(slot));
        when(availabilitySlotRepository.save(slot)).thenReturn(slot);
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        Reservation result = reservationService.cancelReservation(RESERVATION_ID);

        assertEquals(ReservationStatus.CANCELLED, result.getStatus());
        assertEquals(LocalDateTime.of(2026, 6, 1, 10, 0), result.getCancelledAt());
        assertEquals(0, slot.getReservedCount());
        verify(availabilitySlotRepository).save(slot);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void confirmReservation_whenPending_confirmsAndSavesReservation() {
        Reservation reservation = sampleReservation();
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        Reservation result = reservationService.confirmReservation(RESERVATION_ID);

        assertEquals(ReservationStatus.CONFIRMED, result.getStatus());
        verify(reservationRepository).save(reservation);
        verify(availabilitySlotRepository, never()).findById(any());
        verify(availabilitySlotRepository, never()).save(any());
    }

    @Test
    void confirmReservation_whenAlreadyConfirmed_throwsIllegalStateException() {
        Reservation reservation = sampleReservation();
        reservation.confirm();
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

        assertThrows(IllegalStateException.class, () -> reservationService.confirmReservation(RESERVATION_ID));

        verify(reservationRepository, never()).save(any());
        verify(availabilitySlotRepository, never()).findById(any());
    }

    @Test
    void rejectReservation_whenPending_rejectsReleasesAvailabilitySlotAndSavesBoth() {
        Reservation reservation = sampleReservation();
        AvailabilitySlot slot = sampleSlot(10);
        slot.reserve(PARTY_SIZE);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(availabilitySlotRepository.findById(AVAILABILITY_SLOT_ID)).thenReturn(Optional.of(slot));
        when(availabilitySlotRepository.save(slot)).thenReturn(slot);
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        Reservation result = reservationService.rejectReservation(RESERVATION_ID);

        assertEquals(ReservationStatus.REJECTED, result.getStatus());
        assertEquals(0, slot.getReservedCount());
        verify(availabilitySlotRepository).save(slot);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void rejectReservation_whenAlreadyConfirmed_throwsIllegalStateExceptionAndKeepsCapacityReserved() {
        Reservation reservation = sampleReservation();
        reservation.confirm();
        AvailabilitySlot slot = sampleSlot(10);
        slot.reserve(PARTY_SIZE);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(availabilitySlotRepository.findById(AVAILABILITY_SLOT_ID)).thenReturn(Optional.of(slot));

        assertThrows(IllegalStateException.class, () -> reservationService.rejectReservation(RESERVATION_ID));

        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(PARTY_SIZE, slot.getReservedCount());
        verify(availabilitySlotRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void cancelReservation_whenReservationMissing_throwsReservationNotFoundException() {
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class, () -> reservationService.cancelReservation(RESERVATION_ID));

        verify(availabilitySlotRepository, never()).findById(any());
        verify(availabilitySlotRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void cancelReservation_whenAvailabilitySlotMissing_throwsAvailabilitySlotNotFoundExceptionAndKeepsReservationOpen() {
        Reservation reservation = sampleReservation();
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(availabilitySlotRepository.findById(AVAILABILITY_SLOT_ID)).thenReturn(Optional.empty());

        assertThrows(AvailabilitySlotNotFoundException.class,
                () -> reservationService.cancelReservation(RESERVATION_ID));

        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        assertNull(reservation.getCancelledAt());
        verify(availabilitySlotRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void cancelReservation_whenAlreadyCancelled_throwsIllegalStateException() {
        Reservation reservation = sampleReservation();
        reservation.cancel(CLOCK);
        AvailabilitySlot slot = sampleSlot(10);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(availabilitySlotRepository.findById(AVAILABILITY_SLOT_ID)).thenReturn(Optional.of(slot));

        assertThrows(IllegalStateException.class, () -> reservationService.cancelReservation(RESERVATION_ID));

        verify(availabilitySlotRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
    }

    private static AvailabilitySlot sampleSlot(int capacity) {
        return AvailabilitySlot.create(OFFER_ID, STARTS_AT, ENDS_AT, capacity, CLOCK);
    }

    private static Reservation sampleReservation() {
        return Reservation.create(
                AVAILABILITY_SLOT_ID,
                OFFER_ID,
                CUSTOMER_NAME,
                CUSTOMER_EMAIL,
                PARTY_SIZE,
                CLOCK);
    }
}
