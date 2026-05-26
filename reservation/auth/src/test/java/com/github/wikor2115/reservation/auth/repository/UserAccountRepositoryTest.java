package com.github.wikor2115.reservation.auth.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.github.wikor2115.reservation.auth.domain.UserAccount;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserAccountRepositoryTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);

    @Autowired
    private UserAccountRepository userAccountRepository;

    @BeforeEach
    void setUp() {
        userAccountRepository.deleteAll();
    }

    @Test
    void save_persistsUserAccount() {
        UserAccount saved = userAccountRepository.save(sampleAccount());

        UserAccount found = userAccountRepository.findById(saved.getId()).orElseThrow();

        assertEquals(saved.getId(), found.getId());
        assertEquals("jan@example.com", found.getEmail());
        assertEquals("Jan Kowalski", found.getDisplayName());
    }

    @Test
    void findByEmailIgnoreCase_returnsMatchingAccount() {
        UserAccount saved = userAccountRepository.save(sampleAccount());

        UserAccount found = userAccountRepository.findByEmailIgnoreCase("JAN@EXAMPLE.COM").orElseThrow();

        assertEquals(saved.getId(), found.getId());
    }

    @Test
    void existsByEmailIgnoreCase_whenMatchingAccountExists_returnsTrue() {
        userAccountRepository.save(sampleAccount());

        assertTrue(userAccountRepository.existsByEmailIgnoreCase("JAN@EXAMPLE.COM"));
    }

    private static UserAccount sampleAccount() {
        return UserAccount.registerCustomer(
                "jan@example.com",
                "Jan Kowalski",
                "{bcrypt}hash",
                CLOCK);
    }
}
