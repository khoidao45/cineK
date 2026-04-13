import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../lib/api";
import type { MovieResponse, PageResponse, TrendingMovieResponse } from "../types/api";
import { useErrorMessage } from "../hooks/useErrorMessage";
import { ErrorNotice } from "../components/ErrorNotice";

const GRADIENTS = ["pg-1","pg-2","pg-3","pg-4","pg-5","pg-6","pg-7","pg-8"];

function MovieCard({ movie, index }: { movie: MovieResponse; index: number }) {
  const pg = GRADIENTS[index % GRADIENTS.length];
  return (
    <Link className="movie-card" to={`/movies/${movie.id}`}>
      <div className="card-poster">
        {movie.posterUrl
          ? <img src={movie.posterUrl} alt={movie.title} />
          : <div className={`card-poster-placeholder ${pg}`} />}
        <div className="card-overlay">
          <div className="play-circle">
            <svg viewBox="0 0 24 24"><path d="M5 3l14 9-14 9V3z"/></svg>
          </div>
        </div>
      </div>
      <div className="card-body">
        <div className="card-title">{movie.title}</div>
        <div className="card-meta">
          <span className="card-rating">★ {movie.ratingAvg?.toFixed(1) ?? "0.0"}</span>
          <span>{movie.releaseYear}</span>
          <span className="card-genre">{movie.genre}</span>
        </div>
      </div>
    </Link>
  );
}

export function HomePage() {
  const [movies, setMovies] = useState<PageResponse<MovieResponse> | null>(null);
  const [trending, setTrending] = useState<TrendingMovieResponse[]>([]);
  const [keyword, setKeyword] = useState("");
  const [genre, setGenre] = useState("");
  const [page, setPage] = useState(1);
  const [error, setError] = useState<string | null>(null);
  const { parseError } = useErrorMessage();

  const query = useMemo(() => ({ page, size: 12, keyword, genre }), [page, keyword, genre]);

  useEffect(() => {
    void api.get<TrendingMovieResponse[]>("/api/movies/trending", { params: { limit: 8 } })
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
      <h1 className="hero-title">Your Cinema, <span style={{color:"var(--accent)"}}>Curated</span></h1>
      <p className="hero-sub">Browse trending, discover by genre, and continue where you left off.</p>

      <div className="card" style={{display:"flex", gap:"0.75rem", alignItems:"center", flexWrap:"wrap"}}>
        <div className="search-bar" style={{flex:1, minWidth:180}}>
          <svg width="14" height="14" fill="none" stroke="var(--muted)" strokeWidth="2" viewBox="0 0 24 24">
            <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
          </svg>
          <input placeholder="Search by title..." value={keyword} onChange={(e) => setKeyword(e.target.value)} />
        </div>
        <div className="search-bar" style={{flex:1, minWidth:150}}>
          <input placeholder="Filter by genre..." value={genre} onChange={(e) => setGenre(e.target.value)} />
        </div>
        <button className="btn" onClick={() => setPage(1)}>Search</button>
      </div>

      <ErrorNotice message={error} />

      {trending.length > 0 && (
        <>
          <div className="section-header">
            <h2 className="section-title">🔥 Trending <span>Now</span></h2>
          </div>
          <div className="movie-row">
            {trending.map((item, i) => (
              <MovieCard key={item.movie.id} movie={item.movie} index={i} />
            ))}
          </div>
        </>
      )}

      <div className="section-header" style={{marginTop:"1.5rem"}}>
        <h2 className="section-title">All <span>Movies</span></h2>
        {movies && <span style={{color:"var(--muted)", fontSize:13}}>{movies.totalElements} films</span>}
      </div>
      <div className="grid cards">
        {movies?.content.map((movie, i) => (
          <MovieCard key={movie.id} movie={movie} index={i} />
        ))}
      </div>

      {movies && movies.totalPages > 1 && (
        <div className="pager">
          <button className="btn ghost" disabled={page <= 1} onClick={() => setPage((p) => p - 1)}>← Prev</button>
          <span>Page {movies.page} / {movies.totalPages}</span>
          <button className="btn ghost" disabled={page >= movies.totalPages} onClick={() => setPage((p) => p + 1)}>Next →</button>
        </div>
      )}
    </section>
  );
}
