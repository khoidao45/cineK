package com.codek.movieauthservice.mapper;

import com.codek.movieauthservice.dto.MovieRequest;
import com.codek.movieauthservice.dto.MovieResponse;
import com.codek.movieauthservice.entity.Movie;
import org.springframework.stereotype.Component;

@Component
public class MovieMapper {

    public MovieResponse toResponse(Movie movie) {
        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .genre(movie.getGenre())
                .duration(movie.getDuration())
                .releaseYear(movie.getReleaseYear())
                .posterUrl(movie.getPosterUrl())
                .thumbnailUrl(movie.getThumbnailUrl())
                .videoUrl(movie.getVideoUrl())
                .views(movie.getViews())
                .ratingAvg(movie.getRatingAvg())
                .ratingCount(movie.getRatingCount())
                .createdAt(movie.getCreatedAt())
                .updatedAt(movie.getUpdatedAt())
                .build();
    }

    public Movie toEntity(MovieRequest request) {
        return Movie.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .genre(request.getGenre())
                .duration(request.getDuration())
                .releaseYear(request.getReleaseYear())
                .posterUrl(request.getPosterUrl())
                .thumbnailUrl(request.getThumbnailUrl())
                .videoUrl(request.getVideoUrl())
                .build();
    }
}
