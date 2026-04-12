export type Role = "ADMIN" | "USER" | "MODERATOR" | string;

export interface ErrorResponse {
  status: number;
  message: string;
  error: string;
  timestamp: string;
  path: string;
}

export interface AuthResponse {
  token?: string;
  refreshToken?: string;
  type?: string;
  userId?: number;
  username?: string;
  email?: string;
  role?: Role;
  message?: string;
}

export interface MovieResponse {
  id: number;
  title: string;
  description?: string;
  genre: string;
  duration: number;
  releaseYear: number;
  posterUrl?: string;
  thumbnailUrl?: string;
  videoUrl?: string;
  director?: string;
  actors?: string;
  views: number;
  ratingAvg: number;
  ratingCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface TrendingMovieResponse {
  movie: MovieResponse;
  trendingScore: number;
  recentWatchCount: number;
}

export interface ReviewResponse {
  id: number;
  movieId: number;
  userId: number;
  username: string;
  rating: number;
  comment?: string;
  createdAt: string;
}

export interface WatchHistoryResponse {
  id: number;
  movieId: number;
  movieTitle: string;
  progress: number;
  lastWatchedAt: string;
}

export interface RecommendationResponse {
  movie: MovieResponse;
  score: number;
  similarityScore: number;
  genreScore: number;
  trendingScore: number;
  ratingBoost: number;
}

export interface UserResponse {
  id: number;
  username: string;
  email: string;
  fullName: string;
  avatarUrl?: string;
  role: Role;
  active: boolean;
}
