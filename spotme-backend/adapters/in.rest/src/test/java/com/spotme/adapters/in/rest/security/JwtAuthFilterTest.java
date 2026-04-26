package com.spotme.adapters.in.rest.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JwtAuthFilter}.
 *
 * <p>Verifies all three branches:
 * <ol>
 *   <li>No Authorization header → chain continues, SecurityContext left empty.</li>
 *   <li>Valid Bearer token → SecurityContext populated, chain continues.</li>
 *   <li>Invalid / expired Bearer token → 401 JSON response, chain is NOT continued.</li>
 * </ol>
 */
class JwtAuthFilterTest {

    private JwtService jwtService;
    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        filter = new JwtAuthFilter(jwtService);
        SecurityContextHolder.clearContext();
    }

    // ── No Authorization header ───────────────────────────────────────────────

    @Test
    void noAuthorizationHeader_chainContinues_securityContextIsEmpty() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService, never()).extractUserId(anyString());
    }

    @Test
    void authorizationHeaderWithoutBearerPrefix_chainContinues() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // ── Valid Bearer token ────────────────────────────────────────────────────

    @Test
    void validBearerToken_populatesSecurityContext_chainContinues() throws Exception {
        var token = "valid.jwt.token";
        var userId = "user-123";
        when(jwtService.extractUserId(token)).thenReturn(userId);

        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.isAuthenticated());
        assertEquals(userId, auth.getName());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    // ── Invalid / expired Bearer token ───────────────────────────────────────

    @Test
    void invalidBearerToken_returns401Json_chainIsNotContinued() throws Exception {
        var token = "expired.or.tampered.token";
        when(jwtService.extractUserId(token)).thenThrow(new JwtException("Token expired"));

        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        // Chain must NOT be called — the filter short-circuits with 401.
        verify(chain, never()).doFilter(request, response);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertTrue(response.getContentAsString().contains("INVALID_TOKEN"));

        // SecurityContext must be cleared (no leftover auth from a previous request).
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}


