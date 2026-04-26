package com.spotme.adapters.in.rest.security;

import com.spotme.domain.port.PasswordHashPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Beans that live in the REST adapter module related to security infrastructure.
 * The SecurityFilterChain itself is declared in the app/ module's SecurityConfig.
 */
@Configuration
public class RestSecurityBeans {

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


