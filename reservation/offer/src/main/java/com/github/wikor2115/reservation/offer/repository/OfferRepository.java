package com.github.wikor2115.reservation.offer.repository;

import com.github.wikor2115.reservation.offer.domain.Offer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfferRepository extends JpaRepository<Offer, Long> {
	List<Offer> findByArchivedFalse();
}
