package com.github.wikor2115.reservation.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {
        "com.github.wikor2115.reservation.booking.domain",
        "com.github.wikor2115.reservation.availability.domain"
})
@EnableJpaRepositories(basePackages = {
        "com.github.wikor2115.reservation.booking.repository",
        "com.github.wikor2115.reservation.availability.repository"
})
public class BookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingApplication.class, args);
    }
}
