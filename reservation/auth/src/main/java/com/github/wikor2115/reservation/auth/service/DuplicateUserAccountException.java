package com.github.wikor2115.reservation.auth.service;

public class DuplicateUserAccountException extends RuntimeException {
    public DuplicateUserAccountException(String email) {
        super("User account already exists");
    }
}
