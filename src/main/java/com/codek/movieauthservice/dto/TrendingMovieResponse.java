package com.codek.movieauthservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrendingMovieResponse {
    private MovieResponse movie;
    private double trendingScore;
    private long recentWatchCount;
}
