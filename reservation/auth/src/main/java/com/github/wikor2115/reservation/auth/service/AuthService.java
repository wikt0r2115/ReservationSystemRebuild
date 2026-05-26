package com.github.wikor2115.reservation.auth.service;

import java.time.Clock;
import java.util.Locale;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.wikor2115.reservation.auth.api.AuthTokenResponse;
import com.github.wikor2115.reservation.auth.domain.UserAccount;
import com.github.wikor2115.reservation.auth.repository.UserAccountRepository;
import com.github.wikor2115.reservation.security.AuthenticatedUser;
import com.github.wikor2115.reservation.security.jwt.JwtProperties;
import com.github.wikor2115.reservation.security.jwt.JwtTokenService;

@Service
@Transactional
public class AuthService {
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    @Autowired
    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            JwtProperties jwtProperties
    ) {
        this(userAccountRepository, passwordEncoder, jwtTokenService, jwtProperties, Clock.systemUTC());
    }

    AuthService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            JwtProperties jwtProperties,
            Clock clock
    ) {
        this.userAccountRepository = Objects.requireNonNull(userAccountRepository, "userAccountRepository must not be null");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder must not be null");
        this.jwtTokenService = Objects.requireNonNull(jwtTokenService, "jwtTokenService must not be null");
        this.jwtProperties = Objects.requireNonNull(jwtProperties, "jwtProperties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public UserAccount registerCustomer(String email, String displayName, String password) {
        String normalizedEmail = normalizeEmail(email);
        if (userAccountRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateUserAccountException(normalizedEmail);
        }

        UserAccount account = UserAccount.registerCustomer(
                normalizedEmail,
                displayName,
                passwordEncoder.encode(requireText(password, "password")),
                clock);
        return saveNewAccount(account);
    }

    public UserAccount createAdminIfMissing(String email, String displayName, String password) {
        String normalizedEmail = normalizeEmail(email);
        return userAccountRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> saveNewAccount(UserAccount.registerAdmin(
                        normalizedEmail,
                        displayName,
                        passwordEncoder.encode(requireText(password, "password")),
                        clock)));
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse login(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        String rawPassword = requireText(password, "password");
        UserAccount account = userAccountRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!account.isActive() || !passwordEncoder.matches(rawPassword, account.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtTokenService.createToken(new AuthenticatedUser(
                requirePersistedId(account),
                account.getEmail(),
                account.getRole()));
        return AuthTokenResponse.bearer(token, jwtProperties.expiration().toSeconds());
    }

    public void changePassword(AuthenticatedUser user, String currentPassword, String newPassword) {
        Objects.requireNonNull(user, "user must not be null");
        String rawCurrentPassword = requireText(currentPassword, "currentPassword");
        String rawNewPassword = requireText(newPassword, "newPassword");
        UserAccount account = userAccountRepository.findById(user.id())
                .orElseThrow(InvalidCredentialsException::new);

        if (!account.isActive()
                || !account.getEmail().equalsIgnoreCase(user.email())
                || !passwordEncoder.matches(rawCurrentPassword, account.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (passwordEncoder.matches(rawNewPassword, account.getPasswordHash())) {
            throw new IllegalArgumentException("newPassword must be different from currentPassword");
        }

        account.changePasswordHash(passwordEncoder.encode(rawNewPassword));
        userAccountRepository.save(account);
    }

    private UserAccount saveNewAccount(UserAccount account) {
        try {
            return userAccountRepository.save(account);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateUserAccountException(account.getEmail());
        }
    }

    private static Long requirePersistedId(UserAccount account) {
        if (account.getId() == null) {
            throw new IllegalStateException("User account must be persisted before issuing a token");
        }
        return account.getId();
    }

    private static String normalizeEmail(String value) {
        return requireText(value, "email").toLowerCase(Locale.ROOT);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
