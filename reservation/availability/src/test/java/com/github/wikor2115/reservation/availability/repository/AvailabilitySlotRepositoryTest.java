package com.github.wikor2115.reservation.availability.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.github.wikor2115.reservation.availability.domain.AvailabilitySlot;
import com.github.wikor2115.reservation.availability.domain.AvailabilityStatus;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AvailabilitySlotRepositoryTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-01T10:00:00Z"),
            ZoneOffset.UTC);

    private static final Long OFFER_ID = 1L;
    private static final Long OTHER_OFFER_ID = 2L;
    private static final int CAPACITY = 10;

    @Autowired
    private AvailabilitySlotRepository availabilitySlotRepository;

    @Test
    void save_persistsAvailabilitySlot() {
        AvailabilitySlot saved = availabilitySlotRepository.save(sampleSlot(OFFER_ID, startsAt(10)));

        AvailabilitySlot found = availabilitySlotRepository.findById(saved.getId()).orElseThrow();

        assertEquals(saved.getId(), found.getId());
        assertEquals(OFFER_ID, found.getOfferId());
        assertEquals(AvailabilityStatus.OPEN, found.getStatus());
    }

    @Test
    void findById_whenMissing_returnsEmpty() {
        assertTrue(availabilitySlotRepository.findById(999L).isEmpty());
    }

    @Test
    void findByOfferIdAndStatusOrderByStartsAtAsc_returnsOnlyMatchingOpenSlotsSortedByStartDate() {
        AvailabilitySlot laterOpen = sampleSlot(OFFER_ID, startsAt(12));
        AvailabilitySlot earlierOpen = sampleSlot(OFFER_ID, startsAt(9));
        AvailabilitySlot closed = sampleSlot(OFFER_ID, startsAt(8));
        closed.close();
        AvailabilitySlot otherOfferOpen = sampleSlot(OTHER_OFFER_ID, startsAt(7));

        availabilitySlotRepository.saveAll(List.of(laterOpen, earlierOpen, closed, otherOfferOpen));

        List<AvailabilitySlot> result = availabilitySlotRepository.findByOfferIdAndStatusOrderByStartsAtAsc(
                OFFER_ID,
                AvailabilityStatus.OPEN);

        assertEquals(2, result.size());
        assertEquals(earlierOpen.getStartsAt(), result.get(0).getStartsAt());
        assertEquals(laterOpen.getStartsAt(), result.get(1).getStartsAt());
        assertTrue(result.stream().allMatch(slot -> OFFER_ID.equals(slot.getOfferId())));
        assertTrue(result.stream().allMatch(slot -> slot.getStatus() == AvailabilityStatus.OPEN));
    }

    @Test
    void findByOfferIdOrderByStartsAtAsc_returnsAllSlotsForOfferSortedByStartDate() {
        AvailabilitySlot laterOpen = sampleSlot(OFFER_ID, startsAt(12));
        AvailabilitySlot earlierOpen = sampleSlot(OFFER_ID, startsAt(9));
        AvailabilitySlot cancelled = sampleSlot(OFFER_ID, startsAt(8));
        cancelled.cancel();
        AvailabilitySlot otherOfferOpen = sampleSlot(OTHER_OFFER_ID, startsAt(7));

        availabilitySlotRepository.saveAll(List.of(laterOpen, earlierOpen, cancelled, otherOfferOpen));

        List<AvailabilitySlot> result = availabilitySlotRepository.findByOfferIdOrderByStartsAtAsc(OFFER_ID);

        assertEquals(3, result.size());
        assertEquals(cancelled.getStartsAt(), result.get(0).getStartsAt());
        assertEquals(earlierOpen.getStartsAt(), result.get(1).getStartsAt());
        assertEquals(laterOpen.getStartsAt(), result.get(2).getStartsAt());
        assertTrue(result.stream().allMatch(slot -> OFFER_ID.equals(slot.getOfferId())));
    }

    private static AvailabilitySlot sampleSlot(Long offerId, LocalDateTime startsAt) {
        return AvailabilitySlot.create(offerId, startsAt, startsAt.plusHours(2), CAPACITY, CLOCK);
    }

    private static LocalDateTime startsAt(int hour) {
        return LocalDateTime.of(2026, 6, 2, hour, 0);
    }
}
