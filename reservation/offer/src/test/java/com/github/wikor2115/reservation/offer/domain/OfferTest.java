package com.github.wikor2115.reservation.offer.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OfferTest {

	@Test
	void create_validData_createsOffer() {
		Offer offer = Offer.create(
				"City Tour",
				"https://example.com/image.jpg",
				"A full day city tour",
				new BigDecimal("199.99"));

		assertEquals("City Tour", offer.getName());
		assertEquals("https://example.com/image.jpg", offer.getImageUrl());
		assertEquals("A full day city tour", offer.getDescription());
		assertEquals(new BigDecimal("199.99"), offer.getPrice());
		assertFalse(offer.isArchived());
	}

	@Test
	void create_trimsFields() {
		Offer offer = Offer.create(
				"  City Tour  ",
				"  https://example.com/image.jpg  ",
				"  A full day city tour  ",
				new BigDecimal("199.99"));

		assertEquals("City Tour", offer.getName());
		assertEquals("https://example.com/image.jpg", offer.getImageUrl());
		assertEquals("A full day city tour", offer.getDescription());
	}

	@Test
	void create_blankName_throwsIllegalArgumentException() {
		assertThrows(
				IllegalArgumentException.class,
				() -> Offer.create("   ", "https://example.com/image.jpg", "desc", new BigDecimal("10.00")));
	}

	@Test
	void create_whenNameTooLong_throwsIllegalArgumentException() {
		assertThrows(
				IllegalArgumentException.class,
				() -> Offer.create(
						"a".repeat(256),
						"https://example.com/image.jpg",
						"desc",
						new BigDecimal("10.00")));
	}

	@Test
	void create_whenDescriptionTooLong_throwsIllegalArgumentException() {
		assertThrows(
				IllegalArgumentException.class,
				() -> Offer.create(
						"City Tour",
						"https://example.com/image.jpg",
						"a".repeat(2049),
						new BigDecimal("10.00")));
	}

	@Test
	void create_whenImageUrlTooLong_throwsIllegalArgumentException() {
		assertThrows(
				IllegalArgumentException.class,
				() -> Offer.create(
						"City Tour",
						"https://example.com/" + "a".repeat(2048),
						"desc",
						new BigDecimal("10.00")));
	}

	@Test
	void archive_success_archivesOffer() {
		Offer offer = createSampleOffer();
		offer.archive();
		assertTrue(offer.isArchived());
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
	void create_whenImageUrlInvalid_throwsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> Offer.create(
				"City Tour",
				"example.com/image.jpg",
				"A full day city tour",
				new BigDecimal("199.99")));

	}

	@Test
	void create_setsPriceScaleToTwo() {
		Offer offer = Offer.create(
				"City Tour",
				"https://example.com/image.jpg",
				"A full day city tour",
				new BigDecimal("199.9"));
		assertEquals(new BigDecimal("199.90"), offer.getPrice());
	}

	@Test
	void create_whenPriceHasTooManyFractionDigits_throwsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> Offer.create(
				"City Tour",
				"https://example.com/image.jpg",
				"A full day city tour",
				new BigDecimal("199.999")));
	}

	@Test
	void rename_whenNameIsSame_keepsName() {
		Offer offer = createSampleOffer();
		offer.rename(offer.getName());
		assertEquals("Kayak Trip", offer.getName());
	}

	@Test
	void changePrice_whenPriceHasTooManyFractionDigits_throwsIllegalArgumentException() {
		Offer offer = createSampleOffer();
		assertThrows(IllegalArgumentException.class, () -> offer.changePrice(new BigDecimal("19.999")));
	}


	private Offer createSampleOffer() {
		return Offer.create(
				"Kayak Trip",
				"https://example.com/kayak.jpg",
				"Half-day kayaking",
				new BigDecimal("79.90"));
	}
}
