import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <section className="form-wrap">
      <h1>Page Not Found</h1>
      <p>The page you requested does not exist.</p>
      <Link className="btn" to="/">Back to Home</Link>
    </section>
  );
}
