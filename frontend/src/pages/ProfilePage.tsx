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

  useEffect(() => {
    void load();
  }, []);

  const submit = async (e: React.FormEvent) => {
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

  if (!profile) return <section>Loading profile...</section>;

  return (
    <section className="form-wrap">
      <h1>My Profile</h1>
      <div className="card">
        <p><strong>Username:</strong> {profile.username}</p>
        <p><strong>Email:</strong> {profile.email}</p>
        <p><strong>Role:</strong> {profile.role}</p>
      </div>
      <form className="card form" onSubmit={submit}>
        <input className="input" value={fullName} onChange={(e) => setFullName(e.target.value)} placeholder="Full name" />
        <input className="input" value={avatarUrl} onChange={(e) => setAvatarUrl(e.target.value)} placeholder="Avatar URL" />
        <button className="btn" type="submit">Save</button>
        {ok && <div className="ok-box">{ok}</div>}
        <ErrorNotice message={error} />
      </form>
    </section>
  );
}
