package com.github.wikor2115.reservation.booking.api;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.github.wikor2115.reservation.availability.service.AvailabilitySlotNotFoundException;
import com.github.wikor2115.reservation.booking.domain.Reservation;
import com.github.wikor2115.reservation.booking.service.ReservationNotFoundException;
import com.github.wikor2115.reservation.booking.service.ReservationService;
import com.github.wikor2115.reservation.security.AuthenticatedUser;
import com.github.wikor2115.reservation.security.UserRole;

class ReservationControllerTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-01T10:00:00Z"),
            ZoneOffset.UTC);

    private static final Long RESERVATION_ID = 100L;
    private static final Long AVAILABILITY_SLOT_ID = 10L;
    private static final Long OFFER_ID = 1L;
    private static final String CUSTOMER_NAME = "Jan Kowalski";
    private static final String CUSTOMER_EMAIL = "jan@example.com";
    private static final int PARTY_SIZE = 2;

    private StubReservationService reservationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reservationService = new StubReservationService();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ReservationController(reservationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReservation_validRequest_returnsCreatedReservation() throws Exception {
        mockMvc.perform(post("/api/v1/reservations")
                .with(customerAuth(CUSTOMER_EMAIL))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "availabilitySlotId": 10,
                          "customerName": "Jan Kowalski",
                          "customerEmail": "jan@example.com",
                          "partySize": 2
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(RESERVATION_ID))
                .andExpect(jsonPath("$.availabilitySlotId").value(AVAILABILITY_SLOT_ID))
                .andExpect(jsonPath("$.offerId").value(OFFER_ID))
                .andExpect(jsonPath("$.customerName").value(CUSTOMER_NAME))
                .andExpect(jsonPath("$.customerEmail").value(CUSTOMER_EMAIL))
                .andExpect(jsonPath("$.partySize").value(PARTY_SIZE))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void createReservation_invalidRequest_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/reservations")
                .with(customerAuth(CUSTOMER_EMAIL))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "availabilitySlotId": 0,
                          "customerName": "",
                          "customerEmail": "invalid-email",
                          "partySize": 0
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", not(emptyOrNullString())))
                .andExpect(jsonPath("$.details[*].field",
                        hasItems("availabilitySlotId", "customerName", "customerEmail", "partySize")));
    }

    @Test
    void createReservation_whenRequestBodyHasInvalidType_returnsInvalidRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/reservations")
                .with(customerAuth(CUSTOMER_EMAIL))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "availabilitySlotId": "not-a-number",
                          "customerName": "Jan Kowalski",
                          "customerEmail": "jan@example.com",
                          "partySize": 2
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"))
                .andExpect(jsonPath("$.details[0].field").value("body"));
    }

    @Test
    void createReservation_whenAvailabilitySlotMissing_returnsNotFound() throws Exception {
        reservationService.createException = new AvailabilitySlotNotFoundException(404L);

        mockMvc.perform(post("/api/v1/reservations")
                .with(customerAuth(CUSTOMER_EMAIL))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "availabilitySlotId": 404,
                          "customerName": "Jan Kowalski",
                          "customerEmail": "jan@example.com",
                          "partySize": 2
                        }
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AVAILABILITY_SLOT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Availability slot with id 404 not found"))
                .andExpect(jsonPath("$.details[0].field").value("availabilitySlotId"));
    }

    @Test
    void createReservation_whenCapacityExceeded_returnsBusinessRuleViolation() throws Exception {
        reservationService.createException = new IllegalArgumentException("Reservation would exceed capacity of 2");

        mockMvc.perform(post("/api/v1/reservations")
                .with(customerAuth(CUSTOMER_EMAIL))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "availabilitySlotId": 10,
                          "customerName": "Jan Kowalski",
                          "customerEmail": "jan@example.com",
                          "partySize": 3
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value("Reservation would exceed capacity of 2"));
    }

    @Test
    void findReservationById_existingReservation_returnsReservation() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/{reservationId}", RESERVATION_ID)
                .with(customerAuth(CUSTOMER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(RESERVATION_ID))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void findReservationById_whenMissing_returnsNotFound() throws Exception {
        reservationService.findException = new ReservationNotFoundException(404L);

        mockMvc.perform(get("/api/v1/reservations/404")
                .with(customerAuth(CUSTOMER_EMAIL)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Reservation with id 404 not found"))
                .andExpect(jsonPath("$.details[0].field").value("reservationId"));
    }

    @Test
    void findReservationsByCustomerEmail_returnsReservations() throws Exception {
        mockMvc.perform(get("/api/v1/reservations")
                .with(customerAuth(CUSTOMER_EMAIL))
                .param("customerEmail", CUSTOMER_EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(RESERVATION_ID))
                .andExpect(jsonPath("$[0].customerEmail").value(CUSTOMER_EMAIL));
    }

    @Test
    void findAllReservations_returnsReservations() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(RESERVATION_ID));
    }

    @Test
    void findReservationsByAvailabilitySlotId_returnsReservations() throws Exception {
        mockMvc.perform(get("/api/v1/admin/availability/{slotId}/reservations", AVAILABILITY_SLOT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].availabilitySlotId").value(AVAILABILITY_SLOT_ID));
    }

    @Test
    void cancelReservation_existingReservation_returnsCancelledReservation() throws Exception {
        mockMvc.perform(delete("/api/v1/reservations/{reservationId}", RESERVATION_ID)
                .with(customerAuth(CUSTOMER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(RESERVATION_ID))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").value("2026-06-01T10:00:00"));
    }

    @Test
    void cancelReservation_whenAlreadyCancelled_returnsBusinessRuleViolation() throws Exception {
        reservationService.cancelException = new IllegalStateException("Reservation is cancelled");

        mockMvc.perform(delete("/api/v1/reservations/{reservationId}", RESERVATION_ID)
                .with(customerAuth(CUSTOMER_EMAIL)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value("Reservation is cancelled"));
    }

    @Test
    void findReservationById_whenCustomerDoesNotOwnReservation_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/{reservationId}", RESERVATION_ID)
                .with(customerAuth("anna@example.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RESERVATION_ACCESS_DENIED"));
    }

    private static Reservation sampleReservation(Long reservationId, boolean cancelled) {
        Reservation reservation = Reservation.create(
                AVAILABILITY_SLOT_ID,
                OFFER_ID,
                CUSTOMER_NAME,
                CUSTOMER_EMAIL,
                PARTY_SIZE,
                CLOCK);
        ReflectionTestUtils.setField(reservation, "id", reservationId);
        if (cancelled) {
            reservation.cancel(CLOCK);
        }
        return reservation;
    }

    private static RequestPostProcessor customerAuth(String email) {
        return request -> {
            request.setUserPrincipal(new UsernamePasswordAuthenticationToken(
                    new AuthenticatedUser(10L, email, UserRole.CUSTOMER),
                    null));
            return request;
        };
    }

    private static final class StubReservationService extends ReservationService {
        private RuntimeException createException;
        private RuntimeException findException;
        private RuntimeException cancelException;

        private StubReservationService() {
            super(null, null);
        }

        @Override
        public Reservation createReservation(
                Long availabilitySlotId,
                String customerName,
                String customerEmail,
                int partySize
        ) {
            if (createException != null) {
                throw createException;
            }
            return sampleReservation(RESERVATION_ID, false);
        }

        @Override
        public Reservation findReservationById(Long reservationId) {
            if (findException != null) {
                throw findException;
            }
            return sampleReservation(reservationId, false);
        }

        @Override
        public List<Reservation> findReservationsByCustomerEmail(String customerEmail) {
            return List.of(sampleReservation(RESERVATION_ID, false));
        }

        @Override
        public List<Reservation> findAllReservations() {
            return List.of(sampleReservation(RESERVATION_ID, false));
        }

        @Override
        public List<Reservation> findReservationsByAvailabilitySlotId(Long availabilitySlotId) {
            return List.of(sampleReservation(RESERVATION_ID, false));
        }

        @Override
        public Reservation cancelReservation(Long reservationId) {
            if (cancelException != null) {
                throw cancelException;
            }
            return sampleReservation(reservationId, true);
        }
    }
}
