package com.github.wikor2115.reservation.auth.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.wikor2115.reservation.auth.domain.UserAccount;
import com.github.wikor2115.reservation.auth.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserAccountResponse> registerCustomer(
            @Valid @RequestBody RegisterCustomerRequest request
    ) {
        UserAccount account = authService.registerCustomer(
                request.email(),
                request.displayName(),
                request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserAccountResponse.from(account));
    }

    @PostMapping("/login")
    public AuthTokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }
}
