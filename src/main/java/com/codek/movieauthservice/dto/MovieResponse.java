package com.codek.movieauthservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieResponse {
    private Long id;
    private String title;
    private String description;
    private String genre;
    private int duration;
    private int releaseYear;
    private String posterUrl;
    private String thumbnailUrl;
    private String videoUrl;
    private String director;
    private String actors;
    private long views;
    private double ratingAvg;
    private long ratingCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
