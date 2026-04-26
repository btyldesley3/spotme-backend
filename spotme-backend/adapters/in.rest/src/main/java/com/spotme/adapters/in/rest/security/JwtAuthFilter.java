package com.spotme.adapters.in.rest.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Validates the JWT Bearer token on every request that carries one.
 *
 * <ul>
 *   <li>No {@code Authorization} header → pass-through (Spring Security's
 *       {@code AuthenticationEntryPoint} will reject the request if the route is protected).</li>
 *   <li>{@code Bearer} header present, token valid → populates {@link SecurityContextHolder}
 *       so the request is treated as authenticated.</li>
 *   <li>{@code Bearer} header present, token invalid / expired → immediately returns
 *       {@code 401 Unauthorized} with a JSON body; the filter chain is not continued.</li>
 * </ul>
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var header = request.getHeader("Authorization");

        // No token supplied – pass through; the security chain will enforce auth if required.
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        var token = header.substring(BEARER_PREFIX.length());
        try {
            var userId = jwtService.extractUserId(token);
            var auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(request, response);
        } catch (JwtException ex) {
            // Token was supplied but is invalid / expired — short-circuit with 401.
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"status\":401,\"error\":\"INVALID_TOKEN\",\"message\":\"JWT is invalid or has expired\"}");
        }
    }
}

