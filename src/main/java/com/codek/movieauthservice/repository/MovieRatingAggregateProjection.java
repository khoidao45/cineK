package com.codek.movieauthservice.repository;

public interface MovieRatingAggregateProjection {
    Long getRatingCount();
    Double getRatingAvg();
}
