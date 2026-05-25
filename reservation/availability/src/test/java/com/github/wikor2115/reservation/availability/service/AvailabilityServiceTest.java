package com.github.wikor2115.reservation.availability.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.wikor2115.reservation.availability.domain.AvailabilitySlot;
import com.github.wikor2115.reservation.availability.domain.AvailabilityStatus;
import com.github.wikor2115.reservation.availability.repository.AvailabilitySlotRepository;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-01T10:00:00Z"),
            ZoneOffset.UTC);

    private static final Long OFFER_ID = 1L;
    private static final Long SLOT_ID = 10L;
    private static final LocalDateTime STARTS_AT = LocalDateTime.of(2026, 6, 2, 10, 0);
    private static final LocalDateTime ENDS_AT = LocalDateTime.of(2026, 6, 2, 12, 0);
    private static final int CAPACITY = 10;

    @Mock
    private AvailabilitySlotRepository slotRepository;

    @InjectMocks
    private AvailabilityService availabilityService;

    @Test
    void createSlot_savesNewSlot() {
        when(slotRepository.save(any(AvailabilitySlot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AvailabilitySlot slot = availabilityService.createSlot(OFFER_ID, STARTS_AT, ENDS_AT, CAPACITY);

        assertEquals(OFFER_ID, slot.getOfferId());
        assertEquals(STARTS_AT, slot.getStartsAt());
        assertEquals(ENDS_AT, slot.getEndsAt());
        assertEquals(CAPACITY, slot.getCapacity());
        assertEquals(AvailabilityStatus.OPEN, slot.getStatus());

        ArgumentCaptor<AvailabilitySlot> captor = ArgumentCaptor.forClass(AvailabilitySlot.class);
        verify(slotRepository).save(captor.capture());
        assertEquals(OFFER_ID, captor.getValue().getOfferId());
    }

    @Test
    void findOpenSlotsByOfferId_returnsRepositoryResult() {
        AvailabilitySlot slot = sampleSlot();
        when(slotRepository.findByOfferIdAndStatusOrderByStartsAtAsc(OFFER_ID, AvailabilityStatus.OPEN))
                .thenReturn(List.of(slot));

        List<AvailabilitySlot> result = availabilityService.findOpenSlotsByOfferId(OFFER_ID);

        assertEquals(List.of(slot), result);
        verify(slotRepository).findByOfferIdAndStatusOrderByStartsAtAsc(OFFER_ID, AvailabilityStatus.OPEN);
    }

    @Test
    void findSlotsByOfferId_returnsRepositoryResult() {
        AvailabilitySlot slot = sampleSlot();
        when(slotRepository.findByOfferIdOrderByStartsAtAsc(OFFER_ID)).thenReturn(List.of(slot));

        List<AvailabilitySlot> result = availabilityService.findSlotsByOfferId(OFFER_ID);

        assertEquals(List.of(slot), result);
        verify(slotRepository).findByOfferIdOrderByStartsAtAsc(OFFER_ID);
    }

    @Test
    void updateSlotById_updatesCapacityOnly() {
        AvailabilitySlot slot = sampleSlot();
        when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
        when(slotRepository.save(slot)).thenReturn(slot);

        AvailabilitySlot result = availabilityService.updateSlotById(SLOT_ID, null, null, 20);

        assertEquals(STARTS_AT, result.getStartsAt());
        assertEquals(ENDS_AT, result.getEndsAt());
        assertEquals(20, result.getCapacity());
        verify(slotRepository).save(slot);
    }

    @Test
    void updateSlotById_updatesStartTimeOnly() {
        AvailabilitySlot slot = sampleSlot();
        LocalDateTime newStartsAt = LocalDateTime.of(2026, 6, 2, 11, 0);
        when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
        when(slotRepository.save(slot)).thenReturn(slot);

        AvailabilitySlot result = availabilityService.updateSlotById(SLOT_ID, newStartsAt, null, null);

        assertEquals(newStartsAt, result.getStartsAt());
        assertEquals(ENDS_AT, result.getEndsAt());
        assertEquals(CAPACITY, result.getCapacity());
        verify(slotRepository).save(slot);
    }

    @Test
    void updateSlotById_updatesEndTimeOnly() {
        AvailabilitySlot slot = sampleSlot();
        LocalDateTime newEndsAt = LocalDateTime.of(2026, 6, 2, 13, 0);
        when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
        when(slotRepository.save(slot)).thenReturn(slot);

        AvailabilitySlot result = availabilityService.updateSlotById(SLOT_ID, null, newEndsAt, null);

        assertEquals(STARTS_AT, result.getStartsAt());
        assertEquals(newEndsAt, result.getEndsAt());
        assertEquals(CAPACITY, result.getCapacity());
        verify(slotRepository).save(slot);
    }

    @Test
    void updateSlotById_updatesStartAndEndTime() {
        AvailabilitySlot slot = sampleSlot();
        LocalDateTime newStartsAt = LocalDateTime.of(2026, 6, 3, 10, 0);
        LocalDateTime newEndsAt = LocalDateTime.of(2026, 6, 3, 12, 0);
        when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
        when(slotRepository.save(slot)).thenReturn(slot);

        AvailabilitySlot result = availabilityService.updateSlotById(SLOT_ID, newStartsAt, newEndsAt, null);

        assertEquals(newStartsAt, result.getStartsAt());
        assertEquals(newEndsAt, result.getEndsAt());
        assertEquals(CAPACITY, result.getCapacity());
        verify(slotRepository).save(slot);
    }

    @Test
    void updateSlotById_whenSlotMissing_throwsAvailabilitySlotNotFoundException() {
        when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.empty());

        assertThrows(
                AvailabilitySlotNotFoundException.class,
                () -> availabilityService.updateSlotById(SLOT_ID, STARTS_AT, ENDS_AT, CAPACITY));

        verify(slotRepository, never()).save(any());
    }

    @Test
    void cancelSlot_cancelsAndSavesSlot() {
        AvailabilitySlot slot = sampleSlot();
        when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
        when(slotRepository.save(slot)).thenReturn(slot);

        AvailabilitySlot result = availabilityService.cancelSlot(SLOT_ID);

        assertEquals(AvailabilityStatus.CANCELLED, result.getStatus());
        verify(slotRepository).save(slot);
    }

    @Test
    void cancelSlot_whenSlotMissing_throwsAvailabilitySlotNotFoundException() {
        when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.empty());

        assertThrows(AvailabilitySlotNotFoundException.class, () -> availabilityService.cancelSlot(SLOT_ID));

        verify(slotRepository, never()).save(any());
    }

    private static AvailabilitySlot sampleSlot() {
        return AvailabilitySlot.create(OFFER_ID, STARTS_AT, ENDS_AT, CAPACITY, CLOCK);
    }
}
