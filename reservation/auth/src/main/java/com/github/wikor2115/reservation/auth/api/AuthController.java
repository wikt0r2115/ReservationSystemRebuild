package com.github.wikor2115.reservation.auth.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.wikor2115.reservation.auth.domain.UserAccount;
import com.github.wikor2115.reservation.auth.service.AuthService;
import com.github.wikor2115.reservation.security.AuthenticatedUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@Validated
@Tag(name = "Authentication", description = "Customer registration, login and password management")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register customer", description = "Creates a customer account with an email and password.")
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
    @Operation(summary = "Login", description = "Authenticates a user and returns a JWT bearer token.")
    public AuthTokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Changes the password for the authenticated user.")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        authService.changePassword(
                authenticatedUser(authentication),
                request.currentPassword(),
                request.newPassword());
        return ResponseEntity.noContent().build();
    }

    private static AuthenticatedUser authenticatedUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AccessDeniedException("Authenticated user is required");
        }
        return user;
    }
}
