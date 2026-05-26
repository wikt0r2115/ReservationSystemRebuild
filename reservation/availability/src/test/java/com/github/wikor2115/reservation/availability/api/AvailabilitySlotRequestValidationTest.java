package com.github.wikor2115.reservation.availability.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class AvailabilitySlotRequestValidationTest {

    private static final LocalDateTime STARTS_AT = LocalDateTime.of(2026, 6, 2, 10, 0);
    private static final LocalDateTime ENDS_AT = LocalDateTime.of(2026, 6, 2, 12, 0);

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void createRequest_validData_passesValidation() {
        CreateAvailabilitySlotRequest request = new CreateAvailabilitySlotRequest(STARTS_AT, ENDS_AT, 10);

        Set<ConstraintViolation<CreateAvailabilitySlotRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void createRequest_missingRequiredFields_failsValidation() {
        CreateAvailabilitySlotRequest request = new CreateAvailabilitySlotRequest(null, null, null);

        Set<ConstraintViolation<CreateAvailabilitySlotRequest>> violations = validator.validate(request);

        assertThat(propertyPaths(violations)).containsExactlyInAnyOrder("startsAt", "endsAt", "capacity");
    }

    @Test
    void createRequest_nonPositiveCapacity_failsValidation() {
        CreateAvailabilitySlotRequest request = new CreateAvailabilitySlotRequest(STARTS_AT, ENDS_AT, 0);

        Set<ConstraintViolation<CreateAvailabilitySlotRequest>> violations = validator.validate(request);

        assertThat(propertyPaths(violations)).containsExactly("capacity");
    }

    @Test
    void updateRequest_oneFieldProvided_passesValidation() {
        UpdateAvailabilitySlotRequest request = new UpdateAvailabilitySlotRequest(null, null, 20);

        Set<ConstraintViolation<UpdateAvailabilitySlotRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void updateRequest_emptyBody_failsValidation() {
        UpdateAvailabilitySlotRequest request = new UpdateAvailabilitySlotRequest(null, null, null);

        Set<ConstraintViolation<UpdateAvailabilitySlotRequest>> violations = validator.validate(request);

        assertThat(propertyPaths(violations)).containsExactly("anyFieldProvided");
        assertThat(violations).singleElement()
                .extracting(ConstraintViolation::getMessage)
                .isEqualTo("At least one field must be provided");
    }

    @Test
    void updateRequest_nonPositiveCapacity_failsValidation() {
        UpdateAvailabilitySlotRequest request = new UpdateAvailabilitySlotRequest(null, null, 0);

        Set<ConstraintViolation<UpdateAvailabilitySlotRequest>> violations = validator.validate(request);

        assertThat(propertyPaths(violations)).containsExactly("capacity");
    }

    private static Set<String> propertyPaths(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }
}
