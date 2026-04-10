package com.codek.movieauthservice.mapper;

import com.codek.movieauthservice.dto.WatchHistoryResponse;
import com.codek.movieauthservice.entity.WatchHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WatchHistoryMapper {

    private final MovieMapper movieMapper;

    public WatchHistoryResponse toResponse(WatchHistory watchHistory) {
        return WatchHistoryResponse.builder()
                .id(watchHistory.getId())
                .userId(watchHistory.getUser().getId())
                .movie(movieMapper.toResponse(watchHistory.getMovie()))
                .progress(watchHistory.getProgress())
                .lastWatchedAt(watchHistory.getLastWatchedAt())
                .build();
    }
}
