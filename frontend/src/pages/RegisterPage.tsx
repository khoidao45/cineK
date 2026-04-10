import { useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../lib/api";
import type { AuthResponse } from "../types/api";
import { ErrorNotice } from "../components/ErrorNotice";
import { useErrorMessage } from "../hooks/useErrorMessage";

export function RegisterPage() {
  const { parseError } = useErrorMessage();
  const [form, setForm] = useState({
    username: "",
    email: "",
    password: "",
    fullName: "",
    avatarUrl: "",
  });
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setMessage(null);
    setError(null);
    try {
      const res = await api.post<AuthResponse>("/api/auth/register", form);
      setMessage(res.data.message || "Register success. Please verify email.");
    } catch (err) {
      setError(parseError(err));
    }
  };

  const apiBase = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

  return (
    <section className="form-wrap">
      <h1>Create Account</h1>
      <a className="btn ghost google-btn" href={`${apiBase}/oauth2/authorization/google`}>
        <svg width="18" height="18" viewBox="0 0 48 48" style={{marginRight: 8}}><path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/><path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/><path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/><path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/></svg>
        Continue with Google
      </a>
      <div className="divider">or register with email</div>
      <form className="card form" onSubmit={submit}>
        <input className="input" placeholder="Username" required value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} />
        <input className="input" placeholder="Email" required type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
        <input className="input" placeholder="Full name" required value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
        <input className="input" placeholder="Password" required type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
        <input className="input" placeholder="Avatar URL (optional)" value={form.avatarUrl} onChange={(e) => setForm({ ...form, avatarUrl: e.target.value })} />
        <button className="btn" type="submit">Register</button>
        {message && <div className="ok-box">{message}</div>}
        <ErrorNotice message={error} />
      </form>
      <p>Already have account? <Link to="/login">Login</Link></p>
    </section>
  );
}
