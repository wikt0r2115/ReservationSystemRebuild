package com.github.wikor2115.reservation.offer.service;

import com.github.wikor2115.reservation.offer.domain.Offer;
import com.github.wikor2115.reservation.offer.repository.OfferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfferServiceTest {

    @Mock
    private OfferRepository offerRepository;

    @InjectMocks
    private OfferService offerService;

    @Test
    void createOffer_savesNewOffer() {
        when(offerRepository.save(any(Offer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Offer offer = offerService.createOffer(
                "City Tour",
                "https://example.com/city.jpg",
                "Full day city tour",
                new BigDecimal("199.99")
        );

        assertEquals("City Tour", offer.getName());
        assertEquals(new BigDecimal("199.99"), offer.getPrice());

        ArgumentCaptor<Offer> captor = ArgumentCaptor.forClass(Offer.class);
        verify(offerRepository).save(captor.capture());
        assertFalse(captor.getValue().isArchived());
    }

    @Test
    void findActiveOffers_returnsRepositoryResult() {
        Offer offer = sampleOffer();
        when(offerRepository.findByArchivedFalseOrderByNameAsc()).thenReturn(List.of(offer));

        List<Offer> offers = offerService.findActiveOffers();

        assertEquals(List.of(offer), offers);
        verify(offerRepository).findByArchivedFalseOrderByNameAsc();
    }

    @Test
    void findAllOffers_returnsRepositoryResult() {
        Offer offer = sampleOffer();
        when(offerRepository.findAllByOrderByNameAsc()).thenReturn(List.of(offer));

        List<Offer> offers = offerService.findAllOffers();

        assertEquals(List.of(offer), offers);
        verify(offerRepository).findAllByOrderByNameAsc();
    }

    @Test
    void findActiveOfferById_returnsActiveOffer() {
        Offer offer = sampleOffer();
        when(offerRepository.findByIdAndArchivedFalse(10L)).thenReturn(Optional.of(offer));

        Offer result = offerService.findActiveOfferById(10L);

        assertEquals(offer, result);
        verify(offerRepository).findByIdAndArchivedFalse(10L);
    }

    @Test
    void findActiveOfferById_whenMissing_throwsOfferNotFoundException() {
        when(offerRepository.findByIdAndArchivedFalse(404L)).thenReturn(Optional.empty());

        assertThrows(OfferNotFoundException.class, () -> offerService.findActiveOfferById(404L));
    }

    @Test
    void updateOffer_updatesOnlyProvidedFields() {
        Offer offer = sampleOffer();
        when(offerRepository.findById(10L)).thenReturn(Optional.of(offer));
        when(offerRepository.save(offer)).thenReturn(offer);

        Offer updated = offerService.updateOffer(
                10L,
                "Updated Name",
                null,
                "Updated description",
                new BigDecimal("99.99")
        );

        assertEquals("Updated Name", updated.getName());
        assertEquals("Updated description", updated.getDescription());
        assertEquals("https://example.com/kayak.jpg", updated.getImageUrl());
        assertEquals(new BigDecimal("99.99"), updated.getPrice());
        verify(offerRepository).save(offer);
    }

    @Test
    void updateOffer_whenMissingOffer_throwsOfferNotFoundException() {
        when(offerRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(
                OfferNotFoundException.class,
                () -> offerService.updateOffer(404L, "Name", null, null, null)
        );
    }

    @Test
    void archiveOffer_existingOffer_archivesAndSaves() {
        Offer offer = sampleOffer();
        when(offerRepository.findById(10L)).thenReturn(Optional.of(offer));
        when(offerRepository.save(offer)).thenReturn(offer);

        Offer archived = offerService.archiveOffer(10L);

        assertTrue(archived.isArchived());
        verify(offerRepository).save(offer);
    }

    @Test
    void archiveOffer_missingOffer_throwsOfferNotFoundException() {
        when(offerRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(OfferNotFoundException.class, () -> offerService.archiveOffer(404L));

        verify(offerRepository, never()).save(any());
    }

    private static Offer sampleOffer() {
        return Offer.create(
                "Kayak Trip",
                "https://example.com/kayak.jpg",
                "Half-day kayaking",
                new BigDecimal("79.90")
        );
    }
}
