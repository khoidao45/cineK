package com.codek.movieauthservice.repository;

import java.math.BigDecimal;

/**
 * PostgreSQL {@code AVG(integer)} is mapped as {@link BigDecimal}, not {@link Double},
 * which can break Spring Data interface projections and cause 500s at runtime.
 */
public interface MovieRatingAggregateProjection {
    Long getRatingCount();

    BigDecimal getRatingAvg();
}
