package com.github.wikor2115.reservation.booking.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.wikor2115.reservation.booking.domain.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findAllByOrderByCreatedAtDesc();

    List<Reservation> findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(String customerEmail);

    List<Reservation> findByAvailabilitySlotIdOrderByCreatedAtDesc(Long availabilitySlotId);
}
