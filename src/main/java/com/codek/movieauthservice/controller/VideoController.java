package com.codek.movieauthservice.controller;

import com.codek.movieauthservice.security.CustomUserDetailsService;
import com.codek.movieauthservice.service.VideoStreamingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class VideoController {

    private final VideoStreamingService videoStreamingService;

    @GetMapping("/{movieId}")
    public ResponseEntity<StreamingResponseBody> getVideoPlayback(
            @PathVariable Long movieId,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        return videoStreamingService.streamVideo(movieId, rangeHeader);
    }

    @GetMapping("/stream")
    public ResponseEntity<StreamingResponseBody> streamByUrl(
            @RequestParam String videoUrl,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        return videoStreamingService.streamVideoUrl(videoUrl, rangeHeader);
    }
}
