package com.codek.movieauthservice.service;

import com.codek.movieauthservice.dto.WatchHistoryRequest;
import com.codek.movieauthservice.dto.WatchHistoryResponse;
import com.codek.movieauthservice.entity.Movie;
import com.codek.movieauthservice.entity.User;
import com.codek.movieauthservice.entity.WatchHistory;
import com.codek.movieauthservice.mapper.WatchHistoryMapper;
import com.codek.movieauthservice.repository.WatchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WatchHistoryService {

    private final WatchHistoryRepository watchHistoryRepository;
    private final WatchHistoryMapper watchHistoryMapper;
    private final UserService userService;
    private final MovieService movieService;

    @Transactional
    public WatchHistoryResponse upsertProgress(Long userId, WatchHistoryRequest request) {
        WatchHistory watchHistory = watchHistoryRepository
                .findByUserIdAndMovieId(userId, request.getMovieId())
                .orElse(null);

        if (watchHistory != null) {
            watchHistory.setProgress(request.getProgress());
        } else {
            User user = userService.getUserById(userId);
            Movie movie = movieService.findMovieEntityById(request.getMovieId());
            watchHistory = WatchHistory.builder()
                    .user(user)
                    .movie(movie)
                    .progress(request.getProgress())
                    .build();
        }

        WatchHistory saved = watchHistoryRepository.save(watchHistory);
        return watchHistoryMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<WatchHistoryResponse> getContinueWatching(Long userId, Pageable pageable) {
        return watchHistoryRepository
                .findByUserIdOrderByLastWatchedAtDesc(userId, pageable)
                .map(watchHistoryMapper::toResponse);
    }
}
