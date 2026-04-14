package com.codek.movieauthservice.controller;

import com.codek.movieauthservice.dto.ReviewRequest;
import com.codek.movieauthservice.dto.ReviewResponse;
import com.codek.movieauthservice.security.CustomUserDetailsService;
import com.codek.movieauthservice.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/reviews")
    public ResponseEntity<ReviewResponse> addReview(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.addReview(userDetails.getUserId(), request));
    }

    @GetMapping("/movies/{id}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getMovieReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reviewService.getMovieReviews(id, PageRequest.of(page, size)));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        reviewService.deleteReview(reviewId, userDetails.getUserId(), isAdmin);
        return ResponseEntity.noContent().build();
    }
}
