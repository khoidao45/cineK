package com.codek.movieauthservice.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    // Mỗi IP có bucket riêng — lưu trong memory (dùng Redis nếu multi-instance)
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket createLoginBucket() {
        // 5 request / 15 phút
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(15)));
        return Bucket.builder().addLimit(limit).build();
    }

    public boolean tryConsume(String ipAddress) {
        Bucket bucket = buckets.computeIfAbsent(ipAddress, k -> createLoginBucket());
        return bucket.tryConsume(1);
    }

    public long getAvailableTokens(String ipAddress) {
        Bucket bucket = buckets.computeIfAbsent(ipAddress, k -> createLoginBucket());
        return bucket.getAvailableTokens();
    }
}