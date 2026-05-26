package com.github.wikor2115.reservation.security.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.github.wikor2115.reservation.security.jwt.JwtAuthenticationFilter;
import com.github.wikor2115.reservation.security.jwt.JwtProperties;
import com.github.wikor2115.reservation.security.jwt.JwtTokenService;

@AutoConfiguration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    JwtTokenService jwtTokenService(JwtProperties properties) {
        return new JwtTokenService(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        return new JwtAuthenticationFilter(jwtTokenService);
    }

    @Bean
    @ConditionalOnMissingBean
    UserDetailsService jwtOnlyUserDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("User account " + username + " not found");
        };
    }
}
