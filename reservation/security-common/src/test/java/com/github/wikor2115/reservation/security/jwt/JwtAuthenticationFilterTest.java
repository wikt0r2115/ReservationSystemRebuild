package com.github.wikor2115.reservation.security.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.github.wikor2115.reservation.security.AuthenticatedUser;
import com.github.wikor2115.reservation.security.UserRole;

import jakarta.servlet.http.HttpServletResponse;

class JwtAuthenticationFilterTest {
    private static final JwtProperties PROPERTIES = new JwtProperties(
            "test-secret-that-is-long-enough-for-hmac",
            "reservation-auth-test",
            Duration.ofHours(1));

    private final JwtTokenService jwtTokenService = new JwtTokenService(PROPERTIES);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenService);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_whenBearerTokenValid_setsAuthentication() throws Exception {
        String token = jwtTokenService.createToken(new AuthenticatedUser(10L, "admin@example.com", UserRole.ADMIN));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("admin@example.com", ((AuthenticatedUser) authentication.getPrincipal()).email());
        assertEquals("ROLE_ADMIN", authentication.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void doFilterInternal_whenBearerTokenInvalid_returnsUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertFalse(response.getContentAsString().contains("JWT strings must contain exactly 2 period characters"));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
