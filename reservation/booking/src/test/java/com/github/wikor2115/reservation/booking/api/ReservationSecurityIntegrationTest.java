package com.github.wikor2115.reservation.booking.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

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

import com.github.wikor2115.reservation.availability.repository.AvailabilitySlotRepository;
import com.github.wikor2115.reservation.booking.repository.ReservationRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationSecurityIntegrationTest {

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
        mockMvc.perform(withBasicAuth(invalidCreateReservationRequest(), "customer", "customer123"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminReservationList_withCustomerRole_returnsForbidden() throws Exception {
        mockMvc.perform(withBasicAuth(get("/api/v1/admin/reservations"), "customer", "customer123"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminReservationList_withAdminRole_reachesController() throws Exception {
        mockMvc.perform(withBasicAuth(get("/api/v1/admin/reservations"), "admin", "admin123"))
                .andExpect(status().isOk());
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

    private static MockHttpServletRequestBuilder withBasicAuth(
            MockHttpServletRequestBuilder request,
            String username,
            String password
    ) {
        String token = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        return request.header(HttpHeaders.AUTHORIZATION, "Basic " + token);
    }
}
