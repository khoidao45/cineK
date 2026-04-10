package com.codek.movieauthservice.service;

import com.codek.movieauthservice.dto.ReviewRequest;
import com.codek.movieauthservice.dto.ReviewResponse;
import com.codek.movieauthservice.entity.Movie;
import com.codek.movieauthservice.entity.Review;
import com.codek.movieauthservice.entity.User;
import com.codek.movieauthservice.exception.DuplicateReviewException;
import com.codek.movieauthservice.mapper.ReviewMapper;
import com.codek.movieauthservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final UserService userService;
    private final MovieService movieService;

    @Transactional
    public ReviewResponse addReview(Long userId, ReviewRequest request) {
        if (reviewRepository.existsByUserIdAndMovieId(userId, request.getMovieId())) {
            throw new DuplicateReviewException("Bạn đã đánh giá phim này rồi!");
        }

        User user = userService.getUserById(userId);
        Movie movie = movieService.findMovieEntityById(request.getMovieId());

        Review review = Review.builder()
                .user(user)
                .movie(movie)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        Review saved = reviewRepository.save(review);
        movieService.refreshRatingStats(request.getMovieId());
        return reviewMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMovieReviews(Long movieId, Pageable pageable) {
        movieService.findMovieEntityById(movieId); // verify movie exists
        return reviewRepository.findByMovieIdOrderByCreatedAtDesc(movieId, pageable)
                .map(reviewMapper::toResponse);
    }
}
