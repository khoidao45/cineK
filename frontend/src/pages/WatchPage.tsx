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

  if (!movie) return <section>Loading stream...</section>;

  return (
    <section>
      <h1>{movie.title}</h1>
      <ErrorNotice message={error} />
      <video
        className="video"
        controls
        src={`${import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"}/api/videos/${id}`}
        onTimeUpdate={(e) => {
          const el = e.currentTarget;
          if (!el.duration) return;
          setProgress(Math.round((el.currentTime / el.duration) * 100));
        }}
      />
      <div className="card">
        <p>Current progress: {progress}%</p>
        <button className="btn" onClick={updateProgress}>Save Progress</button>
      </div>
    </section>
  );
}
