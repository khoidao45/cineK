import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuthStore } from "../store/auth";
import type { Role } from "../types/api";

function parseJwt(token: string): Record<string, unknown> | null {
  try {
    const parts = token.split(".");
    if (parts.length !== 3) return null;
    // base64url → base64 + add padding so atob never throws
    const base64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const padded = base64 + "=".repeat((4 - (base64.length % 4)) % 4);
    return JSON.parse(atob(padded));
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
      navigate("/login?error=oauth_missing_token");
      return;
    }

    const payload = parseJwt(token);
    const userId = payload?.userId as number | undefined;
    const role = payload?.role as Role | undefined;

    if (!userId || !role) {
      navigate("/login?error=oauth_invalid_token");
      return;
    }

    setSession({ token, refreshToken, userId, role });
    navigate("/", { replace: true });
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <section className="form-wrap">
      <p>Signing you in...</p>
    </section>
  );
}
