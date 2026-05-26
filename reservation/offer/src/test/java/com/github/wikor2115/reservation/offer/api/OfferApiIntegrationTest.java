package com.github.wikor2115.reservation.offer.api;

import com.github.wikor2115.reservation.offer.repository.OfferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class OfferApiIntegrationTest {

    @Autowired
    private OfferController offerController;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    @Autowired
    private OfferRepository offerRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(offerController)
                .setControllerAdvice(globalExceptionHandler)
                .build();
        offerRepository.deleteAll();
    }

    @Test
    void offerLifecycle_createsUpdatesArchivesAndHidesFromPublicEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/admin/offers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "City Tour",
                          "imageUrl": "https://example.com/city.jpg",
                          "description": "Full day city tour",
                          "price": 199.99
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("City Tour"))
                .andExpect(jsonPath("$.archived").value(false));

        long offerId = offerRepository.findAll().get(0).getId();

        mockMvc.perform(get("/api/v1/offers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(offerId))
                .andExpect(jsonPath("$[0].name").value("City Tour"));

        mockMvc.perform(patch("/api/v1/admin/offers/{offerId}", offerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Updated City Tour",
                          "price": 249.99
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated City Tour"))
                .andExpect(jsonPath("$.price").value(249.99));

        mockMvc.perform(delete("/api/v1/admin/offers/{offerId}", offerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));

        mockMvc.perform(get("/api/v1/offers/{offerId}", offerId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("OFFER_NOT_FOUND"));
    }
}
