package com.github.wikor2115.reservation.booking.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
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
import com.github.wikor2115.reservation.security.AuthenticatedUser;
import com.github.wikor2115.reservation.security.UserRole;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Reservations", description = "Customer reservation lifecycle and admin reservation management")
public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a pending reservation",
            description = "Creates a customer reservation for an availability slot and reserves capacity immediately.")
    public ReservationResponse createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            Authentication authentication
    ) {
        AuthenticatedUser user = authenticatedUser(authentication);
        assertCustomerOwnsEmail(user, request.customerEmail());
        return toResponse(reservationService.createReservation(
                request.availabilitySlotId(),
                request.customerName(),
                request.customerEmail(),
                request.partySize()));
    }

    @GetMapping("/reservations/{reservationId}")
    @Operation(
            summary = "Get reservation by id",
            description = "Returns one reservation. Customers can access only reservations matching their token email.")
    public ReservationResponse findReservationById(
            @PathVariable Long reservationId,
            Authentication authentication
    ) {
        AuthenticatedUser user = authenticatedUser(authentication);
        Reservation reservation = reservationService.findReservationById(reservationId);
        assertCustomerOwnsReservation(user, reservation);
        return toResponse(reservation);
    }

    @GetMapping(value = "/reservations", params = "customerEmail")
    @Operation(
            summary = "List reservations for customer email",
            description = "Returns reservations for a customer email. Customer tokens are restricted to their own email.")
    public List<ReservationResponse> findReservationsByCustomerEmail(
            @RequestParam String customerEmail,
            Authentication authentication
    ) {
        AuthenticatedUser user = authenticatedUser(authentication);
        assertCustomerOwnsEmail(user, customerEmail);
        return reservationService.findReservationsByCustomerEmail(customerEmail).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/admin/reservations")
    @Operation(summary = "Admin list reservations", description = "Returns all reservations ordered by creation time.")
    public List<ReservationResponse> findAllReservations() {
        return reservationService.findAllReservations().stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/admin/availability/{slotId}/reservations")
    @Operation(
            summary = "Admin list reservations for a slot",
            description = "Returns all reservations associated with one availability slot.")
    public List<ReservationResponse> findReservationsByAvailabilitySlotId(@PathVariable Long slotId) {
        return reservationService.findReservationsByAvailabilitySlotId(slotId).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/admin/reservations/{reservationId}/confirm")
    @Operation(
            summary = "Admin confirm reservation",
            description = "Confirms a pending reservation. Only pending reservations can be confirmed.")
    public ReservationResponse confirmReservation(@PathVariable Long reservationId) {
        return toResponse(reservationService.confirmReservation(reservationId));
    }

    @PostMapping("/admin/reservations/{reservationId}/reject")
    @Operation(
            summary = "Admin reject reservation",
            description = "Rejects a pending reservation and releases the reserved slot capacity.")
    public ReservationResponse rejectReservation(@PathVariable Long reservationId) {
        return toResponse(reservationService.rejectReservation(reservationId));
    }

    @DeleteMapping("/reservations/{reservationId}")
    @Operation(
            summary = "Cancel reservation",
            description = "Cancels a pending or confirmed reservation and releases reserved capacity.")
    public ReservationResponse cancelReservation(
            @PathVariable Long reservationId,
            Authentication authentication
    ) {
        AuthenticatedUser user = authenticatedUser(authentication);
        Reservation reservation = reservationService.findReservationById(reservationId);
        assertCustomerOwnsReservation(user, reservation);
        return toResponse(reservationService.cancelReservation(reservationId));
    }

    private static AuthenticatedUser authenticatedUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AccessDeniedException("Authenticated user is required");
        }
        return user;
    }

    private static void assertCustomerOwnsReservation(AuthenticatedUser user, Reservation reservation) {
        assertCustomerOwnsEmail(user, reservation.getCustomerEmail());
    }

    private static void assertCustomerOwnsEmail(AuthenticatedUser user, String customerEmail) {
        if (user.role() == UserRole.ADMIN) {
            return;
        }
        if (user.role() != UserRole.CUSTOMER || customerEmail == null
                || !user.email().equalsIgnoreCase(customerEmail.trim())) {
            throw new AccessDeniedException("Reservation does not belong to authenticated customer");
        }
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
