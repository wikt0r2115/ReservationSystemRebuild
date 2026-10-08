package com.github.wikor2115.reservation.booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.github.wikor2115.reservation.availability.domain.AvailabilityCapacityExceededException;
import com.github.wikor2115.reservation.availability.domain.AvailabilitySlot;
import com.github.wikor2115.reservation.availability.repository.AvailabilitySlotRepository;
import com.github.wikor2115.reservation.availability.AvailabilityApplication;
import com.github.wikor2115.reservation.booking.BookingApplication;
import com.github.wikor2115.reservation.booking.repository.ReservationRepository;

@Testcontainers
class ReservationRestartPostgresTest {
    @Container
    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void restart_preservesReservationAndOccupiedCapacity() {
        Long slotId;
        try (ConfigurableApplicationContext firstRun = startBooking()) {
            AvailabilitySlotRepository slots = firstRun.getBean(AvailabilitySlotRepository.class);
            slotId = slots.save(AvailabilitySlot.create(
                    1L,
                    LocalDateTime.of(2099, 6, 2, 10, 0),
                    LocalDateTime.of(2099, 6, 2, 12, 0),
                    1)).getId();
            firstRun.getBean(ReservationService.class).createReservation(
                    slotId, "Jan Kowalski", "jan@example.com", 1);
        }

        try (ConfigurableApplicationContext availabilityRun = startAvailability()) {
            assertEquals(1, availabilityRun.getBean(AvailabilitySlotRepository.class)
                    .findById(slotId).orElseThrow().getReservedCount());
        }

        try (ConfigurableApplicationContext secondRun = startBooking()) {
            AvailabilitySlotRepository slots = secondRun.getBean(AvailabilitySlotRepository.class);
            ReservationRepository reservations = secondRun.getBean(ReservationRepository.class);
            ReservationService service = secondRun.getBean(ReservationService.class);

            assertEquals(1, slots.findById(slotId).orElseThrow().getReservedCount());
            assertEquals(1, reservations.count());
            assertThrows(AvailabilityCapacityExceededException.class,
                    () -> service.createReservation(slotId, "Anna Nowak", "anna@example.com", 1));
            assertEquals(1, slots.findById(slotId).orElseThrow().getReservedCount());
            assertEquals(1, reservations.count());
        }
    }

    private static ConfigurableApplicationContext startBooking() {
        return startApplication(BookingApplication.class, "booking");
    }

    private static ConfigurableApplicationContext startAvailability() {
        return startApplication(AvailabilityApplication.class, "availability");
    }

    private static ConfigurableApplicationContext startApplication(Class<?> application, String module) {
        return new SpringApplicationBuilder(application).run(
                "--server.port=0",
                "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                "--spring.datasource.username=" + POSTGRES.getUsername(),
                "--spring.datasource.password=" + POSTGRES.getPassword(),
                "--spring.datasource.driver-class-name=org.postgresql.Driver",
                "--spring.jpa.hibernate.ddl-auto=validate",
                "--spring.flyway.enabled=true",
                "--spring.flyway.locations=classpath:db/migration/" + module,
                "--spring.flyway.table=" + module + "_flyway_schema_history",
                "--spring.flyway.baseline-on-migrate=true",
                "--spring.flyway.baseline-version=0",
                "--spring.sql.init.mode=never");
    }
}
