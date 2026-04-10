package com.codek.movieauthservice.repository;

import com.codek.movieauthservice.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByMovieIdOrderByCreatedAtDesc(Long movieId, Pageable pageable);
    boolean existsByUserIdAndMovieId(Long userId, Long movieId);
    Optional<Review> findByUserIdAndMovieId(Long userId, Long movieId);
}
