package com.codek.movieauthservice.service;

import com.codek.movieauthservice.config.AppProperties;
import com.codek.movieauthservice.dto.MovieResponse;
import com.codek.movieauthservice.dto.RecommendationResponse;
import com.codek.movieauthservice.entity.Movie;
import com.codek.movieauthservice.entity.neo4j.MovieNode;
import com.codek.movieauthservice.mapper.MovieMapper;
import com.codek.movieauthservice.repository.MovieRepository;
import com.codek.movieauthservice.repository.WatchHistoryRepository;
import com.codek.movieauthservice.repository.neo4j.MovieNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final WatchHistoryRepository watchHistoryRepository;
    private final MovieRepository movieRepository;
    private final MovieNodeRepository movieNodeRepository;
    private final MovieMapper movieMapper;
    private final AppProperties appProperties;

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendations(Long userId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), appProperties.recommendationLimit());

        List<MovieNode> collaborative = movieNodeRepository
                .findCollaborativeRecommendations(userId, safeLimit * 2);
        List<MovieNode> contentBased = movieNodeRepository
                .findContentBasedRecommendations(userId, safeLimit * 2);
        List<MovieNode> trending = movieNodeRepository
                .findTrendingMovieNodes(safeLimit * 2);
        List<MovieNode> coldStart = movieNodeRepository
                .findColdStartRecommendations(userId, safeLimit * 2);

        if (collaborative.isEmpty() && contentBased.isEmpty() && trending.isEmpty() && coldStart.isEmpty()) {
            return buildColdStartRecommendations(watchHistoryRepository.findWatchedMovieIdsByUserId(userId), safeLimit);
        }

        Map<Long, Double> similarityScores = rankToScore(collaborative, 1.0);
        Map<Long, Double> genreScores = rankToScore(contentBased, 1.0);
        Map<Long, Double> trendingScores = rankToScore(trending, 1.0);
        Map<Long, Double> baseScores = new HashMap<>();

        mergeWeighted(baseScores, collaborative, appProperties.collabWeight());
        mergeWeighted(baseScores, contentBased, appProperties.contentWeight());
        mergeWeighted(baseScores, trending, appProperties.trendingWeight());
        mergeWeighted(baseScores, coldStart, appProperties.coldStartWeight());

        List<Long> movieIds = new java.util.ArrayList<>(baseScores.keySet());
        List<Movie> movies = movieRepository.findAllByIdInAndDeletedFalse(movieIds);
        Map<Long, Movie> movieById = movies.stream().collect(Collectors.toMap(Movie::getId, Function.identity()));

        return baseScores.entrySet().stream()
                .map(entry -> toRecommendation(entry.getKey(), entry.getValue(), movieById, similarityScores, genreScores, trendingScores))
                .filter(java.util.Objects::nonNull)
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

    private Map<Long, Double> rankToScore(List<MovieNode> nodes, double maxScore) {
        Map<Long, Double> scores = new HashMap<>();
        int size = nodes.size();
        for (int i = 0; i < size; i++) {
            MovieNode node = nodes.get(i);
            if (node.getId() == null) {
                continue;
            }
            double score = maxScore * (1.0 - ((double) i / Math.max(size, 1)));
            scores.merge(node.getId(), Math.max(score, 0D), Math::max);
        }
        return scores;
    }

    private void mergeWeighted(Map<Long, Double> baseScores, List<MovieNode> rankedNodes, double weight) {
        int size = rankedNodes.size();
        for (int i = 0; i < size; i++) {
            MovieNode node = rankedNodes.get(i);
            if (node.getId() == null) {
                continue;
            }
            double rankScore = 1.0 - ((double) i / Math.max(size, 1));
            baseScores.merge(node.getId(), Math.max(rankScore, 0D) * weight, Double::sum);
        }
    }

    private RecommendationResponse toRecommendation(
            Long movieId,
            double baseScore,
            Map<Long, Movie> movieById,
            Map<Long, Double> similarityScores,
            Map<Long, Double> genreScores,
            Map<Long, Double> trendingScores
    ) {
        Movie movie = movieById.get(movieId);
        if (movie == null) {
            return null;
        }

        double similarityScore = similarityScores.getOrDefault(movieId, 0D);
        double genreScore = genreScores.getOrDefault(movieId, 0D);
        double trendingScore = trendingScores.getOrDefault(movieId, 0D);
        double ratingBoost = Math.min(movie.getRatingAvg() / 5D, 1D) * 0.1;
        double finalScore = baseScore + ratingBoost;

        MovieResponse movieResponse = movieMapper.toResponse(movie);
        return RecommendationResponse.builder()
                .movie(movieResponse)
                .score(finalScore)
                .similarityScore(similarityScore)
                .genreScore(genreScore)
                .trendingScore(trendingScore)
                .ratingBoost(ratingBoost)
                .build();
    }

}
