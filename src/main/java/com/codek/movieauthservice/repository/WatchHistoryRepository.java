package com.codek.movieauthservice.repository;

import com.codek.movieauthservice.entity.WatchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {
    Page<WatchHistory> findByUserIdOrderByLastWatchedAtDesc(Long userId, Pageable pageable);
    Optional<WatchHistory> findByUserIdAndMovieId(Long userId, Long movieId);

    @Query("""
        SELECT w.movie.id
        FROM WatchHistory w
        WHERE w.user.id = :userId
        """)
    List<Long> findWatchedMovieIdsByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT w.user.id as userId, COUNT(DISTINCT w.movie.id) as overlapCount
        FROM WatchHistory w
        WHERE w.user.id <> :userId
          AND w.movie.id IN (
        SELECT w2.movie.id
        FROM WatchHistory w2
        WHERE w2.user.id = :userId
          )
        GROUP BY w.user.id
        ORDER BY COUNT(DISTINCT w.movie.id) DESC
        """)
    List<SimilarUserProjection> findSimilarUsersByWatchOverlap(@Param("userId") Long userId, Pageable pageable);

    @Query("""
        SELECT w
        FROM WatchHistory w
        JOIN FETCH w.movie
        WHERE w.user.id IN :userIds
        """)
    List<WatchHistory> findByUserIdsWithMovie(@Param("userIds") List<Long> userIds);

    @Query("""
        SELECT w.movie.id as movieId, COUNT(w.id) as recentWatchCount
        FROM WatchHistory w
        WHERE w.movie.id IN :movieIds
          AND w.lastWatchedAt >= :cutoff
        GROUP BY w.movie.id
        """)
    List<MovieRecentWatchProjection> findRecentWatchCountsForMovieIds(@Param("movieIds") List<Long> movieIds,
                                       @Param("cutoff") LocalDateTime cutoff);
}
