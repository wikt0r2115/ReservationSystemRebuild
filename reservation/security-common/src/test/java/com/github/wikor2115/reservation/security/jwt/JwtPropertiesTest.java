package com.github.wikor2115.reservation.security.jwt;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

    @Test
    void constructor_whenSecretBlank_throwsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new JwtProperties(" ", "reservation-system", Duration.ofMinutes(15)));
    }

    @Test
    void constructor_whenExpirationNotPositive_throwsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new JwtProperties("secret", "reservation-system", Duration.ZERO));
    }
}
