package com.codek.movieauthservice.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory token blacklist for logout invalidation.
 *
 * Production upgrade: replace with Redis (SETEX token "" ttlSeconds)
 * so blacklist survives restarts and works across multiple instances.
 *
 * Map<token, expiryEpochMs> — scheduled cleanup removes expired entries
 * so memory stays bounded even with many logouts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final JwtTokenProvider jwtTokenProvider;

    // token -> expiry time in epoch ms
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    public void blacklist(String token) {
        try {
            long expiryMs = jwtTokenProvider.getExpirationMs(token);
            blacklist.put(token, expiryMs);
            log.debug("Token blacklisted, expires at epoch ms: {}", expiryMs);
        } catch (Exception e) {
            // Token already invalid — no need to track
            log.debug("Skipped blacklisting already-invalid token");
        }
    }

    public boolean isBlacklisted(String token) {
        return blacklist.containsKey(token);
    }

    /** Remove expired tokens every hour so memory stays bounded. */
    @Scheduled(fixedDelay = 3_600_000)
    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        int before = blacklist.size();
        blacklist.entrySet().removeIf(entry -> entry.getValue() < now);
        log.debug("Blacklist cleanup: removed {} expired tokens", before - blacklist.size());
    }
}
