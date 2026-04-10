package com.codek.movieauthservice.service;

import com.codek.movieauthservice.dto.MovieRequest;
import com.codek.movieauthservice.dto.MovieResponse;
import com.codek.movieauthservice.dto.PageResponse;
import com.codek.movieauthservice.dto.TrendingMovieResponse;
import com.codek.movieauthservice.dto.VideoPlaybackResponse;
import com.codek.movieauthservice.entity.Movie;
import com.codek.movieauthservice.exception.MovieNotFoundException;
import com.codek.movieauthservice.mapper.MovieMapper;
import com.codek.movieauthservice.repository.MovieRatingAggregateProjection;
import com.codek.movieauthservice.repository.MovieRecentWatchProjection;
import com.codek.movieauthservice.repository.MovieRepository;
import com.codek.movieauthservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final ReviewRepository reviewRepository;
    private final MovieMapper movieMapper;

    @Transactional(readOnly = true)
    @Cacheable(value = "movies", key = "#page + '-' + #size + '-' + (#keyword == null ? '' : #keyword) + '-' + (#genre == null ? '' : #genre)")
    public PageResponse<MovieResponse> getMovies(int page, int size, String keyword, String genre) {
        int sanitizedPage = Math.max(page - 1, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 100);

        Page<MovieResponse> moviePage = movieRepository
                .searchByKeywordAndGenre(keyword, genre, Pageable.ofSize(sanitizedSize).withPage(sanitizedPage))
                .map(movieMapper::toResponse);

        return toPageResponse(moviePage, page, sanitizedSize);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "movieById", key = "#id")
    public MovieResponse getMovieById(Long id) {
        Movie movie = movieRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new MovieNotFoundException("Không tìm thấy phim với ID: " + id));
        return movieMapper.toResponse(movie);
    }

    @Transactional
    @CacheEvict(value = {"movies", "movieById", "trendingMovies"}, allEntries = true)
    public MovieResponse createMovie(MovieRequest request) {
        validateReleaseYear(request.getReleaseYear());
        Movie movie = movieMapper.toEntity(request);
        Movie savedMovie = movieRepository.save(movie);
        return movieMapper.toResponse(savedMovie);
    }

    @Transactional
    @CacheEvict(value = {"movies", "movieById", "trendingMovies"}, allEntries = true)
    public MovieResponse updateMovie(Long id, MovieRequest request) {
        validateReleaseYear(request.getReleaseYear());
        Movie movie = movieRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new MovieNotFoundException("Không tìm thấy phim với ID: " + id));
        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
        movie.setGenre(request.getGenre());
        movie.setDuration(request.getDuration());
        movie.setReleaseYear(request.getReleaseYear());
        movie.setPosterUrl(request.getPosterUrl());
        movie.setThumbnailUrl(request.getThumbnailUrl());
        movie.setVideoUrl(request.getVideoUrl());
        Movie updatedMovie = movieRepository.save(movie);
        return movieMapper.toResponse(updatedMovie);
    }

    @Transactional
    @CacheEvict(value = {"movies", "movieById", "trendingMovies"}, allEntries = true)
    public void deleteMovie(Long id) {
        Movie movie = movieRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new MovieNotFoundException("Không tìm thấy phim với ID: " + id));
        movie.setDeleted(true);
        movieRepository.save(movie);
    }

    /**
     * Trending score formula:
     * score = 0.7 * views + 0.3 * (recentWatchCount * 30)
     * where recentWatchCount counts watch activities in the last 7 days.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "trendingMovies", key = "#limit")
    public List<TrendingMovieResponse> getTrendingMovies(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);

        List<MovieRecentWatchProjection> recentWatchCounts = movieRepository.findRecentWatchCounts(cutoff);
        Map<Long, Long> recentCountByMovieId = recentWatchCounts.stream()
                .collect(Collectors.toMap(MovieRecentWatchProjection::getMovieId,
                        projection -> projection.getRecentWatchCount() == null ? 0L : projection.getRecentWatchCount()));

        List<Long> movieIds = recentCountByMovieId.keySet().stream().toList();
        List<Movie> movies = movieIds.isEmpty() ? movieRepository.findAllByDeletedFalse() : movieRepository.findAllByIdInAndDeletedFalse(movieIds);
        Map<Long, Movie> movieById = movies.stream().collect(Collectors.toMap(Movie::getId, Function.identity()));

        return movieById.values().stream()
                .map(movie -> {
                    long recentWatchCount = recentCountByMovieId.getOrDefault(movie.getId(), 0L);
                    double score = movie.getViews() * 0.7 + (recentWatchCount * 30D) * 0.3;
                    return TrendingMovieResponse.builder()
                            .movie(movieMapper.toResponse(movie))
                            .recentWatchCount(recentWatchCount)
                            .trendingScore(score)
                            .build();
                })
                .sorted(Comparator.comparingDouble(TrendingMovieResponse::getTrendingScore).reversed())
                .limit(safeLimit)
                .toList();
    }

    @Transactional(readOnly = true)
    public VideoPlaybackResponse getVideoPlayback(Long movieId) {
        Movie movie = findMovieEntityById(movieId);
        return VideoPlaybackResponse.builder()
                .movieId(movieId)
                .videoUrl(movie.getVideoUrl())
                .rangeRequestSupported(true)
                .rangeSupportGuide("Client sends Range header (bytes=start-end), server returns 206 Partial Content and Content-Range for seek/buffering.")
                .build();
    }

    @Transactional
    @CacheEvict(value = {"movies", "movieById", "trendingMovies"}, allEntries = true)
    public void incrementViews(Long movieId) {
        Movie movie = findMovieEntityById(movieId);
        movie.setViews(movie.getViews() + 1);
        movieRepository.save(movie);
    }

    @Transactional
    @CacheEvict(value = {"movies", "movieById", "trendingMovies"}, allEntries = true)
    public void refreshRatingStats(Long movieId) {
        Movie movie = findMovieEntityById(movieId);
        MovieRatingAggregateProjection aggregate = reviewRepository.aggregateRatingByMovieId(movieId);
        long ratingCount = aggregate != null && aggregate.getRatingCount() != null ? aggregate.getRatingCount() : 0L;
        double ratingAvg = aggregate != null && aggregate.getRatingAvg() != null ? aggregate.getRatingAvg() : 0D;
        movie.setRatingCount(ratingCount);
        movie.setRatingAvg(ratingAvg);
        movieRepository.save(movie);
    }

    // Internal helper — used by WatchHistoryService and ReviewService
    @Transactional(readOnly = true)
    public Movie findMovieEntityById(Long id) {
        return movieRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new MovieNotFoundException("Không tìm thấy phim với ID: " + id));
    }

    private PageResponse<MovieResponse> toPageResponse(Page<MovieResponse> page, int requestPage, int requestSize) {
        return PageResponse.<MovieResponse>builder()
                .content(page.getContent())
                .page(Math.max(requestPage, 1))
                .size(requestSize)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    private void validateReleaseYear(int releaseYear) {
        int currentYear = LocalDate.now().getYear();
        if (releaseYear > currentYear + 1) {
            throw new IllegalArgumentException("Năm phát hành không hợp lệ");
        }
    }
}
