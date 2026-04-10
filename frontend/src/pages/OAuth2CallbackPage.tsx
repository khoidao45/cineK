import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuthStore } from "../store/auth";
import type { Role } from "../types/api";

function parseJwt(token: string): Record<string, unknown> | null {
  try {
    const base64 = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(atob(base64));
  } catch {
    return null;
  }
}

export function OAuth2CallbackPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { setSession } = useAuthStore();

  useEffect(() => {
    const token = params.get("token");
    const refreshToken = params.get("refreshToken");

    if (!token || !refreshToken) {
      navigate("/login");
      return;
    }

    const payload = parseJwt(token);
    const userId = payload?.userId as number | undefined;
    const role = payload?.role as Role | undefined;

    if (!userId || !role) {
      navigate("/login");
      return;
    }

    setSession({ token, refreshToken, userId, role });
    navigate("/");
  }, [params, navigate, setSession]);

  return (
    <section className="form-wrap">
      <p>Signing you in...</p>
    </section>
  );
}
