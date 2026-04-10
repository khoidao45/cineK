package com.codek.movieauthservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoPlaybackResponse {
    private Long movieId;
    private String videoUrl;
    private boolean rangeRequestSupported;
    private String rangeSupportGuide;
}
