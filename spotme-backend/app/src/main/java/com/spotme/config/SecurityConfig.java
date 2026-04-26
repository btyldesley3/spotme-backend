package com.spotme.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotme.adapters.in.rest.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Instant;
import java.util.Map;

/**
 * Stateless JWT security configuration.
 *
 * <p>All routes under {@code /api/v1/**} require a valid Bearer token.
 * Public routes ({@code /api/auth/**}, actuator health/info) are explicitly permitted.
 *
 * <p>Unauthenticated requests receive a {@code 401 Unauthorized} JSON response.
 * Authenticated but unauthorised requests receive a {@code 403 Forbidden} JSON response.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .anonymous(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public auth endpoints only — logout is intentionally excluded (requires JWT)
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        // 401 — no token or invalid token
                        .authenticationEntryPoint((request, response, authException) ->
                                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        "UNAUTHORIZED", "Authentication required: " + authException.getMessage()))
                        // 403 — valid token but insufficient privileges
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                                        "FORBIDDEN", accessDeniedException.getMessage()))
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private void writeJson(HttpServletResponse response, int status, String error, String message) {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        try {
            var body = Map.of(
                    "timestamp", Instant.now().toString(),
                    "status", status,
                    "error", error,
                    "message", message
            );
            response.getWriter().write(objectMapper.writeValueAsString(body));
        } catch (Exception ignored) {
            // If JSON serialisation fails, the status code is still set correctly.
        }
    }
}


