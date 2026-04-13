import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { api } from "../lib/api";
import type { MovieResponse } from "../types/api";
import { useErrorMessage } from "../hooks/useErrorMessage";
import { ErrorNotice } from "../components/ErrorNotice";

export function WatchPage() {
  const { movieId } = useParams();
  const id = Number(movieId);
  const { parseError } = useErrorMessage();
  const [movie, setMovie] = useState<MovieResponse | null>(null);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    void api.get<MovieResponse>(`/api/movies/${id}`)
      .then((res) => setMovie(res.data))
      .catch((e) => setError(parseError(e)));
  }, [id, parseError]);

  const updateProgress = async () => {
    try {
      await api.post("/api/history", { movieId: id, progress });
    } catch (e) {
      setError(parseError(e));
    }
  };

  if (!movie) return <section style={{padding:"2rem", color:"var(--muted)"}}>Loading...</section>;

  // Use videoUrl directly if it's an external URL (Supabase, archive.org, etc.)
  // Otherwise fall back to backend streaming proxy
  const apiBase = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
  const videoSrc = movie.videoUrl?.startsWith("http")
    ? movie.videoUrl
    : `${apiBase}/api/videos/${id}`;

  return (
    <section className="watch-wrap">
      <div className="watch-meta">
        <strong>{movie.title}</strong>
        <span className="badge">{movie.genre}</span>
        <span className="badge">{movie.releaseYear}</span>
        <span className="badge">{movie.duration}m</span>
        {movie.director && <span style={{color:"var(--muted)"}}>Dir. {movie.director}</span>}
      </div>

      <ErrorNotice message={error} />

      {movie.videoUrl ? (
        <video
          className="video"
          controls
          src={videoSrc}
          onTimeUpdate={(e) => {
            const el = e.currentTarget;
            if (!el.duration) return;
            setProgress(Math.round((el.currentTime / el.duration) * 100));
          }}
        />
      ) : (
        <div className="card" style={{textAlign:"center", padding:"3rem", color:"var(--muted)"}}>
          No video available for this movie yet.
        </div>
      )}

      <div className="card" style={{display:"flex", alignItems:"center", gap:"1rem", justifyContent:"space-between"}}>
        <span style={{color:"var(--muted)", fontSize:13}}>Progress: <strong style={{color:"var(--text)"}}>{progress}%</strong></span>
        <button className="btn" onClick={updateProgress}>Save Progress</button>
      </div>

      {movie.description && (
        <div className="card">
          <p style={{color:"var(--muted)", fontSize:14, lineHeight:1.7}}>{movie.description}</p>
          {movie.actors && (
            <p style={{color:"var(--muted)", fontSize:13, marginTop:"0.5rem"}}>
              <strong style={{color:"var(--text)"}}>Cast:</strong> {movie.actors}
            </p>
          )}
        </div>
      )}
    </section>
  );
}
