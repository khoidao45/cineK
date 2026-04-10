package com.codek.movieauthservice.repository;

import com.codek.movieauthservice.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByMovieIdOrderByCreatedAtDesc(Long movieId, Pageable pageable);
    boolean existsByUserIdAndMovieId(Long userId, Long movieId);
    Optional<Review> findByUserIdAndMovieId(Long userId, Long movieId);

    @Query("""
            SELECT COUNT(r.id) as ratingCount, COALESCE(AVG(r.rating), 0) as ratingAvg
            FROM Review r
            WHERE r.movie.id = :movieId
            """)
    MovieRatingAggregateProjection aggregateRatingByMovieId(@Param("movieId") Long movieId);
}
