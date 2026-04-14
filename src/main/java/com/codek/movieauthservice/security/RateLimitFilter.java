package com.codek.movieauthservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rate limiting filter — applied only to /api/auth/** to prevent brute-force attacks.
 * Uses RateLimitService (Bucket4j): 5 requests per 15 minutes per IP.
 *
 * Must be registered BEFORE JwtAuthenticationFilter in SecurityConfig.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.equals("/api/auth/login") || path.equals("/api/auth/register")) {
            String ip = request.getRemoteAddr();
            try {
                if (!rateLimitService.tryConsume(ip)) {
                    log.warn("rate-limit.exceeded ip={}", ip);
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("""
                            {"status":429,"message":"Too many requests. Please try again later.","error":"Rate Limit Exceeded"}
                            """);
                    return;
                }
            } catch (Exception e) {
                // Fail open: if Redis is unavailable, allow the request rather than blocking all logins
                log.error("rate-limit.redis-error ip={} — allowing request: {}", ip, e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
