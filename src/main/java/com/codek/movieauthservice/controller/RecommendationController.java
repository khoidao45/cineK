package com.codek.movieauthservice.controller;

import com.codek.movieauthservice.dto.RecommendationResponse;
import com.codek.movieauthservice.security.CustomUserDetailsService;
import com.codek.movieauthservice.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<RecommendationResponse>> getRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isAdmin && !userDetails.getUserId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền xem gợi ý của người dùng khác");
        }

        return ResponseEntity.ok(recommendationService.getRecommendations(userId, limit));
    }
}
