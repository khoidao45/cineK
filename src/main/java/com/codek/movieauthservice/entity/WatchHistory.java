package com.codek.movieauthservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "watch_history",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_watch_history_user_movie",
                columnNames = {"user_id", "movie_id"}
        ),
        indexes = @Index(name = "idx_watch_history_user", columnList = "user_id")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(nullable = false)
    private int progress; // 0-100

    private LocalDateTime lastWatchedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.lastWatchedAt = LocalDateTime.now();
    }
}
