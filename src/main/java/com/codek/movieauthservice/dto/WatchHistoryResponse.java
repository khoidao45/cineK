package com.codek.movieauthservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchHistoryResponse {
    private Long id;
    private Long userId;
    private MovieResponse movie;
    private int progress;
    private LocalDateTime lastWatchedAt;
}
