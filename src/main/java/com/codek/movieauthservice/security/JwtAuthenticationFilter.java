package com.codek.movieauthservice.security;

import com.codek.movieauthservice.exception.InvalidTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter
 *
 * Per-request flow:
 *  1. Extract Bearer token from Authorization header
 *  2. Check token is not blacklisted (logged-out tokens)
 *  3. Validate JWT signature + expiry
 *  4. Load CustomUserDetails from DB to verify account is still active
 *  5. Set principal = CustomUserDetails in SecurityContext
 *     → controllers can use @AuthenticationPrincipal directly
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (jwt != null) {
                // Reject blacklisted (logged-out) tokens before any DB call
                if (tokenBlacklistService.isBlacklisted(jwt)) {
                    log.debug("Rejected blacklisted token");
                    filterChain.doFilter(request, response);
                    return;
                }

                if (tokenProvider.validateToken(jwt)) {
                    String username = tokenProvider.getUsernameFromToken(jwt);

                    // Load from DB to verify account is still active/not locked
                    CustomUserDetailsService.CustomUserDetails userDetails =
                            (CustomUserDetailsService.CustomUserDetails) userDetailsService.loadUserByUsername(username);

                    if (!userDetails.isEnabled()) {
                        log.warn("Account is disabled: {}", username);
                        filterChain.doFilter(request, response);
                        return;
                    }

                    // principal = userDetails → @AuthenticationPrincipal works in controllers
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Authenticated user: {}, roles: {}", username, userDetails.getAuthorities());
                }
            }
        } catch (InvalidTokenException e) {
            log.error("Token validation failed: {}", e.getMessage());
        } catch (Exception e) {
            log.error("JWT filter error: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
