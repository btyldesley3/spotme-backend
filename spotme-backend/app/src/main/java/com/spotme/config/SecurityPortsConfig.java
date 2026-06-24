package com.spotme.config;

import com.spotme.domain.port.PasswordHashPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class SecurityPortsConfig {

    @Bean
    public PasswordHashPort passwordHashPort() {
        var encoder = new BCryptPasswordEncoder();
        return new PasswordHashPort() {
            @Override
            public String hash(String rawPassword) {
                return encoder.encode(rawPassword);
            }

            @Override
            public boolean verify(String rawPassword, String storedHash) {
                return encoder.matches(rawPassword, storedHash);
            }
        };
    }
}

