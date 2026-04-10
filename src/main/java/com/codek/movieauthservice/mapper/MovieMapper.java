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
                .createdAt(movie.getCreatedAt())
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
                .build();
    }
}
