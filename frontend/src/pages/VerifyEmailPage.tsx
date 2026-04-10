import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { api } from "../lib/api";
import type { AuthResponse } from "../types/api";
import { useErrorMessage } from "../hooks/useErrorMessage";

export function VerifyEmailPage() {
  const [params] = useSearchParams();
  const token = params.get("token");
  const { parseError } = useErrorMessage();
  const [message, setMessage] = useState("Verifying...");

  useEffect(() => {
    if (!token) {
      setMessage("Missing verification token.");
      return;
    }
    void api.get<AuthResponse>("/api/auth/verify-email", { params: { token } })
      .then((res) => setMessage(res.data.message || "Email verified."))
      .catch((e) => setMessage(parseError(e)));
  }, [token, parseError]);

  return (
    <section className="form-wrap">
      <h1>Email Verification</h1>
      <div className="card">{message}</div>
      <Link to="/login" className="btn">Go to Login</Link>
    </section>
  );
}
