import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../lib/api";
import { useAuthStore } from "../store/auth";
import type { AuthResponse } from "../types/api";
import { useErrorMessage } from "../hooks/useErrorMessage";
import { ErrorNotice } from "../components/ErrorNotice";

export function LoginPage() {
  const navigate = useNavigate();
  const { setSession } = useAuthStore();
  const { parseError } = useErrorMessage();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      const res = await api.post<AuthResponse>("/api/auth/login", { username, password });
      const data = res.data;
      if (!data.token || !data.refreshToken || !data.userId || !data.role) {
        throw new Error("Invalid login response");
      }
      setSession({
        token: data.token,
        refreshToken: data.refreshToken,
        userId: data.userId,
        role: data.role,
      });
      navigate("/");
    } catch (err) {
      setError(parseError(err));
    }
  };

  return (
    <section className="form-wrap">
      <h1>Login</h1>
      <form className="card form" onSubmit={submit}>
        <input className="input" placeholder="Username" value={username} onChange={(e) => setUsername(e.target.value)} required />
        <input className="input" placeholder="Password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        <button className="btn" type="submit">Sign In</button>
        <ErrorNotice message={error} />
      </form>
      <p>Need account? <Link to="/register">Register now</Link></p>
    </section>
  );
}
