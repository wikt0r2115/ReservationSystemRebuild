package com.github.wikor2115.reservation.offer.api;

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

import com.github.wikor2115.reservation.offer.repository.OfferRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OfferSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OfferRepository offerRepository;

    @BeforeEach
    void setUp() {
        offerRepository.deleteAll();
    }

    @Test
    void publicOfferList_isAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/offers"))
                .andExpect(status().isOk());
    }

    @Test
    void adminCreateOffer_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(validCreateOfferRequest())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCreateOffer_withCustomerRole_returnsForbidden() throws Exception {
        mockMvc.perform(withBasicAuth(validCreateOfferRequest(), "customer", "customer123"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreateOffer_withAdminRole_reachesController() throws Exception {
        mockMvc.perform(withBasicAuth(validCreateOfferRequest(), "admin", "admin123"))
                .andExpect(status().isCreated());
    }

    private static MockHttpServletRequestBuilder validCreateOfferRequest() {
        return post("/api/v1/admin/offers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "City Tour",
                          "imageUrl": "https://example.com/city.jpg",
                          "description": "Full day city tour",
                          "price": 199.99
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
