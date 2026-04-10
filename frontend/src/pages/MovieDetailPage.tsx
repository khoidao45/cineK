import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../lib/api";
import type { MovieResponse, ReviewResponse } from "../types/api";
import { useAuthStore } from "../store/auth";
import { useErrorMessage } from "../hooks/useErrorMessage";
import { ErrorNotice } from "../components/ErrorNotice";

export function MovieDetailPage() {
  const { id } = useParams();
  const movieId = Number(id);
  const { parseError } = useErrorMessage();
  const authed = useAuthStore((s) => s.isAuthed());

  const [movie, setMovie] = useState<MovieResponse | null>(null);
  const [reviews, setReviews] = useState<ReviewResponse[]>([]);
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState("");
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    try {
      const [mRes, rRes] = await Promise.all([
        api.get<MovieResponse>(`/api/movies/${movieId}`),
        api.get<{ content: ReviewResponse[] }>(`/api/movies/${movieId}/reviews`, { params: { page: 0, size: 20 } }),
      ]);
      setMovie(mRes.data);
      setReviews(rRes.data.content || []);
    } catch (err) {
      setError(parseError(err));
    }
  };

  useEffect(() => {
    void load();
  }, [id]);

  const submitReview = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      await api.post("/api/reviews", { movieId, rating, comment });
      setComment("");
      await load();
    } catch (err) {
      setError(parseError(err));
    }
  };

  if (!movie) return <section>Loading movie...</section>;

  return (
    <section>
      <h1>{movie.title}</h1>
      <p>{movie.description || "No description"}</p>
      <p>{movie.genre} · {movie.releaseYear} · {movie.duration}m</p>
      <p>Views {movie.views} · Rating {movie.ratingAvg.toFixed(1)} ({movie.ratingCount})</p>
      {authed && <Link className="btn" to={`/watch/${movie.id}`}>Watch now</Link>}
      <ErrorNotice message={error} />

      <h2 className="section-title">Reviews</h2>
      <div className="grid cards">
        {reviews.map((r) => (
          <article className="movie-card" key={r.id}>
            <strong>{r.username}</strong>
            <span>Rating {r.rating}/5</span>
            <span>{r.comment || "No comment"}</span>
          </article>
        ))}
      </div>

      {authed && (
        <form className="card form" onSubmit={submitReview}>
          <h3>Write a review</h3>
          <input className="input" type="number" min={1} max={5} value={rating} onChange={(e) => setRating(Number(e.target.value))} />
          <textarea className="input" value={comment} onChange={(e) => setComment(e.target.value)} placeholder="Your comment" />
          <button className="btn" type="submit">Submit</button>
        </form>
      )}
    </section>
  );
}
