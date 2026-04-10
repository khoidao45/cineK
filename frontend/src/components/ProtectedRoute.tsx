import { Navigate, Outlet } from "react-router-dom";
import { useAuthStore } from "../store/auth";

interface Props {
  adminOnly?: boolean;
}

export function ProtectedRoute({ adminOnly = false }: Props) {
  const isAuthed = useAuthStore((s) => s.isAuthed());
  const isAdmin = useAuthStore((s) => s.isAdmin());

  if (!isAuthed) return <Navigate to="/login" replace />;
  if (adminOnly && !isAdmin) return <Navigate to="/" replace />;

  return <Outlet />;
}
