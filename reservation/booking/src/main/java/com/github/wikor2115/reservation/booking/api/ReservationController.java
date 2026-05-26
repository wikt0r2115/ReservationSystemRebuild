package com.github.wikor2115.reservation.booking.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.github.wikor2115.reservation.booking.domain.Reservation;
import com.github.wikor2115.reservation.booking.service.ReservationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse createReservation(@Valid @RequestBody CreateReservationRequest request) {
        return toResponse(reservationService.createReservation(
                request.availabilitySlotId(),
                request.customerName(),
                request.customerEmail(),
                request.partySize()));
    }

    @GetMapping("/reservations/{reservationId}")
    public ReservationResponse findReservationById(@PathVariable Long reservationId) {
        return toResponse(reservationService.findReservationById(reservationId));
    }

    @GetMapping(value = "/reservations", params = "customerEmail")
    public List<ReservationResponse> findReservationsByCustomerEmail(@RequestParam String customerEmail) {
        return reservationService.findReservationsByCustomerEmail(customerEmail).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/admin/reservations")
    public List<ReservationResponse> findAllReservations() {
        return reservationService.findAllReservations().stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/admin/availability/{slotId}/reservations")
    public List<ReservationResponse> findReservationsByAvailabilitySlotId(@PathVariable Long slotId) {
        return reservationService.findReservationsByAvailabilitySlotId(slotId).stream()
                .map(this::toResponse)
                .toList();
    }

    @DeleteMapping("/reservations/{reservationId}")
    public ReservationResponse cancelReservation(@PathVariable Long reservationId) {
        return toResponse(reservationService.cancelReservation(reservationId));
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getAvailabilitySlotId(),
                reservation.getOfferId(),
                reservation.getCustomerName(),
                reservation.getCustomerEmail(),
                reservation.getPartySize(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getCancelledAt());
    }
}
