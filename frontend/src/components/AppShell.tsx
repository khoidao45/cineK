import { Link, NavLink, useNavigate } from "react-router-dom";
import { useAuthStore } from "../store/auth";
import { api } from "../lib/api";

export function AppShell({ children }: { children: React.ReactNode }) {
  const navigate = useNavigate();
  const { isAuthed, isAdmin, clearSession } = useAuthStore();

  const logout = async () => {
    const { refreshToken } = useAuthStore.getState();
    try {
      await api.post("/api/auth/logout", { refreshToken });
    } catch {
      // Ignore API logout failure and clear local state anyway.
    } finally {
      clearSession();
      navigate("/login");
    }
  };

  return (
    <div className="app-bg">
      <header className="topbar">
        <Link to="/" className="brand">MovieFlow</Link>
        <nav>
          <NavLink to="/" className="nav">Home</NavLink>
          <NavLink to="/me/recommendations" className="nav">For You</NavLink>
          <NavLink to="/me/continue-watching" className="nav">Continue</NavLink>
          {isAdmin() && <NavLink to="/admin/movies" className="nav">Admin</NavLink>}
        </nav>
        <div className="actions">
          {!isAuthed() && <Link className="btn ghost" to="/login">Login</Link>}
          {!isAuthed() && <Link className="btn" to="/register">Register</Link>}
          {isAuthed() && <Link className="btn ghost" to="/me/profile">Profile</Link>}
          {isAuthed() && <button className="btn" onClick={logout}>Logout</button>}
        </div>
      </header>
      <main className="container">{children}</main>
    </div>
  );
}
