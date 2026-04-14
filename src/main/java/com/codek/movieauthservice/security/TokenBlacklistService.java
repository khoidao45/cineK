package com.codek.movieauthservice.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed token blacklist for logout invalidation.
 *
 * Each revoked token is stored in Redis as:
 *   KEY  → "blacklist:<jwt>"
 *   VALUE → "1"
 *   TTL  → remaining time until the token expires naturally
 *
 * Redis TTL handles cleanup automatically — no scheduled job needed.
 * Works correctly across multiple application instances.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    private static final String PREFIX = "blacklist:";

    public void blacklist(String token) {
        try {
            long expiryMs = jwtTokenProvider.getExpirationMs(token);
            long ttlSeconds = Math.max((expiryMs - System.currentTimeMillis()) / 1000, 1);
            redisTemplate.opsForValue().set(PREFIX + token, "1", Duration.ofSeconds(ttlSeconds));
            log.debug("token.blacklisted ttl={}s", ttlSeconds);
        } catch (Exception e) {
            // Token already expired or invalid — no need to track
            log.debug("token.blacklist.skip — already invalid: {}", e.getMessage());
        }
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + token));
    }
}
