package com.github.wikor2115.reservation.availability.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

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
import com.github.wikor2115.reservation.security.AuthenticatedUser;
import com.github.wikor2115.reservation.security.UserRole;
import com.github.wikor2115.reservation.security.jwt.JwtProperties;
import com.github.wikor2115.reservation.security.jwt.JwtTokenService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AvailabilitySecurityIntegrationTest {
    private static final JwtTokenService TOKEN_SERVICE = new JwtTokenService(new JwtProperties(
            "test-secret",
            "reservation-auth-test",
            Duration.ofHours(1)));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AvailabilitySlotRepository availabilitySlotRepository;

    @BeforeEach
    void setUp() {
        availabilitySlotRepository.deleteAll();
    }

    @Test
    void publicAvailabilityList_isAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/offers/{offerId}/availability", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void adminCreateSlot_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(validCreateSlotRequest())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCreateSlot_withCustomerRole_returnsForbidden() throws Exception {
        mockMvc.perform(withBearerToken(validCreateSlotRequest(), UserRole.CUSTOMER))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreateSlot_withAdminRole_reachesController() throws Exception {
        mockMvc.perform(withBearerToken(validCreateSlotRequest(), UserRole.ADMIN))
                .andExpect(status().isCreated());
    }

    private static MockHttpServletRequestBuilder validCreateSlotRequest() {
        return post("/api/v1/admin/offers/{offerId}/availability", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "startsAt": "2099-06-02T10:00:00",
                          "endsAt": "2099-06-02T12:00:00",
                          "capacity": 10
                        }
                        """);
    }

    private static MockHttpServletRequestBuilder withBearerToken(
            MockHttpServletRequestBuilder request,
            UserRole role
    ) {
        String token = TOKEN_SERVICE.createToken(new AuthenticatedUser(1L, role.name().toLowerCase() + "@example.com", role));
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
}
