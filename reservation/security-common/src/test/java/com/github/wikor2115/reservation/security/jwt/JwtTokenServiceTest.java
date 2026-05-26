package com.github.wikor2115.reservation.security.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wikor2115.reservation.security.AuthenticatedUser;
import com.github.wikor2115.reservation.security.UserRole;

class JwtTokenServiceTest {
    private static final JwtProperties PROPERTIES = new JwtProperties(
            "test-secret-that-is-long-enough-for-hmac",
            "reservation-system",
            Duration.ofMinutes(15));
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);
    private static final AuthenticatedUser USER = new AuthenticatedUser(10L, "customer@example.com", UserRole.CUSTOMER);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createToken_andParseToken_returnsAuthenticatedUser() {
        JwtTokenService tokenService = new JwtTokenService(PROPERTIES, CLOCK);

        String token = tokenService.createToken(USER);
        AuthenticatedUser result = tokenService.parseToken(token);

        assertEquals(USER, result);
    }

    @Test
    void createToken_containsRequiredClaims() throws Exception {
        JwtTokenService tokenService = new JwtTokenService(PROPERTIES, CLOCK);

        String token = tokenService.createToken(USER);
        Map<String, Object> payload = decodePayload(token);

        assertEquals("reservation-system", payload.get("iss"));
        assertEquals("10", payload.get("sub"));
        assertEquals("customer@example.com", payload.get("email"));
        assertEquals("CUSTOMER", payload.get("role"));
        assertEquals(CLOCK.instant().getEpochSecond(), ((Number) payload.get("iat")).longValue());
        assertEquals(CLOCK.instant().plus(PROPERTIES.expiration()).getEpochSecond(),
                ((Number) payload.get("exp")).longValue());
    }

    @Test
    void parseToken_whenExpired_throwsJwtTokenException() {
        JwtTokenService issuer = new JwtTokenService(PROPERTIES, CLOCK);
        JwtTokenService parser = new JwtTokenService(PROPERTIES, Clock.fixed(
                CLOCK.instant().plus(PROPERTIES.expiration()).plusSeconds(1),
                ZoneOffset.UTC));

        String token = issuer.createToken(USER);

        JwtTokenException exception = assertThrows(JwtTokenException.class, () -> parser.parseToken(token));
        assertTrue(exception.getMessage().contains("expired"));
    }

    @Test
    void parseToken_whenSignatureInvalid_throwsJwtTokenException() {
        JwtTokenService tokenService = new JwtTokenService(PROPERTIES, CLOCK);
        String token = tokenService.createToken(USER);
        String tamperedToken = token.substring(0, token.length() - 1) + "x";

        JwtTokenException exception = assertThrows(JwtTokenException.class, () -> tokenService.parseToken(tamperedToken));
        assertTrue(exception.getMessage().contains("signature"));
    }

    @Test
    void parseToken_whenIssuerInvalid_throwsJwtTokenException() {
        JwtTokenService issuer = new JwtTokenService(PROPERTIES, CLOCK);
        JwtTokenService parser = new JwtTokenService(
                new JwtProperties(PROPERTIES.secret(), "other-issuer", PROPERTIES.expiration()),
                CLOCK);

        String token = issuer.createToken(USER);

        JwtTokenException exception = assertThrows(JwtTokenException.class, () -> parser.parseToken(token));
        assertTrue(exception.getMessage().contains("issuer"));
    }

    private Map<String, Object> decodePayload(String token) throws Exception {
        String encodedPayload = token.split("\\.")[1];
        byte[] payload = Base64.getUrlDecoder().decode(encodedPayload);
        return objectMapper.readValue(new String(payload, StandardCharsets.UTF_8), new TypeReference<>() {});
    }
}
