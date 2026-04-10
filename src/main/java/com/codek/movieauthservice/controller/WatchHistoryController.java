package com.codek.movieauthservice.controller;

import com.codek.movieauthservice.dto.WatchHistoryRequest;
import com.codek.movieauthservice.dto.WatchHistoryResponse;
import com.codek.movieauthservice.security.CustomUserDetailsService;
import com.codek.movieauthservice.service.WatchHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class WatchHistoryController {

    private final WatchHistoryService watchHistoryService;

    @PostMapping
    public ResponseEntity<WatchHistoryResponse> upsertProgress(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
            @Valid @RequestBody WatchHistoryRequest request) {
        return ResponseEntity.ok(watchHistoryService.upsertProgress(userDetails.getUserId(), request));
    }

    @GetMapping("/continue")
    public ResponseEntity<Page<WatchHistoryResponse>> getContinueWatching(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(watchHistoryService.getContinueWatching(userDetails.getUserId(), PageRequest.of(page, size)));
    }
}
