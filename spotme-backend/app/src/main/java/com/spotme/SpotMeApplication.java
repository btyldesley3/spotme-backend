package com.spotme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * SpotMe backend entry point.
 *
 * <p>{@link UserDetailsServiceAutoConfiguration} is excluded because authentication is
 * entirely JWT-based; no in-memory {@code UserDetailsService} is needed or desired.
 */
@SpringBootApplication(
        scanBasePackages = "com.spotme",
        exclude = UserDetailsServiceAutoConfiguration.class
)
public class SpotMeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpotMeApplication.class, args);
    }
}
