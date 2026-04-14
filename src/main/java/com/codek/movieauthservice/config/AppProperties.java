package com.codek.movieauthservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Centralizes all formerly-hardcoded tuning values so they can be adjusted
 * via application.properties without touching Java code.
 *
 * Recommendation weight contract: collabWeight + contentWeight + trendingWeight + coldStartWeight = 1.0
 */
@ConfigurationProperties(prefix = "app.tuning")
public record AppProperties(
        @DefaultValue("7")    int    trendingDays,
        @DefaultValue("10")   int    viewDedupMinutes,
        @DefaultValue("50")   int    recommendationLimit,
        @DefaultValue("100")  int    pageMaxSize,
        @DefaultValue("0.45") double collabWeight,
        @DefaultValue("0.30") double contentWeight,
        @DefaultValue("0.20") double trendingWeight,
        @DefaultValue("0.05") double coldStartWeight
) {}
