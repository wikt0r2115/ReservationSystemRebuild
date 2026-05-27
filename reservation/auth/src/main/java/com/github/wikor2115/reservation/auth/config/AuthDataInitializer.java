package com.github.wikor2115.reservation.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.wikor2115.reservation.auth.service.AuthService;

@Configuration
public class AuthDataInitializer {

    @Bean
    ApplicationRunner seedAdminAccount(
            AuthService authService,
            @Value("${reservation.auth.admin.seed.enabled:false}") boolean enabled,
            @Value("${reservation.auth.admin.seed.email:admin@example.com}") String email,
            @Value("${reservation.auth.admin.seed.display-name:Admin}") String displayName,
            @Value("${reservation.auth.admin.seed.password}") String password
    ) {
        return args -> {
            if (enabled) {
                authService.createAdminIfMissing(email, displayName, password);
            }
        };
    }
}
