package com.github.wikor2115.reservation.availability.api;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.github.wikor2115.reservation.availability.domain.AvailabilitySlot;
import com.github.wikor2115.reservation.availability.service.AvailabilityService;
import com.github.wikor2115.reservation.availability.service.AvailabilitySlotNotFoundException;

class AvailabilityControllerTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-01T10:00:00Z"),
            ZoneOffset.UTC);

    private static final Long OFFER_ID = 1L;
    private static final Long SLOT_ID = 10L;
    private static final LocalDateTime STARTS_AT = LocalDateTime.of(2026, 6, 2, 10, 0);
    private static final LocalDateTime ENDS_AT = LocalDateTime.of(2026, 6, 2, 12, 0);

    private StubAvailabilityService availabilityService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        availabilityService = new StubAvailabilityService();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AvailabilityController(availabilityService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void findOpenSlotsByOfferId_returnsOpenSlots() throws Exception {
        availabilityService.openSlots = List.of(sampleSlot(SLOT_ID, OFFER_ID, STARTS_AT, ENDS_AT, 10));

        mockMvc.perform(get("/api/v1/offers/{offerId}/availability", OFFER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(SLOT_ID))
                .andExpect(jsonPath("$[0].offerId").value(OFFER_ID))
                .andExpect(jsonPath("$[0].capacity").value(10))
                .andExpect(jsonPath("$[0].reservedCount").value(0))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    void createSlot_validRequest_returnsCreatedSlot() throws Exception {
        mockMvc.perform(post("/api/v1/admin/offers/{offerId}/availability", OFFER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "startsAt": "2026-06-02T10:00:00",
                          "endsAt": "2026-06-02T12:00:00",
                          "capacity": 10
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.offerId").value(OFFER_ID))
                .andExpect(jsonPath("$.capacity").value(10))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void createSlot_invalidRequest_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/admin/offers/{offerId}/availability", OFFER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "startsAt": null,
                          "endsAt": null,
                          "capacity": 0
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", not(emptyOrNullString())))
                .andExpect(jsonPath("$.details[*].field", hasItems("startsAt", "endsAt", "capacity")));
    }

    @Test
    void createSlot_whenRequestBodyHasInvalidType_returnsInvalidRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/admin/offers/{offerId}/availability", OFFER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "startsAt": "not-a-date",
                          "endsAt": "2026-06-02T12:00:00",
                          "capacity": 10
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"))
                .andExpect(jsonPath("$.details[0].field").value("body"));
    }

    @Test
    void updateSlot_updatesSelectedFields() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/availability/{slotId}", SLOT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "capacity": 20
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SLOT_ID))
                .andExpect(jsonPath("$.capacity").value(20))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void updateSlot_whenBodyEmpty_returnsValidationError() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/availability/{slotId}", SLOT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("anyFieldProvided"))
                .andExpect(jsonPath("$.details[0].message").value("At least one field must be provided"));
    }

    @Test
    void updateSlot_whenMissing_returnsNotFound() throws Exception {
        availabilityService.updateException = new AvailabilitySlotNotFoundException(404L);

        mockMvc.perform(patch("/api/v1/admin/availability/404")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "capacity": 20
                        }
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AVAILABILITY_SLOT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Availability slot with id 404 not found"))
                .andExpect(jsonPath("$.details[0].field").value("slotId"))
                .andExpect(jsonPath("$.details[0].message").value("Availability slot with id 404 not found"));
    }

    @Test
    void cancelSlot_existingSlot_returnsCancelledSlot() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/availability/{slotId}", SLOT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SLOT_ID))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelSlot_whenMissing_returnsNotFound() throws Exception {
        availabilityService.cancelException = new AvailabilitySlotNotFoundException(404L);

        mockMvc.perform(delete("/api/v1/admin/availability/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AVAILABILITY_SLOT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Availability slot with id 404 not found"))
                .andExpect(jsonPath("$.details[0].field").value("slotId"))
                .andExpect(jsonPath("$.details[0].message").value("Availability slot with id 404 not found"));
    }

    @Test
    void cancelSlot_whenAlreadyCancelled_returnsBusinessRuleViolation() throws Exception {
        availabilityService.cancelException = new IllegalStateException("Availability slot is cancelled");

        mockMvc.perform(delete("/api/v1/admin/availability/{slotId}", SLOT_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value("Availability slot is cancelled"));
    }

    private static AvailabilitySlot sampleSlot(
            Long slotId,
            Long offerId,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            int capacity
    ) {
        AvailabilitySlot slot = AvailabilitySlot.create(offerId, startsAt, endsAt, capacity, CLOCK);
        ReflectionTestUtils.setField(slot, "id", slotId);
        return slot;
    }

    private static final class StubAvailabilityService extends AvailabilityService {
        private List<AvailabilitySlot> openSlots = List.of(sampleSlot(SLOT_ID, OFFER_ID, STARTS_AT, ENDS_AT, 10));
        private RuntimeException updateException;
        private RuntimeException cancelException;

        private StubAvailabilityService() {
            super(null);
        }

        @Override
        public List<AvailabilitySlot> findOpenSlotsByOfferId(Long offerId) {
            return openSlots;
        }

        @Override
        public AvailabilitySlot createSlot(Long offerId, LocalDateTime startsAt, LocalDateTime endsAt, int capacity) {
            return sampleSlot(SLOT_ID, offerId, startsAt, endsAt, capacity);
        }

        @Override
        public AvailabilitySlot updateSlotById(
                Long slotId,
                LocalDateTime startsAt,
                LocalDateTime endsAt,
                Integer capacity
        ) {
            if (updateException != null) {
                throw updateException;
            }
            return sampleSlot(slotId, OFFER_ID, STARTS_AT, ENDS_AT, capacity);
        }

        @Override
        public AvailabilitySlot cancelSlot(Long slotId) {
            if (cancelException != null) {
                throw cancelException;
            }
            AvailabilitySlot slot = sampleSlot(slotId, OFFER_ID, STARTS_AT, ENDS_AT, 10);
            slot.cancel();
            return slot;
        }
    }
}
