package com.github.wikor2115.reservation.booking.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.github.wikor2115.reservation.availability.domain.AvailabilitySlot;
import com.github.wikor2115.reservation.availability.repository.AvailabilitySlotRepository;
import com.github.wikor2115.reservation.booking.domain.Reservation;
import com.github.wikor2115.reservation.booking.repository.ReservationRepository;
import com.github.wikor2115.reservation.security.AuthenticatedUser;
import com.github.wikor2115.reservation.security.UserRole;

@SpringBootTest
@ActiveProfiles("test")
class ReservationApiIntegrationTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-01T10:00:00Z"),
            ZoneOffset.UTC);

    @Autowired
    private ReservationController reservationController;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private AvailabilitySlotRepository availabilitySlotRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(reservationController)
                .setControllerAdvice(globalExceptionHandler)
                .build();
        reservationRepository.deleteAll();
        availabilitySlotRepository.deleteAll();
    }

    @Test
    void reservationLifecycle_createsReservationReservesCapacityRejectsOverbookingAndCancels() throws Exception {
        AvailabilitySlot slot = availabilitySlotRepository.save(sampleSlot());

        mockMvc.perform(post("/api/v1/reservations")
                .with(customerAuth("jan@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "availabilitySlotId": %d,
                          "customerName": "Jan Kowalski",
                          "customerEmail": "jan@example.com",
                          "partySize": 2
                        }
                        """.formatted(slot.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.availabilitySlotId").value(slot.getId()))
                .andExpect(jsonPath("$.offerId").value(slot.getOfferId()))
                .andExpect(jsonPath("$.customerName").value("Jan Kowalski"))
                .andExpect(jsonPath("$.customerEmail").value("jan@example.com"))
                .andExpect(jsonPath("$.partySize").value(2))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        Reservation reservation = reservationRepository.findAll().get(0);
        assertEquals(2, availabilitySlotRepository.findById(slot.getId()).orElseThrow().getReservedCount());

        mockMvc.perform(get("/api/v1/reservations/{reservationId}", reservation.getId())
                .with(customerAuth("jan@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservation.getId()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(get("/api/v1/reservations")
                .with(customerAuth("jan@example.com"))
                .param("customerEmail", "JAN@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(reservation.getId()));

        mockMvc.perform(get("/api/v1/admin/availability/{slotId}/reservations", slot.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].availabilitySlotId").value(slot.getId()));

        mockMvc.perform(post("/api/v1/reservations")
                .with(customerAuth("anna@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "availabilitySlotId": %d,
                          "customerName": "Anna Nowak",
                          "customerEmail": "anna@example.com",
                          "partySize": 1
                        }
                        """.formatted(slot.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value("Reservation would exceed capacity of 2"));

        mockMvc.perform(delete("/api/v1/reservations/{reservationId}", reservation.getId())
                .with(customerAuth("jan@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservation.getId()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertEquals(0, availabilitySlotRepository.findById(slot.getId()).orElseThrow().getReservedCount());
    }

    private static AvailabilitySlot sampleSlot() {
        return AvailabilitySlot.create(
                1L,
                LocalDateTime.of(2099, 6, 2, 10, 0),
                LocalDateTime.of(2099, 6, 2, 12, 0),
                2,
                CLOCK);
    }

    private static RequestPostProcessor customerAuth(String email) {
        return request -> {
            request.setUserPrincipal(new UsernamePasswordAuthenticationToken(
                    new AuthenticatedUser(10L, email, UserRole.CUSTOMER),
                    null));
            return request;
        };
    }
}
