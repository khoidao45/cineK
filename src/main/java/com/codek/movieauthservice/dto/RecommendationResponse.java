package com.codek.movieauthservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResponse {
    private MovieResponse movie;
    private double score;
    private double similarityScore;
    private double genreScore;
    private double trendingScore;
    private double ratingBoost;
}
