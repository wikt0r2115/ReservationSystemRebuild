package com.github.wikor2115.reservation.offer.repository;

import com.github.wikor2115.reservation.offer.domain.Offer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class OfferRepositoryTest {

    @Autowired
    private OfferRepository offerRepository;

    @Test
    void findByArchivedFalseOrderByNameAsc_returnsOnlyActiveSortedOffers() {
        Offer archivedOffer = Offer.create(
                "Zoo trip",
                "https://example.com/zoo.jpg",
                "Zoo visit",
                new BigDecimal("59.99")
        );
        archivedOffer.archive();

        Offer activeB = Offer.create(
                "Mountain Hike",
                "https://example.com/hike.jpg",
                "Hike",
                new BigDecimal("99.99")
        );
        Offer activeA = Offer.create(
                "City Tour",
                "https://example.com/city.jpg",
                "Tour",
                new BigDecimal("79.99")
        );

        offerRepository.saveAll(List.of(archivedOffer, activeB, activeA));

        List<Offer> result = offerRepository.findByArchivedFalseOrderByNameAsc();

        assertEquals(2, result.size());
        assertEquals("City Tour", result.get(0).getName());
        assertEquals("Mountain Hike", result.get(1).getName());
        assertFalse(result.get(0).isArchived());
        assertFalse(result.get(1).isArchived());
    }

    @Test
    void findByIdAndArchivedFalse_returnsEmptyForArchivedOffer() {
        Offer offer = Offer.create(
                "Kayak Trip",
                "https://example.com/kayak.jpg",
                "Kayak",
                new BigDecimal("89.99")
        );
        offerRepository.save(offer);
        offer.archive();
        offerRepository.save(offer);

        assertTrue(offerRepository.findByIdAndArchivedFalse(offer.getId()).isEmpty());
    }
}
