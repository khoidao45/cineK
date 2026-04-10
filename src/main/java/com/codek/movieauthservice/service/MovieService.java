package com.codek.movieauthservice.service;

import com.codek.movieauthservice.dto.MovieRequest;
import com.codek.movieauthservice.dto.MovieResponse;
import com.codek.movieauthservice.entity.Movie;
import com.codek.movieauthservice.exception.MovieNotFoundException;
import com.codek.movieauthservice.mapper.MovieMapper;
import com.codek.movieauthservice.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;

    public Page<MovieResponse> getAllMovies(Pageable pageable) {
        return movieRepository.findAll(pageable)
                .map(movieMapper::toResponse);
    }

    public MovieResponse getMovieById(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new MovieNotFoundException("Không tìm thấy phim với ID: " + id));
        return movieMapper.toResponse(movie);
    }

    public Page<MovieResponse> searchByTitle(String title, Pageable pageable) {
        return movieRepository.findByTitleContainingIgnoreCase(title, pageable)
                .map(movieMapper::toResponse);
    }

    public Page<MovieResponse> filterByGenre(String genre, Pageable pageable) {
        return movieRepository.findByGenreIgnoreCase(genre, pageable)
                .map(movieMapper::toResponse);
    }

    public MovieResponse createMovie(MovieRequest request) {
        Movie movie = movieMapper.toEntity(request);
        Movie savedMovie = movieRepository.save(movie);
        return movieMapper.toResponse(savedMovie);
    }

    public MovieResponse updateMovie(Long id, MovieRequest request) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new MovieNotFoundException("Không tìm thấy phim với ID: " + id));
        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
        movie.setGenre(request.getGenre());
        movie.setDuration(request.getDuration());
        movie.setReleaseYear(request.getReleaseYear());
        movie.setPosterUrl(request.getPosterUrl());
        Movie updatedMovie = movieRepository.save(movie);
        return movieMapper.toResponse(updatedMovie);
    }

    public void deleteMovie(Long id) {
        if (!movieRepository.existsById(id)) {
            throw new MovieNotFoundException("Không tìm thấy phim với ID: " + id);
        }
        movieRepository.deleteById(id);
    }

    // Internal helper — used by WatchHistoryService and ReviewService
    public Movie findMovieEntityById(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new MovieNotFoundException("Không tìm thấy phim với ID: " + id));
    }
}
