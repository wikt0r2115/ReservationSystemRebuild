package com.github.wikor2115.reservation.booking.service;

import java.time.Clock;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.wikor2115.reservation.availability.domain.AvailabilitySlot;
import com.github.wikor2115.reservation.availability.repository.AvailabilitySlotRepository;
import com.github.wikor2115.reservation.availability.service.AvailabilitySlotNotFoundException;
import com.github.wikor2115.reservation.booking.domain.Reservation;
import com.github.wikor2115.reservation.booking.repository.ReservationRepository;

@Service
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final Clock clock;

    @Autowired
    public ReservationService(
            ReservationRepository reservationRepository,
            AvailabilitySlotRepository availabilitySlotRepository
    ) {
        this(reservationRepository, availabilitySlotRepository, Clock.systemDefaultZone());
    }

    ReservationService(
            ReservationRepository reservationRepository,
            AvailabilitySlotRepository availabilitySlotRepository,
            Clock clock
    ) {
        this.reservationRepository = reservationRepository;
        this.availabilitySlotRepository = availabilitySlotRepository;
        this.clock = clock;
    }

    @Transactional
    public Reservation createReservation(
            Long availabilitySlotId,
            String customerName,
            String customerEmail,
            int partySize
    ) {
        AvailabilitySlot slot = findAvailabilitySlotOrThrow(availabilitySlotId);
        slot.reserve(partySize);

        Reservation reservation = Reservation.create(
                availabilitySlotId,
                slot.getOfferId(),
                customerName,
                customerEmail,
                partySize,
                clock);

        availabilitySlotRepository.save(slot);
        return reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public Reservation findReservationById(Long reservationId) {
        return findReservationOrThrow(reservationId);
    }

    @Transactional(readOnly = true)
    public List<Reservation> findAllReservations() {
        return reservationRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Reservation> findReservationsByCustomerEmail(String customerEmail) {
        validateCustomerEmailForLookup(customerEmail);
        return reservationRepository.findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(customerEmail.trim());
    }

    @Transactional(readOnly = true)
    public List<Reservation> findReservationsByAvailabilitySlotId(Long availabilitySlotId) {
        validateAvailabilitySlotId(availabilitySlotId);
        return reservationRepository.findByAvailabilitySlotIdOrderByCreatedAtDesc(availabilitySlotId);
    }

    @Transactional
    public Reservation confirmReservation(Long reservationId) {
        Reservation reservation = findReservationOrThrow(reservationId);
        reservation.confirm();
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation rejectReservation(Long reservationId) {
        Reservation reservation = findReservationOrThrow(reservationId);
        AvailabilitySlot slot = findAvailabilitySlotOrThrow(reservation.getAvailabilitySlotId());

        reservation.reject();
        slot.release(reservation.getPartySize());

        availabilitySlotRepository.save(slot);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation cancelReservation(Long reservationId) {
        Reservation reservation = findReservationOrThrow(reservationId);
        AvailabilitySlot slot = findAvailabilitySlotOrThrow(reservation.getAvailabilitySlotId());

        reservation.cancel(clock);
        slot.release(reservation.getPartySize());

        availabilitySlotRepository.save(slot);
        return reservationRepository.save(reservation);
    }

    private Reservation findReservationOrThrow(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
    }

    private AvailabilitySlot findAvailabilitySlotOrThrow(Long availabilitySlotId) {
        return availabilitySlotRepository.findById(availabilitySlotId)
                .orElseThrow(() -> new AvailabilitySlotNotFoundException(availabilitySlotId));
    }

    private static void validateAvailabilitySlotId(Long availabilitySlotId) {
        if (availabilitySlotId == null)
            throw new IllegalArgumentException("Availability slot id must not be null");
        if (availabilitySlotId <= 0)
            throw new IllegalArgumentException("Availability slot id must be greater than 0");
    }

    private static void validateCustomerEmailForLookup(String customerEmail) {
        if (customerEmail == null)
            throw new IllegalArgumentException("Customer email must not be null");
        if (customerEmail.trim().isEmpty())
            throw new IllegalArgumentException("Customer email must not be blank");
    }
}
