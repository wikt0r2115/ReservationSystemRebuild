package com.github.wikor2115.reservation.booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.github.wikor2115.reservation.availability.domain.AvailabilitySlot;
import com.github.wikor2115.reservation.availability.repository.AvailabilitySlotRepository;
import com.github.wikor2115.reservation.booking.domain.Reservation;
import com.github.wikor2115.reservation.booking.domain.ReservationStatus;
import com.github.wikor2115.reservation.booking.repository.ReservationRepository;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration/booking",
        "spring.flyway.table=booking_flyway_schema_history",
        "spring.sql.init.mode=never",
        "spring.h2.console.enabled=false",
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
@Testcontainers(disabledWithoutDocker = true)
class ReservationServicePostgresTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);

    @Container
    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private AvailabilitySlotRepository availabilitySlotRepository;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        availabilitySlotRepository.deleteAll();
    }

    @Test
    void createConfirmAndCancelReservation_usesPostgresFlywaySchema() {
        AvailabilitySlot slot = availabilitySlotRepository.save(sampleSlot());

        Reservation created = reservationService.createReservation(
                slot.getId(),
                "Jan Kowalski",
                "jan@example.com",
                2);

        assertEquals(ReservationStatus.PENDING, created.getStatus());
        assertEquals(2, availabilitySlotRepository.findById(slot.getId()).orElseThrow().getReservedCount());

        Reservation confirmed = reservationService.confirmReservation(created.getId());

        assertEquals(ReservationStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(2, availabilitySlotRepository.findById(slot.getId()).orElseThrow().getReservedCount());

        Reservation cancelled = reservationService.cancelReservation(created.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(0, availabilitySlotRepository.findById(slot.getId()).orElseThrow().getReservedCount());
    }

    @Test
    void rejectReservation_releasesCapacityOnPostgres() {
        AvailabilitySlot slot = availabilitySlotRepository.save(sampleSlot());
        Reservation created = reservationService.createReservation(
                slot.getId(),
                "Jan Kowalski",
                "jan@example.com",
                2);

        Reservation rejected = reservationService.rejectReservation(created.getId());

        assertEquals(ReservationStatus.REJECTED, rejected.getStatus());
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
}
