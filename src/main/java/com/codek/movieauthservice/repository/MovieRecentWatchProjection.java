package com.codek.movieauthservice.repository;

public interface MovieRecentWatchProjection {
    Long getMovieId();
    Long getRecentWatchCount();
}
