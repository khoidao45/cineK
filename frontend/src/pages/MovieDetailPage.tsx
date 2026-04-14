import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../lib/api";
import type { MovieResponse, ReviewResponse } from "../types/api";
import { useAuthStore } from "../store/auth";
import { useErrorMessage } from "../hooks/useErrorMessage";
import { ErrorNotice } from "../components/ErrorNotice";

const GRADIENTS = ["pg-1","pg-2","pg-3","pg-4","pg-5","pg-6","pg-7","pg-8"];

export function MovieDetailPage() {
  const { id } = useParams();
  const movieId = Number(id);
  const { parseError } = useErrorMessage();
  const authed = useAuthStore((s) => s.isAuthed());
  const currentUserId = useAuthStore((s) => s.userId);
  const isAdmin = useAuthStore((s) => s.isAdmin());

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

  useEffect(() => { void load(); }, [id]);

  const deleteReview = async (reviewId: number) => {
    try {
      await api.delete(`/api/reviews/${reviewId}`);
      await load();
    } catch (err) {
      setError(parseError(err));
    }
  };

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

  if (!movie) return <section style={{padding:"2rem", color:"var(--muted)"}}>Loading...</section>;

  const pg = GRADIENTS[movieId % GRADIENTS.length];

  return (
    <section>
      <ErrorNotice message={error} />

      <div className="detail-hero">
        <div className="detail-poster">
          {movie.posterUrl
            ? <img src={movie.posterUrl} alt={movie.title} />
            : <div className={`detail-poster-placeholder ${pg}`} style={{width:"100%",height:"100%"}} />}
        </div>
        <div className="detail-info">
          <h1 className="detail-title">{movie.title}</h1>
          <div className="detail-tags">
            <span className="tag">{movie.genre}</span>
            <span className="tag">{movie.releaseYear}</span>
            <span className="tag">{movie.duration}m</span>
            {movie.director && <span className="tag">Dir. {movie.director}</span>}
          </div>
          <div style={{display:"flex", alignItems:"center", gap:8}}>
            <span className="rating-big">★ {movie.ratingAvg.toFixed(1)}</span>
            <span style={{color:"var(--muted)", fontSize:13}}>({movie.ratingCount} reviews)</span>
            <span style={{color:"var(--muted)", fontSize:13}}>· {movie.views} views</span>
          </div>
          {movie.description && <p className="detail-desc">{movie.description}</p>}
          {movie.actors && (
            <p style={{fontSize:13, color:"var(--muted)"}}>
              <strong style={{color:"var(--text)"}}>Cast:</strong> {movie.actors}
            </p>
          )}
          {authed && (
            <div style={{marginTop:"0.5rem"}}>
              <Link className="btn" to={`/watch/${movie.id}`} style={{display:"inline-flex", gap:8}}>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="white"><path d="M5 3l14 9-14 9V3z"/></svg>
                Watch Now
              </Link>
            </div>
          )}
        </div>
      </div>

      <h2 className="section-title">Reviews <span>({reviews.length})</span></h2>
      <div className="grid cards" style={{marginBottom:"1rem"}}>
        {reviews.map((r) => (
          <article className="card" key={r.id} style={{margin:0}}>
            <div style={{display:"flex", justifyContent:"space-between", alignItems:"center", marginBottom:4}}>
              <strong style={{fontSize:13}}>{r.username}</strong>
              <div style={{display:"flex", alignItems:"center", gap:8}}>
                <span style={{color:"var(--gold)", fontSize:13}}>{"★".repeat(r.rating)}{"☆".repeat(5-r.rating)}</span>
                {(isAdmin || r.userId === currentUserId) && (
                  <button
                    onClick={() => deleteReview(r.id)}
                    style={{background:"none", border:"none", color:"#f66", cursor:"pointer", fontSize:13, padding:"2px 6px"}}
                    title="Delete review"
                  >✕</button>
                )}
              </div>
            </div>
            <p style={{color:"var(--muted)", fontSize:13}}>{r.comment || "No comment"}</p>
          </article>
        ))}
      </div>

      {authed && (
        <form className="card form" onSubmit={submitReview}>
          <h3 style={{fontSize:15, fontWeight:600}}>Write a Review</h3>
          <div style={{display:"flex", gap:8, alignItems:"center"}}>
            <label style={{color:"var(--muted)", fontSize:13}}>Rating:</label>
            <input className="input" type="number" min={1} max={5} value={rating}
              onChange={(e) => setRating(Number(e.target.value))}
              style={{width:80}} />
            <span style={{color:"var(--gold)"}}>{"★".repeat(rating)}{"☆".repeat(5-rating)}</span>
          </div>
          <textarea className="input" value={comment} onChange={(e) => setComment(e.target.value)} placeholder="Your comment..." />
          <button className="btn" type="submit">Submit Review</button>
        </form>
      )}
    </section>
  );
}
