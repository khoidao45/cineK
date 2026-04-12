package com.codek.movieauthservice.service;

import com.codek.movieauthservice.dto.MovieResponse;
import com.codek.movieauthservice.dto.RecommendationResponse;
import com.codek.movieauthservice.entity.Movie;
import com.codek.movieauthservice.entity.WatchHistory;
import com.codek.movieauthservice.mapper.MovieMapper;
import com.codek.movieauthservice.repository.MovieRecentWatchProjection;
import com.codek.movieauthservice.repository.MovieRepository;
import com.codek.movieauthservice.repository.SimilarUserProjection;
import com.codek.movieauthservice.repository.WatchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final WatchHistoryRepository watchHistoryRepository;
    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendations(Long userId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);

        List<Long> watchedMovieIds = watchHistoryRepository.findWatchedMovieIdsByUserId(userId);
        Set<Long> watchedMovieSet = new HashSet<>(watchedMovieIds);

        List<SimilarUserProjection> similarUsers = watchHistoryRepository
                .findSimilarUsersByWatchOverlap(userId, PageRequest.of(0, 30));

        if (similarUsers.isEmpty()) {
            return buildColdStartRecommendations(watchedMovieIds, safeLimit);
        }

        Map<Long, Long> overlapByUserId = similarUsers.stream()
                .collect(Collectors.toMap(SimilarUserProjection::getUserId,
                        s -> s.getOverlapCount() == null ? 0L : s.getOverlapCount()));

        List<Long> similarUserIds = new ArrayList<>(overlapByUserId.keySet());
        List<WatchHistory> similarWatchHistories = watchHistoryRepository.findByUserIdsWithMovie(similarUserIds);

        Map<Long, Double> weightedSimilarityByMovieId = new HashMap<>();
        for (WatchHistory watchHistory : similarWatchHistories) {
            Long movieId = watchHistory.getMovie().getId();
            if (watchedMovieSet.contains(movieId)) {
                continue;
            }
            double weight = overlapByUserId.getOrDefault(watchHistory.getUser().getId(), 0L);
            weightedSimilarityByMovieId.merge(movieId, weight, Double::sum);
        }

        if (weightedSimilarityByMovieId.isEmpty()) {
            return buildColdStartRecommendations(watchedMovieIds, safeLimit);
        }

        List<Long> candidateMovieIds = new ArrayList<>(weightedSimilarityByMovieId.keySet());
        List<Movie> candidateMovies = movieRepository.findAllByIdInAndDeletedFalse(candidateMovieIds);
        Map<Long, Movie> movieById = candidateMovies.stream().collect(Collectors.toMap(Movie::getId, m -> m));

        Set<String> preferredGenres = derivePreferredGenres(watchedMovieIds);
        Set<String> preferredActors = derivePreferredActors(watchedMovieIds);
        Set<String> preferredDirectors = derivePreferredDirectors(watchedMovieIds);
        Map<Long, Long> recentCountByMovieId = watchHistoryRepository
                .findRecentWatchCountsForMovieIds(candidateMovieIds, LocalDateTime.now().minusDays(7))
                .stream()
                .collect(Collectors.toMap(MovieRecentWatchProjection::getMovieId,
                        p -> p.getRecentWatchCount() == null ? 0L : p.getRecentWatchCount()));

        double maxSimilarity = weightedSimilarityByMovieId.values().stream().mapToDouble(Double::doubleValue).max().orElse(1D);
        double maxViews = candidateMovies.stream().mapToDouble(Movie::getViews).max().orElse(1D);
        double maxRecent = recentCountByMovieId.values().stream().mapToDouble(Long::doubleValue).max().orElse(1D);

        return candidateMovieIds.stream()
                .map(movieId -> {
                    Movie movie = movieById.get(movieId);
                    if (movie == null) {
                        return null;
                    }

                    double similarityScore = weightedSimilarityByMovieId.getOrDefault(movieId, 0D) / Math.max(maxSimilarity, 1D);
                    double genreScore = preferredGenres.contains(normalizeGenre(movie.getGenre())) ? 1D : 0D;
                    double actorScore = computeActorScore(movie.getActors(), preferredActors);
                    double directorScore = preferredDirectors.contains(normalizeName(movie.getDirector())) ? 1D : 0D;
                    double contentScore = (actorScore * 0.6) + (directorScore * 0.4);
                    double viewsNormalized = movie.getViews() / Math.max(maxViews, 1D);
                    double recentNormalized = recentCountByMovieId.getOrDefault(movieId, 0L) / Math.max(maxRecent, 1D);
                    double trendingScore = viewsNormalized * 0.7 + recentNormalized * 0.3;
                    double ratingBoost = Math.min(movie.getRatingAvg() / 5D, 1D) * 0.1;

                    double totalScore = (similarityScore * 0.4) + (genreScore * 0.25) + (contentScore * 0.25) + (trendingScore * 0.1) + ratingBoost;
                    MovieResponse movieResponse = movieMapper.toResponse(movie);

                    return RecommendationResponse.builder()
                            .movie(movieResponse)
                            .score(totalScore)
                            .similarityScore(similarityScore)
                            .genreScore(genreScore)
                            .trendingScore(trendingScore)
                            .ratingBoost(ratingBoost)
                            .build();
                })
                .filter(r -> r != null)
                .sorted(Comparator.comparingDouble(RecommendationResponse::getScore).reversed())
                .limit(safeLimit)
                .toList();
    }

    private List<RecommendationResponse> buildColdStartRecommendations(List<Long> watchedMovieIds, int limit) {
        Set<Long> watchedSet = new HashSet<>(watchedMovieIds);
        Set<String> preferredGenres = derivePreferredGenres(watchedMovieIds);

        return movieRepository.findAllByDeletedFalse().stream()
                .filter(movie -> !watchedSet.contains(movie.getId()))
                .map(movie -> {
                    double genreScore = preferredGenres.contains(normalizeGenre(movie.getGenre())) ? 1D : 0D;
                    double trendingScore = Math.min(movie.getViews() / 1000D, 1D);
                    double ratingBoost = Math.min(movie.getRatingAvg() / 5D, 1D) * 0.1;
                    double totalScore = (genreScore * 0.6) + (trendingScore * 0.4) + ratingBoost;

                    return RecommendationResponse.builder()
                            .movie(movieMapper.toResponse(movie))
                            .score(totalScore)
                            .similarityScore(0D)
                            .genreScore(genreScore)
                            .trendingScore(trendingScore)
                            .ratingBoost(ratingBoost)
                            .build();
                })
                .sorted(Comparator.comparingDouble(RecommendationResponse::getScore).reversed())
                .limit(limit)
                .toList();
    }

    private Set<String> derivePreferredGenres(List<Long> watchedMovieIds) {
        if (watchedMovieIds == null || watchedMovieIds.isEmpty()) {
            return Set.of();
        }

        return movieRepository.findAllByIdInAndDeletedFalse(watchedMovieIds).stream()
                .map(Movie::getGenre)
                .filter(g -> g != null && !g.isBlank())
                .map(this::normalizeGenre)
                .collect(Collectors.groupingBy(g -> g, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private String normalizeGenre(String genre) {
        return genre == null ? "" : genre.trim().toLowerCase();
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }

    private Set<String> derivePreferredActors(List<Long> watchedMovieIds) {
        if (watchedMovieIds == null || watchedMovieIds.isEmpty()) {
            return Set.of();
        }
        return movieRepository.findAllByIdInAndDeletedFalse(watchedMovieIds).stream()
                .filter(m -> m.getActors() != null && !m.getActors().isBlank())
                .flatMap(m -> java.util.Arrays.stream(m.getActors().split(",")))
                .map(a -> a.trim().toLowerCase())
                .filter(a -> !a.isEmpty())
                .collect(Collectors.groupingBy(a -> a, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private Set<String> derivePreferredDirectors(List<Long> watchedMovieIds) {
        if (watchedMovieIds == null || watchedMovieIds.isEmpty()) {
            return Set.of();
        }
        return movieRepository.findAllByIdInAndDeletedFalse(watchedMovieIds).stream()
                .filter(m -> m.getDirector() != null && !m.getDirector().isBlank())
                .map(m -> m.getDirector().trim().toLowerCase())
                .collect(Collectors.groupingBy(d -> d, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private double computeActorScore(String actorsField, Set<String> preferredActors) {
        if (actorsField == null || actorsField.isBlank() || preferredActors.isEmpty()) {
            return 0D;
        }
        long matches = java.util.Arrays.stream(actorsField.split(","))
                .map(a -> a.trim().toLowerCase())
                .filter(preferredActors::contains)
                .count();
        return Math.min(matches / 2D, 1D);
    }
}
