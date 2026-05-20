package com.github.wikor2115.reservation.availability.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

class AvailabilitySlotTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-01T10:00:00Z"),
            ZoneOffset.UTC);

    private static final Long OFFER_ID = 1L;
    private static final LocalDateTime STARTS_AT = LocalDateTime.of(2026, 6, 2, 10, 0);
    private static final LocalDateTime ENDS_AT = LocalDateTime.of(2026, 6, 2, 12, 0);
    private static final int CAPACITY = 10;

    @Test
    void create_validData_createsOpenSlot() {
        AvailabilitySlot slot = sampleSlot();
        assertAll(
                () -> assertEquals(OFFER_ID, slot.getOfferId()),
                () -> assertEquals(STARTS_AT, slot.getStartsAt()),
                () -> assertEquals(ENDS_AT, slot.getEndsAt()),
                () -> assertEquals(CAPACITY, slot.getCapacity()),
                () -> assertEquals(0, slot.getReservedCount()),
                () -> assertEquals(AvailabilityStatus.OPEN, slot.getStatus()));
    }

    @Test
    void create_whenOfferIdNull_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> AvailabilitySlot.create(null, STARTS_AT, ENDS_AT, CAPACITY, CLOCK));
    }

    @Test
    void create_whenStartDateNull_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> AvailabilitySlot.create(OFFER_ID, null, ENDS_AT, CAPACITY, CLOCK));
    }

    @Test
    void create_whenEndDateNull_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> AvailabilitySlot.create(OFFER_ID, STARTS_AT, null, CAPACITY, CLOCK));
    }

    @Test
    void create_whenStartDateInPast_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> AvailabilitySlot.create(OFFER_ID, pastStartsAt(), ENDS_AT, CAPACITY, CLOCK));
    }

    @Test
    void create_whenEndDateIsNotAfterStartDate_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> AvailabilitySlot.create(OFFER_ID, STARTS_AT, STARTS_AT, CAPACITY, CLOCK));
    }

    @Test
    void create_whenCapacityIsZero_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> AvailabilitySlot.create(OFFER_ID, STARTS_AT, ENDS_AT, 0, CLOCK));

    }

    @Test
    void changeTime_validData_updatesTime() {
        AvailabilitySlot slot = sampleSlot();
        slot.changeTime(futureStartsAt(), futureEndsAt(), CLOCK);
        assertEquals(futureStartsAt(), slot.getStartsAt());
        assertEquals(futureEndsAt(), slot.getEndsAt());
    }

    @Test
    void changeTime_whenCancelled_throwsIllegalStateException() {
        AvailabilitySlot slot = sampleSlot();
        slot.cancel();
        assertThrows(IllegalStateException.class, () -> slot.changeTime(futureStartsAt(), futureEndsAt(), CLOCK));
    }

    @Test
    void changeCapacity_validData_updatesCapacity() {
        AvailabilitySlot slot = sampleSlot();
        slot.changeCapacity(20);
        assertEquals(20, slot.getCapacity());
    }

    @Test
    void changeCapacity_belowReservedCount_throwsIllegalArgumentException() {
        AvailabilitySlot slot = slotWithReservedCount(8);
        assertThrows(IllegalArgumentException.class, () -> slot.changeCapacity(7));
    }

    @Test
    void reserve_validPartySize_increasesReservedCount() {
        AvailabilitySlot slot = sampleSlotWithCapacity(20);
        slot.reserve(10);
        assertEquals(10, slot.getReservedCount());
    }

    @Test
    void reserve_whenSlotClosed_throwsIllegalStateException() {
        AvailabilitySlot slot = sampleSlot();
        slot.close();
        assertThrows(IllegalStateException.class, () -> slot.reserve(1));
    }

    @Test
    void reserve_whenPartySizeExceedsCapacity_throwsIllegalArgumentException() {
        AvailabilitySlot slot = sampleSlotWithCapacity(20);
        assertThrows(IllegalArgumentException.class, () -> slot.reserve(25));
    }

    @Test
    void release_validPartySize_decreasesReservedCount() {
        AvailabilitySlot slot = sampleSlotWithCapacity(20);
        slot.reserve(10);
        slot.release(5);
        assertEquals(5, slot.getReservedCount());
    }

    @Test
    void release_whenPartySizeExceedsReservedCount_throwsIllegalArgumentException() {
        AvailabilitySlot slot = sampleSlotWithCapacity(20);
        assertThrows(IllegalArgumentException.class, () -> slot.release(50));
    }

    @Test
    void close_whenOpen_changesStatusToClosed() {
        AvailabilitySlot slot = sampleSlot();
        slot.close();
        assertEquals(AvailabilityStatus.CLOSED, slot.getStatus());
    }

    @Test
    void close_whenAlreadyClosed_throwsIllegalStateException() {
        AvailabilitySlot slot = closedSlot();
        assertThrows(IllegalStateException.class, () -> slot.close());
    }

    @Test
    void reopen_whenClosed_changesStatusToOpen() {
        AvailabilitySlot slot = closedSlot();
        slot.reopen();
        assertEquals(AvailabilityStatus.OPEN, slot.getStatus());
    }

    @Test
    void reopen_whenCancelled_throwsIllegalStateException() {
        AvailabilitySlot slot = sampleSlot();
        slot.cancel();
        assertThrows(IllegalStateException.class, () -> slot.reopen());
    }

    @Test
    void cancel_whenOpen_changesStatusToCancelled() {
        AvailabilitySlot slot = cancelledSlot();
        assertEquals(AvailabilityStatus.CANCELLED, slot.getStatus());
    }

    @Test
    void cancel_whenAlreadyCancelled_throwsIllegalStateException() {
        AvailabilitySlot slot = cancelledSlot();
        assertThrows(IllegalStateException.class, ()->slot.cancel());
    }

    private static AvailabilitySlot sampleSlot() {
        return AvailabilitySlot.create(OFFER_ID, STARTS_AT, ENDS_AT, CAPACITY, CLOCK);
    }

    private static AvailabilitySlot sampleSlotWithCapacity(int capacity) {
        return AvailabilitySlot.create(OFFER_ID, STARTS_AT, ENDS_AT, capacity, CLOCK);
    }

    private static AvailabilitySlot slotWithReservedCount(int reservedCount) {
        AvailabilitySlot slot = sampleSlot();
        slot.reserve(reservedCount);
        return slot;
    }

    private static AvailabilitySlot closedSlot() {
        AvailabilitySlot slot = sampleSlot();
        slot.close();
        return slot;
    }

    private static AvailabilitySlot cancelledSlot() {
        AvailabilitySlot slot = sampleSlot();
        slot.cancel();
        return slot;
    }

    private static LocalDateTime futureStartsAt() {
        return LocalDateTime.of(2026, 6, 3, 10, 0);
    }

    private static LocalDateTime futureEndsAt() {
        return LocalDateTime.of(2026, 6, 3, 12, 0);
    }

    private static LocalDateTime pastStartsAt() {
        return LocalDateTime.of(2026, 5, 31, 10, 0);
    }
}
