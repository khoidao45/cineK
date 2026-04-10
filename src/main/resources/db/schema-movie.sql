CREATE TABLE IF NOT EXISTS movies (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    genre VARCHAR(100) NOT NULL,
    duration INTEGER NOT NULL CHECK (duration > 0),
    release_year INTEGER NOT NULL CHECK (release_year >= 1888 AND release_year <= 2100),
    poster_url VARCHAR(500),
    thumbnail_url VARCHAR(500),
    video_url VARCHAR(1000),
    views BIGINT NOT NULL DEFAULT 0 CHECK (views >= 0),
    rating_avg DOUBLE PRECISION NOT NULL DEFAULT 0,
    rating_count BIGINT NOT NULL DEFAULT 0 CHECK (rating_count >= 0),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_movies_title ON movies(title);
CREATE INDEX IF NOT EXISTS idx_movies_genre ON movies(genre);
