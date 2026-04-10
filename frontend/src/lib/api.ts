import axios, { AxiosError, type AxiosRequestConfig } from "axios";
import { useAuthStore } from "../store/auth";
import type { AuthResponse, ErrorResponse } from "../types/api";

const apiBase = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export const api = axios.create({
  baseURL: apiBase,
  timeout: 20000,
});

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshing = false;
let queue: Array<(token: string | null) => void> = [];

const flushQueue = (token: string | null) => {
  queue.forEach((cb) => cb(token));
  queue = [];
};

api.interceptors.response.use(
  (res) => res,
  async (error: AxiosError<ErrorResponse>) => {
    const original = error.config as AxiosRequestConfig & { _retry?: boolean };
    const status = error.response?.status;

    if (status === 401 && !original?._retry) {
      const { refreshToken, clearSession, setSession, userId, role } = useAuthStore.getState();
      if (!refreshToken) {
        clearSession();
        return Promise.reject(error);
      }

      if (refreshing) {
        return new Promise((resolve, reject) => {
          queue.push((newToken) => {
            if (!newToken || !original) {
              reject(error);
              return;
            }
            original.headers = original.headers || {};
            original.headers.Authorization = `Bearer ${newToken}`;
            resolve(api(original));
          });
        });
      }

      refreshing = true;
      original._retry = true;

      try {
        const refreshRes = await axios.post<AuthResponse>(
          `${apiBase}/api/auth/refresh`,
          null,
          {
            headers: {
              Authorization: `Bearer ${refreshToken}`,
            },
          }
        );

        const newAccess = refreshRes.data.token;
        const newRefresh = refreshRes.data.refreshToken || refreshToken;

        if (!newAccess || !userId || !role) {
          throw new Error("Invalid refresh response");
        }

        setSession({
          token: newAccess,
          refreshToken: newRefresh,
          userId,
          role,
        });

        flushQueue(newAccess);
        original.headers = original.headers || {};
        original.headers.Authorization = `Bearer ${newAccess}`;
        return api(original);
      } catch (refreshErr) {
        flushQueue(null);
        clearSession();
        return Promise.reject(refreshErr);
      } finally {
        refreshing = false;
      }
    }

    return Promise.reject(error);
  }
);
