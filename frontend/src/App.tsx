import { BrowserRouter, Route, Routes } from "react-router-dom";
import { AppShell } from "./components/AppShell";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { AdminMovieFormPage } from "./pages/AdminMovieFormPage";
import { AdminMoviesPage } from "./pages/AdminMoviesPage";
import { ContinueWatchingPage } from "./pages/ContinueWatchingPage";
import { HomePage } from "./pages/HomePage";
import { LoginPage } from "./pages/LoginPage";
import { MovieDetailPage } from "./pages/MovieDetailPage";
import { NotFoundPage } from "./pages/NotFoundPage";
import { OAuth2CallbackPage } from "./pages/OAuth2CallbackPage";
import { ProfilePage } from "./pages/ProfilePage";
import { RecommendationsPage } from "./pages/RecommendationsPage";
import { RegisterPage } from "./pages/RegisterPage";
import { VerifyEmailPage } from "./pages/VerifyEmailPage";
import { WatchPage } from "./pages/WatchPage";

export default function App() {
  return (
    <BrowserRouter>
      <AppShell>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/verify-email" element={<VerifyEmailPage />} />
          <Route path="/oauth2/callback" element={<OAuth2CallbackPage />} />
          <Route path="/movies/:id" element={<MovieDetailPage />} />

          <Route element={<ProtectedRoute />}>
            <Route path="/me/profile" element={<ProfilePage />} />
            <Route path="/me/continue-watching" element={<ContinueWatchingPage />} />
            <Route path="/me/recommendations" element={<RecommendationsPage />} />
            <Route path="/watch/:movieId" element={<WatchPage />} />
          </Route>

          <Route element={<ProtectedRoute adminOnly />}>
            <Route path="/admin/movies" element={<AdminMoviesPage />} />
            <Route path="/admin/movies/new" element={<AdminMovieFormPage />} />
            <Route path="/admin/movies/:id/edit" element={<AdminMovieFormPage />} />
          </Route>

          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </AppShell>
    </BrowserRouter>
  );
}
