package com.codek.movieauthservice.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Distributed rate limiting backed by Redis via Bucket4j.
 * Each IP address gets its own bucket: 5 requests per 15 minutes.
 *
 * Keys are prefixed with "rl:" to avoid collisions with other Redis keys.
 */
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final ProxyManager<byte[]> rateLimitProxyManager;

    private static final BucketConfiguration LOGIN_BUCKET_CONFIG = BucketConfiguration.builder()
            .addLimit(Bandwidth.builder()
                    .capacity(5)
                    .refillIntervally(5, Duration.ofMinutes(15))
                    .build())
            .build();

    public boolean tryConsume(String ipAddress) {
        return resolveBucket(ipAddress).tryConsume(1);
    }

    public long getAvailableTokens(String ipAddress) {
        return resolveBucket(ipAddress).getAvailableTokens();
    }

    private Bucket resolveBucket(String ipAddress) {
        byte[] key = ("rl:" + ipAddress).getBytes(StandardCharsets.UTF_8);
        return rateLimitProxyManager.builder().build(key, () -> LOGIN_BUCKET_CONFIG);
    }
}
