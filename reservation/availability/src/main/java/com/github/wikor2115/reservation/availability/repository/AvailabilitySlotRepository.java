package com.github.wikor2115.reservation.availability.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.wikor2115.reservation.availability.domain.AvailabilitySlot;
import com.github.wikor2115.reservation.availability.domain.AvailabilityStatus;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {
    List<AvailabilitySlot> findByOfferIdAndStatusOrderByStartsAtAsc(Long offerId, AvailabilityStatus status);

    List<AvailabilitySlot> findByOfferIdOrderByStartsAtAsc(Long offerId);
}
