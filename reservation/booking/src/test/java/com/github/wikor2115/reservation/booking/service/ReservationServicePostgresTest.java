package com.github.wikor2115.reservation.booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.github.wikor2115.reservation.availability.domain.AvailabilitySlot;
import com.github.wikor2115.reservation.availability.repository.AvailabilitySlotRepository;
import com.github.wikor2115.reservation.availability.service.AvailabilityService;
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
@Testcontainers
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

    @Autowired
    private PlatformTransactionManager transactionManager;

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

    @Test
    void concurrentReservations_forLastPlace_allowExactlyOneCustomer() throws Exception {
        assertExactlyOneReservationWins(2);
    }

    @Test
    void fiveConcurrentCustomers_forLastPlace_allowExactlyOneCustomer() throws Exception {
        assertExactlyOneReservationWins(5);
    }

    private void assertExactlyOneReservationWins(int customerCount) throws Exception {
        AvailabilitySlot slot = availabilitySlotRepository.save(AvailabilitySlot.create(
                1L,
                LocalDateTime.of(2099, 6, 2, 10, 0),
                LocalDateTime.of(2099, 6, 2, 12, 0),
                1,
                CLOCK));
        CyclicBarrier bothReadSlot = new CyclicBarrier(customerCount);
        ExecutorService executor = Executors.newFixedThreadPool(customerCount);

        try {
            List<Future<Long>> attempts = IntStream.range(0, customerCount)
                    .mapToObj(index -> executor.submit(() -> reserveAfterReadingSlot(
                            slot.getId(), "customer" + index + "@example.com", bothReadSlot)))
                    .toList();
            List<Long> successes = new ArrayList<>();
            List<Throwable> failures = new ArrayList<>();
            for (Future<Long> attempt : attempts) {
                try {
                    successes.add(attempt.get(20, TimeUnit.SECONDS));
                } catch (ExecutionException exception) {
                    failures.add(exception.getCause());
                }
            }

            assertEquals(1, successes.size(), "Exactly one customer should reserve the last place: " + failures);
            assertEquals(customerCount - 1, failures.size());
            for (Throwable failure : failures) {
                assertTrue(failure instanceof OptimisticLockingFailureException,
                        "The losing transaction should report a concurrent change: " + failure);
            }
            assertEquals(1, reservationRepository.findAll().size());
            assertEquals(1, availabilitySlotRepository.findById(slot.getId()).orElseThrow().getReservedCount());
            assertTrue(reservationRepository.findById(successes.get(0)).isPresent());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentCancellationAndRejection_releaseTheSameReservationOnlyOnce() throws Exception {
        AvailabilitySlot slot = availabilitySlotRepository.save(sampleSlot());
        Reservation target = reservationService.createReservation(
                slot.getId(), "Jan Kowalski", "jan@example.com", 1);
        Reservation other = reservationService.createReservation(
                slot.getId(), "Anna Nowak", "anna@example.com", 1);
        CyclicBarrier bothReadState = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            List<Future<Boolean>> attempts = List.of(
                    executor.submit(() -> changeReservationAfterReadingState(
                            slot.getId(), target.getId(), bothReadState,
                            () -> reservationService.cancelReservation(target.getId()))),
                    executor.submit(() -> changeReservationAfterReadingState(
                            slot.getId(), target.getId(), bothReadState,
                            () -> reservationService.rejectReservation(target.getId()))));
            int successes = 0;
            List<Throwable> failures = new ArrayList<>();
            for (Future<Boolean> attempt : attempts) {
                try {
                    assertTrue(attempt.get(20, TimeUnit.SECONDS));
                    successes++;
                } catch (ExecutionException exception) {
                    failures.add(exception.getCause());
                }
            }

            assertEquals(1, successes, "Only one state change should commit: " + failures);
            assertEquals(1, failures.size());
            assertTrue(failures.get(0) instanceof OptimisticLockingFailureException);
            assertEquals(1, availabilitySlotRepository.findById(slot.getId()).orElseThrow().getReservedCount());
            assertTrue(List.of(ReservationStatus.CANCELLED, ReservationStatus.REJECTED)
                    .contains(reservationRepository.findById(target.getId()).orElseThrow().getStatus()));
            assertEquals(ReservationStatus.PENDING,
                    reservationRepository.findById(other.getId()).orElseThrow().getStatus());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentCapacityChangeAndBooking_keepReservationsWithinCapacity() throws Exception {
        AvailabilitySlot slot = availabilitySlotRepository.save(sampleSlot());
        AvailabilityService availabilityService = new AvailabilityService(availabilitySlotRepository);
        CyclicBarrier bothReadSlot = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            List<Future<Boolean>> attempts = List.of(
                    executor.submit(() -> changeReservationAfterReadingState(
                            slot.getId(), null, bothReadSlot,
                            () -> availabilityService.updateSlotById(slot.getId(), null, null, 1))),
                    executor.submit(() -> changeReservationAfterReadingState(
                            slot.getId(), null, bothReadSlot,
                            () -> reservationService.createReservation(
                                    slot.getId(), "Jan Kowalski", "jan@example.com", 2))));
            int successes = 0;
            List<Throwable> failures = new ArrayList<>();
            for (Future<Boolean> attempt : attempts) {
                try {
                    assertTrue(attempt.get(20, TimeUnit.SECONDS));
                    successes++;
                } catch (ExecutionException exception) {
                    failures.add(exception.getCause());
                }
            }

            assertEquals(1, successes, "Only one slot change should commit: " + failures);
            assertEquals(1, failures.size());
            assertTrue(failures.get(0) instanceof OptimisticLockingFailureException);
            AvailabilitySlot persisted = availabilitySlotRepository.findById(slot.getId()).orElseThrow();
            assertTrue(persisted.getReservedCount() <= persisted.getCapacity());
            assertEquals(reservationRepository.findAll().stream().mapToInt(Reservation::getPartySize).sum(),
                    persisted.getReservedCount());
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean changeReservationAfterReadingState(
            Long slotId, Long reservationId, CyclicBarrier barrier, Runnable action) {
        return Boolean.TRUE.equals(new TransactionTemplate(transactionManager).execute(status -> {
            availabilitySlotRepository.findById(slotId).orElseThrow();
            if (reservationId != null) {
                reservationRepository.findById(reservationId).orElseThrow();
            }
            await(barrier);
            action.run();
            return true;
        }));
    }

    private Long reserveAfterReadingSlot(Long slotId, String email, CyclicBarrier bothReadSlot) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            availabilitySlotRepository.findById(slotId).orElseThrow();
            await(bothReadSlot);
            return reservationService.createReservation(slotId, email, email, 1).getId();
        });
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for concurrent transactions", exception);
        } catch (BrokenBarrierException | TimeoutException exception) {
            throw new IllegalStateException("Transactions did not reach the same starting point", exception);
        }
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
