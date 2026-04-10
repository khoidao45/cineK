package com.codek.movieauthservice.mapper;

import com.codek.movieauthservice.dto.ReviewResponse;
import com.codek.movieauthservice.entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUser().getId())
                .username(review.getUser().getUsername())
                .movieId(review.getMovie().getId())
                .movieTitle(review.getMovie().getTitle())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
