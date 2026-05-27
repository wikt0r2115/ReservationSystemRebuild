package com.github.wikor2115.reservation.availability.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.github.wikor2115.reservation.availability.domain.AvailabilitySlot;
import com.github.wikor2115.reservation.availability.service.AvailabilityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Availability", description = "Public slot browsing and admin availability management")
public class AvailabilityController {
    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService){
        this.availabilityService = availabilityService;
    }

    @GetMapping("/offers/{offerId}/availability")
    @Operation(summary = "List open slots", description = "Returns open availability slots for an active offer.")
    public List<AvailabilitySlotResponse> findOpenSlotsByOfferId(@PathVariable Long offerId){
        return availabilityService.findOpenSlotsByOfferId(offerId).stream().map(this::toResponse).toList();
    }

    @GetMapping("/admin/offers/{offerId}/availability")
    @Operation(summary = "Admin list slots", description = "Returns all slots for an offer, including cancelled slots.")
    public List<AvailabilitySlotResponse> findSlotsByOfferId(@PathVariable Long offerId){
        return availabilityService.findSlotsByOfferId(offerId).stream().map(this::toResponse).toList();
    }

    @PostMapping("/admin/offers/{offerId}/availability")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Admin create slot", description = "Creates an open availability slot for an offer.")
    public AvailabilitySlotResponse createSlot(@PathVariable Long offerId, @Valid @RequestBody CreateAvailabilitySlotRequest request){
        return toResponse(availabilityService.createSlot(offerId, request.startsAt(), request.endsAt(), request.capacity()));
    }

    @PatchMapping("/admin/availability/{slotId}")
    @Operation(summary = "Admin update slot", description = "Partially updates time or capacity for an availability slot.")
    public AvailabilitySlotResponse updateSlot(@PathVariable Long slotId, @Valid @RequestBody UpdateAvailabilitySlotRequest request){
        return toResponse(availabilityService.updateSlotById(slotId, request.startsAt(), request.endsAt(), request.capacity()));
    }

    @DeleteMapping("/admin/availability/{slotId}")
    @Operation(summary = "Admin cancel slot", description = "Cancels an availability slot so it cannot accept reservations.")
    public AvailabilitySlotResponse cancelSlot(@PathVariable Long slotId){
        return toResponse(availabilityService.cancelSlot(slotId));
    }

    private AvailabilitySlotResponse toResponse(AvailabilitySlot slot){
        return new AvailabilitySlotResponse(
            slot.getId(),
            slot.getOfferId(),
            slot.getStartsAt(),
            slot.getEndsAt(),
            slot.getCapacity(),
            slot.getReservedCount(),
            slot.getStatus()
        );
    }
}
