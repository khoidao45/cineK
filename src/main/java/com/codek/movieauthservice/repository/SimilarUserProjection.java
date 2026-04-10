package com.codek.movieauthservice.repository;

public interface SimilarUserProjection {
    Long getUserId();
    Long getOverlapCount();
}
