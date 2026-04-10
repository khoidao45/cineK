import { create } from "zustand";
import type { Role } from "../types/api";

const TOKEN_KEY = "mf_token";
const REFRESH_TOKEN_KEY = "mf_refresh_token";
const USER_ID_KEY = "mf_user_id";
const ROLE_KEY = "mf_role";

interface AuthState {
  token: string | null;
  refreshToken: string | null;
  userId: number | null;
  role: Role | null;
  setSession: (payload: {
    token: string;
    refreshToken: string;
    userId: number;
    role: Role;
  }) => void;
  clearSession: () => void;
  isAuthed: () => boolean;
  isAdmin: () => boolean;
}

const savedToken = localStorage.getItem(TOKEN_KEY);
const savedRefreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
const savedUserId = localStorage.getItem(USER_ID_KEY);
const savedRole = localStorage.getItem(ROLE_KEY);

export const useAuthStore = create<AuthState>((set, get) => ({
  token: savedToken,
  refreshToken: savedRefreshToken,
  userId: savedUserId ? Number(savedUserId) : null,
  role: savedRole,

  setSession: ({ token, refreshToken, userId, role }) => {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    localStorage.setItem(USER_ID_KEY, String(userId));
    localStorage.setItem(ROLE_KEY, role);
    set({ token, refreshToken, userId, role });
  },

  clearSession: () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_ID_KEY);
    localStorage.removeItem(ROLE_KEY);
    set({ token: null, refreshToken: null, userId: null, role: null });
  },

  isAuthed: () => Boolean(get().token),
  isAdmin: () => get().role === "ADMIN",
}));
