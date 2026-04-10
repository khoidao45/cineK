import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../lib/api";
import type { WatchHistoryResponse } from "../types/api";
import { useErrorMessage } from "../hooks/useErrorMessage";
import { ErrorNotice } from "../components/ErrorNotice";

export function ContinueWatchingPage() {
  const { parseError } = useErrorMessage();
  const [items, setItems] = useState<WatchHistoryResponse[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    void api.get<{ content: WatchHistoryResponse[] }>("/api/history/continue", { params: { page: 0, size: 20 } })
      .then((res) => setItems(res.data.content || []))
      .catch((e) => setError(parseError(e)));
  }, [parseError]);

  return (
    <section>
      <h1>Continue Watching</h1>
      <ErrorNotice message={error} />
      <div className="grid cards">
        {items.map((it) => (
          <Link className="movie-card" key={it.id} to={`/watch/${it.movieId}`}>
            <strong>{it.movieTitle}</strong>
            <span>Progress {it.progress}%</span>
          </Link>
        ))}
      </div>
    </section>
  );
}
