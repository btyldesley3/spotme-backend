package com.spotme.adapters.in.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Assigns a correlation ID to every inbound request.
 *
 * <p>The ID is sourced from the {@code X-Request-ID} request header when present,
 * otherwise a random UUID is generated. The resolved value is:
 * <ul>
 *   <li>stored in {@link MDC} under key {@code requestId} so it appears in every log line</li>
 *   <li>echoed back to the caller via the {@code X-Request-ID} response header</li>
 * </ul>
 *
 * <p>Runs at highest precedence so all downstream filters and handlers see the MDC entry.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            // Always clean up MDC to prevent leaks in thread-pool environments.
            MDC.remove(MDC_KEY);
        }
    }
}

