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

  return (
    <section className="form-wrap">
      <h1>Create Account</h1>
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
