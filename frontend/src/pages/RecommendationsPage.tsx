import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../lib/api";
import { useAuthStore } from "../store/auth";
import type { RecommendationResponse } from "../types/api";
import { useErrorMessage } from "../hooks/useErrorMessage";
import { ErrorNotice } from "../components/ErrorNotice";

export function RecommendationsPage() {
  const { userId } = useAuthStore();
  const { parseError } = useErrorMessage();
  const [items, setItems] = useState<RecommendationResponse[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!userId) return;
    void api.get<RecommendationResponse[]>(`/api/recommendations/${userId}`, { params: { limit: 20 } })
      .then((res) => setItems(res.data))
      .catch((e) => setError(parseError(e)));
  }, [userId, parseError]);

  return (
    <section>
      <h1>Recommended for You</h1>
      <ErrorNotice message={error} />
      <div className="grid cards">
        {items.map((it) => (
          <Link className="movie-card" key={it.movie.id} to={`/movies/${it.movie.id}`}>
            <strong>{it.movie.title}</strong>
            <span>Final Score {it.score.toFixed(2)}</span>
            <span>Similarity {it.similarityScore.toFixed(2)} · Trend {it.trendingScore.toFixed(2)}</span>
          </Link>
        ))}
      </div>
    </section>
  );
}
