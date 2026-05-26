package com.github.wikor2115.reservation.security.jwt;

public class JwtTokenException extends RuntimeException {
    public JwtTokenException(String message) {
        super(message);
    }

    public JwtTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
