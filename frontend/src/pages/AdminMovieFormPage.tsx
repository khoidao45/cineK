import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { api } from "../lib/api";
import type { MovieResponse } from "../types/api";
import { useErrorMessage } from "../hooks/useErrorMessage";
import { ErrorNotice } from "../components/ErrorNotice";

const emptyForm = {
  title: "",
  description: "",
  genre: "",
  duration: 90,
  releaseYear: new Date().getFullYear(),
  posterUrl: "",
  thumbnailUrl: "",
  videoUrl: "",
};

export function AdminMovieFormPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { parseError } = useErrorMessage();
  const isEdit = Boolean(id);
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isEdit) return;
    void api.get<MovieResponse>(`/api/movies/${id}`)
      .then((res) => setForm({
        title: res.data.title,
        description: res.data.description || "",
        genre: res.data.genre,
        duration: res.data.duration,
        releaseYear: res.data.releaseYear,
        posterUrl: res.data.posterUrl || "",
        thumbnailUrl: res.data.thumbnailUrl || "",
        videoUrl: res.data.videoUrl || "",
      }))
      .catch((e) => setError(parseError(e)));
  }, [id, isEdit, parseError]);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      if (isEdit) {
        await api.put(`/api/movies/${id}`, form);
      } else {
        await api.post("/api/movies", form);
      }
      navigate("/admin/movies");
    } catch (err) {
      setError(parseError(err));
    }
  };

  return (
    <section className="form-wrap">
      <h1>{isEdit ? "Edit Movie" : "Create Movie"}</h1>
      <form className="card form" onSubmit={submit}>
        <input className="input" required placeholder="Title" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
        <textarea className="input" placeholder="Description" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
        <input className="input" required placeholder="Genre" value={form.genre} onChange={(e) => setForm({ ...form, genre: e.target.value })} />
        <input className="input" type="number" min={1} required placeholder="Duration" value={form.duration} onChange={(e) => setForm({ ...form, duration: Number(e.target.value) })} />
        <input className="input" type="number" min={1888} required placeholder="Release year" value={form.releaseYear} onChange={(e) => setForm({ ...form, releaseYear: Number(e.target.value) })} />
        <input className="input" placeholder="Poster URL" value={form.posterUrl} onChange={(e) => setForm({ ...form, posterUrl: e.target.value })} />
        <input className="input" placeholder="Thumbnail URL" value={form.thumbnailUrl} onChange={(e) => setForm({ ...form, thumbnailUrl: e.target.value })} />
        <input className="input" placeholder="Video URL" value={form.videoUrl} onChange={(e) => setForm({ ...form, videoUrl: e.target.value })} />
        <button className="btn" type="submit">Save</button>
        <ErrorNotice message={error} />
      </form>
    </section>
  );
}
