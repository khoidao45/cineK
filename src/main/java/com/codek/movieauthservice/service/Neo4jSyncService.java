package com.codek.movieauthservice.service;

import com.codek.movieauthservice.entity.Movie;
import com.codek.movieauthservice.entity.Review;
import com.codek.movieauthservice.entity.User;
import com.codek.movieauthservice.entity.WatchHistory;
import com.codek.movieauthservice.entity.neo4j.MovieNode;
import com.codek.movieauthservice.entity.neo4j.RatedRelationship;
import com.codek.movieauthservice.entity.neo4j.UserNode;
import com.codek.movieauthservice.entity.neo4j.WatchedRelationship;
import com.codek.movieauthservice.repository.MovieRepository;
import com.codek.movieauthservice.repository.ReviewRepository;
import com.codek.movieauthservice.repository.UserRepository;
import com.codek.movieauthservice.repository.WatchHistoryRepository;
import com.codek.movieauthservice.repository.neo4j.MovieNodeRepository;
import com.codek.movieauthservice.repository.neo4j.UserNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * On application startup, syncs existing PostgreSQL data into the Neo4j graph.
 * Uses MERGE semantics (idempotent) — safe to run multiple times.
 *
 * After the initial sync, ongoing updates happen via dual-write in the service layer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Neo4jSyncService {

    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final WatchHistoryRepository watchHistoryRepository;
    private final ReviewRepository reviewRepository;
    private final UserNodeRepository userNodeRepository;
    private final MovieNodeRepository movieNodeRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        log.info("neo4j.sync.start — syncing PostgreSQL data into Neo4j graph");
        try {
            syncMovies();
            syncUsers();
            syncWatchHistory();
            syncReviews();
            log.info("neo4j.sync.complete");
        } catch (Exception e) {
            log.error("neo4j.sync.failed — recommendations will use cold-start until next restart", e);
        }
    }

    @Transactional(readOnly = true)
    public void syncMovies() {
        List<Movie> movies = movieRepository.findAll();
        for (Movie m : movies) {
            movieNodeRepository.save(toMovieNode(m));
        }
        log.info("neo4j.sync.movies count={}", movies.size());
    }

    @Transactional(readOnly = true)
    public void syncUsers() {
        List<User> users = userRepository.findAll();
        for (User u : users) {
            userNodeRepository.save(UserNode.builder()
                    .id(u.getId())
                    .username(u.getUsername())
                    .build());
        }
        log.info("neo4j.sync.users count={}", users.size());
    }

    @Transactional(readOnly = true)
    public void syncWatchHistory() {
        int page = 0;
        int batchSize = 500;
        long total = 0;
        List<WatchHistory> batch;
        do {
            batch = watchHistoryRepository.findAll(PageRequest.of(page++, batchSize)).getContent();
            for (WatchHistory wh : batch) {
                mergeWatchedRelationship(
                        wh.getUser().getId(),
                        wh.getMovie().getId(),
                        wh.getProgress(),
                    wh.getLastWatchedAt()
                );
            }
            total += batch.size();
        } while (batch.size() == batchSize);
        log.info("neo4j.sync.watchHistory count={}", total);
    }

    @Transactional(readOnly = true)
    public void syncReviews() {
        int page = 0;
        int batchSize = 500;
        long total = 0;
        List<Review> batch;
        do {
            batch = reviewRepository.findAll(PageRequest.of(page++, batchSize)).getContent();
            for (Review r : batch) {
                mergeRatedRelationship(
                        r.getUser().getId(),
                        r.getMovie().getId(),
                        r.getRating()
                );
            }
            total += batch.size();
        } while (batch.size() == batchSize);
        log.info("neo4j.sync.reviews count={}", total);
    }

    // ── Public merge helpers — called by dual-write in service layer ─────────

    public void mergeMovieNode(Movie movie) {
        try {
            movieNodeRepository.save(toMovieNode(movie));
        } catch (Exception e) {
            log.warn("neo4j.mergeMovie.failed movieId={}: {}", movie.getId(), e.getMessage());
        }
    }

    public void mergeUserNode(User user) {
        try {
            userNodeRepository.save(UserNode.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .build());
        } catch (Exception e) {
            log.warn("neo4j.mergeUser.failed userId={}: {}", user.getId(), e.getMessage());
        }
    }

    public void mergeWatchedRelationship(Long userId, Long movieId, int progress, java.time.LocalDateTime lastWatchedAt) {
        try {
            UserNode user = userNodeRepository.findById(userId)
                    .orElseGet(() -> userNodeRepository.save(UserNode.builder().id(userId).username("").build()));
            MovieNode movie = movieNodeRepository.findById(movieId)
                    .orElseGet(() -> movieNodeRepository.save(MovieNode.builder().id(movieId).build()));

            // Remove existing WATCHED relationship for this user→movie pair, then re-add updated one
            user.getWatched().removeIf(w -> w.getMovie() != null && movieId.equals(w.getMovie().getId()));
            user.getWatched().add(WatchedRelationship.builder()
                    .movie(movie)
                    .progress(progress)
                    .lastWatchedAt(lastWatchedAt == null ? java.time.LocalDateTime.now() : lastWatchedAt)
                    .build());
            userNodeRepository.save(user);
        } catch (Exception e) {
            log.warn("neo4j.mergeWatched.failed userId={} movieId={}: {}", userId, movieId, e.getMessage());
        }
    }

    public void mergeRatedRelationship(Long userId, Long movieId, int rating) {
        try {
            UserNode user = userNodeRepository.findById(userId)
                    .orElseGet(() -> userNodeRepository.save(UserNode.builder().id(userId).username("").build()));
            MovieNode movie = movieNodeRepository.findById(movieId)
                    .orElseGet(() -> movieNodeRepository.save(MovieNode.builder().id(movieId).build()));

            user.getRated().removeIf(r -> r.getMovie() != null && movieId.equals(r.getMovie().getId()));
            user.getRated().add(RatedRelationship.builder()
                    .movie(movie)
                    .rating(rating)
                    .createdAt(java.time.LocalDateTime.now())
                    .build());
            userNodeRepository.save(user);
        } catch (Exception e) {
            log.warn("neo4j.mergeRated.failed userId={} movieId={}: {}", userId, movieId, e.getMessage());
        }
    }

    public void markMovieDeleted(Long movieId) {
        try {
            movieNodeRepository.findById(movieId).ifPresent(node -> {
                node.setDeleted(true);
                movieNodeRepository.save(node);
            });
        } catch (Exception e) {
            log.warn("neo4j.markDeleted.failed movieId={}: {}", movieId, e.getMessage());
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private MovieNode toMovieNode(Movie m) {
        return MovieNode.builder()
                .id(m.getId())
                .title(m.getTitle())
                .genre(m.getGenre())
                .director(m.getDirector())
                .actors(m.getActors())
                .releaseYear(m.getReleaseYear())
                .deleted(m.isDeleted())
                .build();
    }
}
