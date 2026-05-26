package com.github.wikor2115.reservation.booking.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.github.wikor2115.reservation.availability.domain.AvailabilitySlot;
import com.github.wikor2115.reservation.availability.repository.AvailabilitySlotRepository;
import com.github.wikor2115.reservation.booking.domain.Reservation;
import com.github.wikor2115.reservation.booking.repository.ReservationRepository;
import com.github.wikor2115.reservation.security.AuthenticatedUser;
import com.github.wikor2115.reservation.security.UserRole;
import com.github.wikor2115.reservation.security.jwt.JwtProperties;
import com.github.wikor2115.reservation.security.jwt.JwtTokenService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationSecurityIntegrationTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);
    private static final JwtTokenService TOKEN_SERVICE = new JwtTokenService(new JwtProperties(
            "test-secret",
            "reservation-auth-test",
            Duration.ofHours(1)));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private AvailabilitySlotRepository availabilitySlotRepository;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        availabilitySlotRepository.deleteAll();
    }

    @Test
    void createReservation_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(validCreateReservationRequest())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReservation_withCustomerRole_reachesControllerValidation() throws Exception {
        mockMvc.perform(withBearerToken(invalidCreateReservationRequest(), UserRole.CUSTOMER))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminReservationList_withCustomerRole_returnsForbidden() throws Exception {
        mockMvc.perform(withBearerToken(get("/api/v1/admin/reservations"), UserRole.CUSTOMER))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminReservationList_withAdminRole_reachesController() throws Exception {
        mockMvc.perform(withBearerToken(get("/api/v1/admin/reservations"), UserRole.ADMIN))
                .andExpect(status().isOk());
    }

    @Test
    void findReservationById_withDifferentCustomerToken_returnsForbidden() throws Exception {
        Reservation reservation = reservationRepository.save(sampleReservation("jan@example.com"));

        mockMvc.perform(withBearerToken(
                get("/api/v1/reservations/{reservationId}", reservation.getId()),
                "anna@example.com",
                UserRole.CUSTOMER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RESERVATION_ACCESS_DENIED"));
    }

    @Test
    void findReservationsByCustomerEmail_withDifferentCustomerToken_returnsForbidden() throws Exception {
        mockMvc.perform(withBearerToken(
                get("/api/v1/reservations").param("customerEmail", "jan@example.com"),
                "anna@example.com",
                UserRole.CUSTOMER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RESERVATION_ACCESS_DENIED"));
    }

    @Test
    void cancelReservation_withDifferentCustomerToken_returnsForbidden() throws Exception {
        Reservation reservation = reservationRepository.save(sampleReservation("jan@example.com"));

        mockMvc.perform(withBearerToken(
                delete("/api/v1/reservations/{reservationId}", reservation.getId()),
                "anna@example.com",
                UserRole.CUSTOMER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RESERVATION_ACCESS_DENIED"));
    }

    @Test
    void createReservation_withDifferentCustomerEmail_returnsForbidden() throws Exception {
        mockMvc.perform(withBearerToken(validCreateReservationRequest(), "anna@example.com", UserRole.CUSTOMER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RESERVATION_ACCESS_DENIED"));
    }

    private static MockHttpServletRequestBuilder validCreateReservationRequest() {
        return post("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "availabilitySlotId": 1,
                          "customerName": "Jan Kowalski",
                          "customerEmail": "jan@example.com",
                          "partySize": 2
                        }
                        """);
    }

    private static MockHttpServletRequestBuilder invalidCreateReservationRequest() {
        return post("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "availabilitySlotId": 0,
                          "customerName": "",
                          "customerEmail": "invalid-email",
                          "partySize": 0
                        }
                        """);
    }

    private static MockHttpServletRequestBuilder withBearerToken(
            MockHttpServletRequestBuilder request,
            UserRole role
    ) {
        return withBearerToken(request, role.name().toLowerCase() + "@example.com", role);
    }

    private static MockHttpServletRequestBuilder withBearerToken(
            MockHttpServletRequestBuilder request,
            String email,
            UserRole role
    ) {
        String token = TOKEN_SERVICE.createToken(new AuthenticatedUser(1L, email, role));
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private static Reservation sampleReservation(String customerEmail) {
        return Reservation.create(
                1L,
                1L,
                "Jan Kowalski",
                customerEmail,
                2,
                CLOCK);
    }
}
