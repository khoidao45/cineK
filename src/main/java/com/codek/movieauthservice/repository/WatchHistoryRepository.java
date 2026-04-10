package com.codek.movieauthservice.repository;

import com.codek.movieauthservice.entity.WatchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {
    Page<WatchHistory> findByUserIdOrderByLastWatchedAtDesc(Long userId, Pageable pageable);
    Optional<WatchHistory> findByUserIdAndMovieId(Long userId, Long movieId);
}
