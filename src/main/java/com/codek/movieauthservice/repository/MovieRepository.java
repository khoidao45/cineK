package com.codek.movieauthservice.repository;

import com.codek.movieauthservice.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
        Page<Movie> findByTitleContainingIgnoreCaseAndDeletedFalse(String title, Pageable pageable);
        Page<Movie> findByGenreIgnoreCaseAndDeletedFalse(String genre, Pageable pageable);
        Optional<Movie> findByIdAndDeletedFalse(Long id);
        boolean existsByIdAndDeletedFalse(Long id);
        List<Movie> findAllByDeletedFalse();
        List<Movie> findAllByIdInAndDeletedFalse(List<Long> ids);

        @Query("""
                        SELECT m
                        FROM Movie m
                        WHERE m.deleted = false
                            AND (:keyword IS NULL OR :keyword = '' OR LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
                            AND (:genre IS NULL OR :genre = '' OR LOWER(m.genre) = LOWER(:genre))
                        """)
        Page<Movie> searchByKeywordAndGenre(@Param("keyword") String keyword,
                                                                                @Param("genre") String genre,
                                                                                Pageable pageable);

        @Query("""
                        SELECT w.movie.id as movieId, COUNT(w.id) as recentWatchCount
                        FROM WatchHistory w
                        JOIN w.movie m
                        WHERE w.lastWatchedAt >= :cutoff
                            AND m.deleted = false
                        GROUP BY w.movie.id
                        """)
        List<MovieRecentWatchProjection> findRecentWatchCounts(@Param("cutoff") LocalDateTime cutoff);
}
