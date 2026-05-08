package com.teamresource.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.teamresource.booking.api",
        "com.teamresource.booking.config",
        "com.teamresource.booking.infra",
        "com.teamresource.booking.infrastructure.messaging",
        "com.teamresource.booking.service"
})
@EntityScan(basePackages = "com.teamresource.booking.infra.persistence")
@EnableJpaRepositories(basePackages = "com.teamresource.booking.infra.persistence")
@EnableScheduling
public class BookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }
}
