import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../lib/api";
import type { MovieResponse, PageResponse } from "../types/api";
import { useErrorMessage } from "../hooks/useErrorMessage";
import { ErrorNotice } from "../components/ErrorNotice";

export function AdminMoviesPage() {
  const { parseError } = useErrorMessage();
  const [items, setItems] = useState<MovieResponse[]>([]);
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    try {
      const res = await api.get<PageResponse<MovieResponse>>("/api/movies", { params: { page: 1, size: 50 } });
      setItems(res.data.content || []);
    } catch (err) {
      setError(parseError(err));
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const removeMovie = async (id: number) => {
    try {
      await api.delete(`/api/movies/${id}`);
      await load();
    } catch (err) {
      setError(parseError(err));
    }
  };

  return (
    <section>
      <div className="row-between">
        <h1>Admin Movies</h1>
        <Link className="btn" to="/admin/movies/new">+ New Movie</Link>
      </div>
      <ErrorNotice message={error} />
      <div className="grid cards">
        {items.map((m) => (
          <article className="movie-card" key={m.id}>
            <strong>{m.title}</strong>
            <span>{m.genre} · {m.releaseYear}</span>
            <div className="row-actions">
              <Link className="btn ghost" to={`/admin/movies/${m.id}/edit`}>Edit</Link>
              <button className="btn" onClick={() => removeMovie(m.id)}>Delete</button>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
