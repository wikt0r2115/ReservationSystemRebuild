package com.github.wikor2115.reservation.offer.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class OfferTest {

	@Test
	void create_validData_createsOffer() {
		Offer offer = Offer.create(
				"City Tour",
				"https://example.com/image.jpg",
				"A full day city tour",
				new BigDecimal("199.99")
		);

		assertEquals("City Tour", offer.getName());
		assertEquals("https://example.com/image.jpg", offer.getImageUrl());
		assertEquals("A full day city tour", offer.getDescription());
		assertEquals(new BigDecimal("199.99"), offer.getPrice());
		assertFalse(offer.isArchived());
	}

	@Test
	void create_blankName_throwsIllegalArgumentException() {
		assertThrows(
				IllegalArgumentException.class,
				() -> Offer.create("   ", "https://example.com/image.jpg", "desc", new BigDecimal("10.00"))
		);
	}

	@Test
	void archive_twice_throwsIllegalStateException() {
		Offer offer = createSampleOffer();

		offer.archive();

		assertThrows(IllegalStateException.class, offer::archive);
	}

	@Test
	void changePrice_whenArchived_throwsIllegalStateException() {
		Offer offer = createSampleOffer();
		offer.archive();

		assertThrows(IllegalStateException.class, () -> offer.changePrice(new BigDecimal("49.99")));
	}

    @Test
    private Offer createOffer_validData_savesAndRetirnsOffer(){
        Offer offer = createSampleOffer();

        assertEquals("Mountain Hike", offer.getName());
        assertEquals("https://example.com/hike.jpg", offer.getImageUrl());
        assertEquals("A challenging mountain hike", offer.getDescription());
        assertEquals(new BigDecimal("149.50"), offer.getPrice());
        assertFalse(offer.isArchived());

        return offer;
    }

    private Offer createSampleOffer() {
		return Offer.create(
				"Kayak Trip",
				"https://example.com/kayak.jpg",
				"Half-day kayaking",
				new BigDecimal("79.90")
		);
	}
}
