import { useEffect, useState } from "react";
import { api } from "../lib/api";
import type { UserResponse } from "../types/api";
import { useErrorMessage } from "../hooks/useErrorMessage";
import { ErrorNotice } from "../components/ErrorNotice";

export function ProfilePage() {
  const { parseError } = useErrorMessage();
  const [profile, setProfile] = useState<UserResponse | null>(null);
  const [fullName, setFullName] = useState("");
  const [avatarUrl, setAvatarUrl] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [ok, setOk] = useState<string | null>(null);

  // Change-password state
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [pwError, setPwError] = useState<string | null>(null);
  const [pwOk, setPwOk] = useState<string | null>(null);

  const load = async () => {
    try {
      const res = await api.get<UserResponse>("/api/users/profile");
      setProfile(res.data);
      setFullName(res.data.fullName || "");
      setAvatarUrl(res.data.avatarUrl || "");
    } catch (err) {
      setError(parseError(err));
    }
  };

  useEffect(() => { void load(); }, []);

  const submitProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setOk(null);
    setError(null);
    try {
      await api.put("/api/users/profile", { fullName, avatarUrl });
      setOk("Profile updated.");
      await load();
    } catch (err) {
      setError(parseError(err));
    }
  };

  const submitChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setPwOk(null);
    setPwError(null);
    if (newPassword !== confirmPassword) {
      setPwError("Mật khẩu xác nhận không khớp.");
      return;
    }
    try {
      await api.post("/api/users/change-password", { oldPassword, newPassword, confirmPassword });
      setPwOk("Password changed successfully.");
      setOldPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (err) {
      setPwError(parseError(err));
    }
  };

  const resendVerification = async () => {
    setError(null);
    try {
      await api.post(`/api/auth/resend-verification?email=${encodeURIComponent(profile!.email)}`);
      setOk("Verification email sent. Please check your inbox.");
    } catch (err) {
      setError(parseError(err));
    }
  };

  if (!profile) return <section>Loading profile...</section>;

  const isLocal = !profile.provider || profile.provider === "LOCAL";
  const verified = profile.emailVerified !== false;

  return (
    <section className="form-wrap">
      <h1>My Profile</h1>

      <div className="card">
        <p><strong>Username:</strong> {profile.username}</p>
        <p><strong>Email:</strong> {profile.email}</p>
        <p><strong>Role:</strong> {profile.role}</p>
        <p><strong>Login via:</strong> {profile.provider || "LOCAL"}</p>
        <p>
          <strong>Email verified:</strong>{" "}
          {verified
            ? <span style={{ color: "var(--gold)" }}>✓ Verified</span>
            : <span style={{ color: "#f66" }}>✗ Not verified</span>}
          {!verified && isLocal && (
            <button
              className="btn ghost"
              style={{ marginLeft: 12, fontSize: 12, padding: "2px 10px" }}
              onClick={resendVerification}
            >
              Resend email
            </button>
          )}
        </p>
      </div>

      <form className="card form" onSubmit={submitProfile}>
        <h3 style={{ fontSize: 15, fontWeight: 600, marginBottom: 8 }}>Edit Profile</h3>
        <input className="input" value={fullName} onChange={(e) => setFullName(e.target.value)} placeholder="Full name" />
        <input className="input" value={avatarUrl} onChange={(e) => setAvatarUrl(e.target.value)} placeholder="Avatar URL" />
        <button className="btn" type="submit">Save</button>
        {ok && <div className="ok-box">{ok}</div>}
        <ErrorNotice message={error} />
      </form>

      {isLocal && (
        <form className="card form" onSubmit={submitChangePassword}>
          <h3 style={{ fontSize: 15, fontWeight: 600, marginBottom: 8 }}>Change Password</h3>
          <input className="input" type="password" value={oldPassword}
            onChange={(e) => setOldPassword(e.target.value)} placeholder="Current password" />
          <input className="input" type="password" value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)} placeholder="New password" />
          <input className="input" type="password" value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)} placeholder="Confirm new password" />
          <button className="btn" type="submit">Change Password</button>
          {pwOk && <div className="ok-box">{pwOk}</div>}
          <ErrorNotice message={pwError} />
        </form>
      )}
    </section>
  );
}
