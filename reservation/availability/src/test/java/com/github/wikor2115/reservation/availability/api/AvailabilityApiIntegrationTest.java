package com.github.wikor2115.reservation.availability.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.github.wikor2115.reservation.availability.repository.AvailabilitySlotRepository;

@SpringBootTest
@ActiveProfiles("test")
class AvailabilityApiIntegrationTest {

    private static final String DUPLICATE_SLOT_MESSAGE =
            "Availability slot already exists for this offer and time range";

    @Autowired
    private AvailabilityController availabilityController;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    @Autowired
    private AvailabilitySlotRepository availabilitySlotRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(availabilityController)
                .setControllerAdvice(globalExceptionHandler)
                .build();
        availabilitySlotRepository.deleteAll();
    }

    @Test
    void availabilityLifecycle_createsUpdatesCancelsAndHidesCancelledSlotFromPublicEndpoint() throws Exception {
        long offerId = 1L;

        mockMvc.perform(post("/api/v1/admin/offers/{offerId}/availability", offerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "startsAt": "2099-06-02T10:00:00",
                          "endsAt": "2099-06-02T12:00:00",
                          "capacity": 10
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.offerId").value(offerId))
                .andExpect(jsonPath("$.capacity").value(10))
                .andExpect(jsonPath("$.status").value("OPEN"));

        long slotId = availabilitySlotRepository.findAll().get(0).getId();

        mockMvc.perform(get("/api/v1/offers/{offerId}/availability", offerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(slotId))
                .andExpect(jsonPath("$[0].status").value("OPEN"));

        mockMvc.perform(patch("/api/v1/admin/availability/{slotId}", slotId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "startsAt": "2099-06-03T10:00:00",
                          "endsAt": "2099-06-03T12:00:00",
                          "capacity": 20
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(slotId))
                .andExpect(jsonPath("$.capacity").value(20));

        mockMvc.perform(delete("/api/v1/admin/availability/{slotId}", slotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(slotId))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/api/v1/offers/{offerId}/availability", offerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/v1/admin/offers/{offerId}/availability", offerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(slotId))
                .andExpect(jsonPath("$[0].status").value("CANCELLED"));
    }

    @Test
    void createSlot_whenDuplicate_returnsConflict() throws Exception {
        long offerId = 1L;

        String payload = """
                {
                  "startsAt": "2099-06-02T10:00:00",
                  "endsAt": "2099-06-02T12:00:00",
                  "capacity": 10
                }
                """;

        mockMvc.perform(post("/api/v1/admin/offers/{offerId}/availability", offerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/admin/offers/{offerId}/availability", offerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AVAILABILITY_SLOT_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value(DUPLICATE_SLOT_MESSAGE));
    }
}
