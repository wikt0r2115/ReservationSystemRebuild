package com.github.wikor2115.reservation.availability.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.wikor2115.reservation.availability.domain.AvailabilitySlot;
import com.github.wikor2115.reservation.availability.domain.AvailabilityStatus;
import com.github.wikor2115.reservation.availability.repository.AvailabilitySlotRepository;

@Service
public class AvailabilityService {
    private final AvailabilitySlotRepository slotRepository;

    public AvailabilityService(AvailabilitySlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    @Transactional
    public AvailabilitySlot createSlot(Long offerId, LocalDateTime startsAt, LocalDateTime endsAt, int capacity) {
        AvailabilitySlot slot = AvailabilitySlot.create(offerId, startsAt, endsAt, capacity);
        ensureSlotIsUnique(slot.getOfferId(), slot.getStartsAt(), slot.getEndsAt());
        return slotRepository.save(slot);
    }

    @Transactional(readOnly = true)
    public List<AvailabilitySlot> findOpenSlotsByOfferId(Long offerId) {
        return slotRepository.findByOfferIdAndStatusOrderByStartsAtAsc(offerId, AvailabilityStatus.OPEN);
    }

    @Transactional(readOnly = true)
    public List<AvailabilitySlot> findSlotsByOfferId(Long offerId) {
        return slotRepository.findByOfferIdOrderByStartsAtAsc(offerId);
    }

    @Transactional
    public AvailabilitySlot updateSlotById(Long slotId, LocalDateTime startsAt, LocalDateTime endsAt,
            Integer capacity) {
        AvailabilitySlot slot = findSlotOrThrow(slotId);
        if (startsAt != null || endsAt != null) {
            LocalDateTime newStartsAt = startsAt != null ? startsAt : slot.getStartsAt();
            LocalDateTime newEndsAt = endsAt != null ? endsAt : slot.getEndsAt();
            slot.changeTime(newStartsAt, newEndsAt);
            ensureSlotIsUnique(slot.getOfferId(), newStartsAt, newEndsAt, slotId);
        }
        if (capacity != null)
            slot.changeCapacity(capacity);
        return slotRepository.save(slot);
    }

    @Transactional
    public AvailabilitySlot cancelSlot(Long slotId) {
        AvailabilitySlot slot = findSlotOrThrow(slotId);
        slot.cancel();
        return slotRepository.save(slot);
    }

    private AvailabilitySlot findSlotOrThrow(Long slotId) {
        return slotRepository.findById(slotId)
                .orElseThrow(() -> new AvailabilitySlotNotFoundException(slotId));
    }

    private void ensureSlotIsUnique(Long offerId, LocalDateTime startsAt, LocalDateTime endsAt) {
        if (slotRepository.existsByOfferIdAndStartsAtAndEndsAt(offerId, startsAt, endsAt))
            throw new DuplicateAvailabilitySlotException();
    }

    private void ensureSlotIsUnique(Long offerId, LocalDateTime startsAt, LocalDateTime endsAt, Long slotId) {
        if (slotRepository.existsByOfferIdAndStartsAtAndEndsAtAndIdNot(offerId, startsAt, endsAt, slotId))
            throw new DuplicateAvailabilitySlotException();
    }
}
