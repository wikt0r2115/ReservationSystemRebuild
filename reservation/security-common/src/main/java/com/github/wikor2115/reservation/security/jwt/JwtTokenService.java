package com.github.wikor2115.reservation.security.jwt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wikor2115.reservation.security.AuthenticatedUser;
import com.github.wikor2115.reservation.security.UserRole;

public class JwtTokenService {
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final JwtProperties properties;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public JwtTokenService(JwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    public JwtTokenService(JwtProperties properties, Clock clock) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.objectMapper = new ObjectMapper();
    }

    public String createToken(AuthenticatedUser user) {
        Objects.requireNonNull(user, "user must not be null");
        Instant now = Instant.now(clock);

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", properties.issuer());
        payload.put("sub", user.id().toString());
        payload.put("email", user.email());
        payload.put("role", user.role().name());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plus(properties.expiration()).getEpochSecond());

        String unsignedToken = encodeJson(header) + "." + encodeJson(payload);
        return unsignedToken + "." + sign(unsignedToken);
    }

    public AuthenticatedUser parseToken(String token) {
        String[] parts = splitToken(token);
        verifySignature(parts[0] + "." + parts[1], parts[2]);
        Map<String, Object> payload = decodePayload(parts[1]);

        String issuer = requireString(payload, "iss");
        if (!properties.issuer().equals(issuer)) {
            throw new JwtTokenException("JWT issuer is invalid");
        }

        long expiresAt = requireLong(payload, "exp");
        if (expiresAt <= Instant.now(clock).getEpochSecond()) {
            throw new JwtTokenException("JWT token has expired");
        }

        Long userId = parseUserId(requireString(payload, "sub"));
        String email = requireString(payload, "email");
        UserRole role = parseRole(requireString(payload, "role"));
        return new AuthenticatedUser(userId, email, role);
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException exception) {
            throw new JwtTokenException("JWT payload cannot be serialized", exception);
        }
    }

    private Map<String, Object> decodePayload(String encodedPayload) {
        try {
            return objectMapper.readValue(BASE64_URL_DECODER.decode(encodedPayload), MAP_TYPE);
        } catch (RuntimeException | IOException exception) {
            throw new JwtTokenException("JWT payload is invalid", exception);
        }
    }

    private String[] splitToken(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtTokenException("JWT token is blank");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new JwtTokenException("JWT token format is invalid");
        }
        return parts;
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new JwtTokenException("JWT token cannot be signed", exception);
        }
    }

    private void verifySignature(String unsignedToken, String signature) {
        byte[] expected = sign(unsignedToken).getBytes(StandardCharsets.UTF_8);
        byte[] actual = signature.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new JwtTokenException("JWT signature is invalid");
        }
    }

    private String requireString(Map<String, Object> payload, String claim) {
        Object value = payload.get(claim);
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        throw new JwtTokenException("JWT claim is missing: " + claim);
    }

    private long requireLong(Map<String, Object> payload, String claim) {
        Object value = payload.get(claim);
        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }
        throw new JwtTokenException("JWT claim is missing: " + claim);
    }

    private Long parseUserId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new JwtTokenException("JWT subject is invalid", exception);
        }
    }

    private UserRole parseRole(String value) {
        try {
            return UserRole.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new JwtTokenException("JWT role is invalid", exception);
        }
    }
}
