package com.github.wikor2115.reservation.offer.api;

import com.github.wikor2115.reservation.offer.domain.Offer;
import com.github.wikor2115.reservation.offer.service.OfferNotFoundException;
import com.github.wikor2115.reservation.offer.service.OfferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OfferControllerTest {

    @Mock
    private OfferService offerService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new OfferController(offerService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getActiveOffers_returnsOnlyActiveOffers() throws Exception {
        when(offerService.findActiveOffers()).thenReturn(List.of(sampleOffer()));

        mockMvc.perform(get("/api/v1/offers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Kayak Trip"))
                .andExpect(jsonPath("$[0].archived").value(false));
    }

    @Test
    void getActiveOfferById_returnsOffer() throws Exception {
        when(offerService.findActiveOfferById(1L)).thenReturn(sampleOffer());

        mockMvc.perform(get("/api/v1/offers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kayak Trip"))
                .andExpect(jsonPath("$.archived").value(false));
    }

    @Test
    void getActiveOfferById_whenMissing_returnsNotFound() throws Exception {
        when(offerService.findActiveOfferById(404L)).thenThrow(new OfferNotFoundException(404L));

        mockMvc.perform(get("/api/v1/offers/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("OFFER_NOT_FOUND"));
    }

    @Test
    void getAdminOffers_returnsAllOffers() throws Exception {
        Offer archived = sampleOffer();
        archived.archive();
        when(offerService.findAllOffers()).thenReturn(List.of(sampleOffer(), archived));

        mockMvc.perform(get("/api/v1/admin/offers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].archived").value(false))
                .andExpect(jsonPath("$[1].archived").value(true));
    }

    @Test
    void createOffer_validRequest_returnsCreatedOffer() throws Exception {
        when(offerService.createOffer(
                "City Tour",
                "https://example.com/city.jpg",
                "Full day city tour",
                new BigDecimal("199.99")
        )).thenReturn(Offer.create(
                "City Tour",
                "https://example.com/city.jpg",
                "Full day city tour",
                new BigDecimal("199.99")
        ));

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
    }

    @Test
    void createOffer_invalidRequest_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/admin/offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "imageUrl": "not-a-url",
                                  "description": "Full day city tour",
                                  "price": 199.999
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.length()").value(3));

        verifyNoInteractions(offerService);
    }

    @Test
    void updateOffer_updatesSelectedFields() throws Exception {
        Offer offer = sampleOffer();
        offer.rename("Updated");
        when(offerService.updateOffer(
                1L,
                "Updated",
                null,
                null,
                null
        )).thenReturn(offer);

        mockMvc.perform(patch("/api/v1/admin/offers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void updateOffer_whenBodyEmpty_returnsValidationError() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/offers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(offerService);
    }

    @Test
    void archiveOffer_missingOffer_returnsNotFound() throws Exception {
        when(offerService.archiveOffer(404L)).thenThrow(new OfferNotFoundException(404L));

        mockMvc.perform(delete("/api/v1/admin/offers/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("OFFER_NOT_FOUND"));
    }

    private static Offer sampleOffer() {
        return Offer.create(
                "Kayak Trip",
                "https://example.com/kayak.jpg",
                "Half-day kayaking",
                new BigDecimal("79.90")
        );
    }
}
