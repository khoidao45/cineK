package com.codek.movieauthservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "movies",
        indexes = {
                @Index(name = "idx_movies_genre", columnList = "genre"),
                @Index(name = "idx_movies_title", columnList = "title")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 100)
    private String genre;

    private int duration; // minutes

    private int releaseYear;

    @Column(length = 500)
    private String posterUrl;

    @Column(length = 500)
    private String thumbnailUrl;

    @Column(length = 1000)
    private String videoUrl;

    @Column(nullable = false)
    @Builder.Default
    private long views = 0L;

    @Column(nullable = false)
    @Builder.Default
    private double ratingAvg = 0D;

    @Column(nullable = false)
    @Builder.Default
    private long ratingCount = 0L;

        @Column(nullable = false)
        @Builder.Default
        private boolean deleted = false;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
