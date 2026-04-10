import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../lib/api";
import type { MovieResponse, PageResponse, TrendingMovieResponse } from "../types/api";
import { useErrorMessage } from "../hooks/useErrorMessage";
import { ErrorNotice } from "../components/ErrorNotice";

export function HomePage() {
  const [movies, setMovies] = useState<PageResponse<MovieResponse> | null>(null);
  const [trending, setTrending] = useState<TrendingMovieResponse[]>([]);
  const [keyword, setKeyword] = useState("");
  const [genre, setGenre] = useState("");
  const [page, setPage] = useState(1);
  const [error, setError] = useState<string | null>(null);
  const { parseError } = useErrorMessage();

  const query = useMemo(() => ({ page, size: 8, keyword, genre }), [page, keyword, genre]);

  useEffect(() => {
    void api.get<TrendingMovieResponse[]>("/api/movies/trending", { params: { limit: 6 } })
      .then((res) => setTrending(res.data))
      .catch((e) => setError(parseError(e)));
  }, [parseError]);

  useEffect(() => {
    void api.get<PageResponse<MovieResponse>>("/api/movies", { params: query })
      .then((res) => setMovies(res.data))
      .catch((e) => setError(parseError(e)));
  }, [query, parseError]);

  return (
    <section>
      <h1 className="hero-title">Your Cinema, Personally Curated</h1>
      <p className="hero-sub">Browse trending, discover by genre, and continue exactly where you left off.</p>

      <div className="card grid-3">
        <input className="input" placeholder="Search by title..." value={keyword} onChange={(e) => setKeyword(e.target.value)} />
        <input className="input" placeholder="Filter by genre..." value={genre} onChange={(e) => setGenre(e.target.value)} />
        <button className="btn" onClick={() => setPage(1)}>Apply Filter</button>
      </div>

      <ErrorNotice message={error} />

      <h2 className="section-title">Trending Now</h2>
      <div className="grid cards">
        {trending.map((item) => (
          <Link className="movie-card" key={item.movie.id} to={`/movies/${item.movie.id}`}>
            <strong>{item.movie.title}</strong>
            <span>{item.movie.genre} · {item.movie.releaseYear}</span>
            <span>Score {item.trendingScore.toFixed(1)}</span>
          </Link>
        ))}
      </div>

      <h2 className="section-title">All Movies</h2>
      <div className="grid cards">
        {movies?.content.map((movie) => (
          <Link className="movie-card" key={movie.id} to={`/movies/${movie.id}`}>
            <strong>{movie.title}</strong>
            <span>{movie.genre} · {movie.duration}m</span>
            <span>Rating {movie.ratingAvg?.toFixed?.(1) ?? "0.0"}</span>
          </Link>
        ))}
      </div>

      {movies && (
        <div className="pager">
          <button className="btn ghost" disabled={page <= 1} onClick={() => setPage((p) => p - 1)}>Prev</button>
          <span>Page {movies.page} / {movies.totalPages || 1}</span>
          <button className="btn ghost" disabled={page >= (movies.totalPages || 1)} onClick={() => setPage((p) => p + 1)}>Next</button>
        </div>
      )}
    </section>
  );
}
