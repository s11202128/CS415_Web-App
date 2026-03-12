import { useState } from "react";
import { api, setToken } from "../api";
import SiteFooter from "./SiteFooter";

export default function AuthPage({ onLoginSuccess, currentYear }) {
  const [authView, setAuthView] = useState("login");
  const [authForm, setAuthForm] = useState({ fullName: "", mobile: "", email: "", password: "", confirmPassword: "" });
  const [authMessage, setAuthMessage] = useState("");

  function onAuthViewChange(view) {
    setAuthView(view);
    setAuthMessage("");
  }

  async function onLogin(e) {
    e.preventDefault();
    setAuthMessage("");
    try {
      const result = await api.login({ email: authForm.email, password: authForm.password });
      setToken(result.token);
      onLoginSuccess(result.token, {
        fullName: result.fullName,
        userId: result.userId,
        customerId: result.customerId,
        isAdmin: Boolean(result.isAdmin),
      });
    } catch (err) {
      setAuthMessage(err.message);
    }
  }

  async function onRegister(e) {
    e.preventDefault();
    setAuthMessage("");
    if (authForm.password !== authForm.confirmPassword) {
      setAuthMessage("Passwords do not match");
      return;
    }
    try {
      await api.register({
        fullName: authForm.fullName,
        mobile: authForm.mobile,
        email: authForm.email,
        password: authForm.password,
      });
      setAuthMessage("Registration successful! You can now log in.");
      setAuthView("login");
      setAuthForm({ ...authForm, password: "", confirmPassword: "" });
    } catch (err) {
      setAuthMessage(err.message);
    }
  }

  return (
    <div className="app-shell">
      <header className="hero">
        <h1>Bank of Fiji Online Banking</h1>
        <p>{authView === "login" ? "Sign in to access your banking dashboard." : "Create your online banking account."}</p>
      </header>
      <section className="panel-grid">
        <article className="panel auth-card">
          {authView === "login" ? (
            <>
              <h2>Sign In</h2>
              <form onSubmit={onLogin}>
                <label>
                  Email
                  <input
                    type="email"
                    value={authForm.email}
                    onChange={(e) => setAuthForm({ ...authForm, email: e.target.value })}
                    required
                    autoComplete="email"
                  />
                </label>
                <label>
                  Password
                  <input
                    type="password"
                    value={authForm.password}
                    onChange={(e) => setAuthForm({ ...authForm, password: e.target.value })}
                    required
                    autoComplete="current-password"
                  />
                </label>
                <button type="submit" className="btn-primary">Sign In</button>
              </form>
              {authMessage && <p className={authMessage.startsWith("Registration") ? "status ok" : "status error"}>{authMessage}</p>}
              <p className="auth-switch">
                Don&apos;t have an account?{" "}
                <button type="button" className="link-btn" onClick={() => onAuthViewChange("register")}>
                  Register here
                </button>
              </p>
            </>
          ) : (
            <>
              <h2>Create Account</h2>
              <form onSubmit={onRegister}>
                <label>
                  Full Name
                  <input
                    value={authForm.fullName}
                    onChange={(e) => setAuthForm({ ...authForm, fullName: e.target.value })}
                    required
                    autoComplete="name"
                  />
                </label>
                <label>
                  Mobile Number
                  <input
                    value={authForm.mobile}
                    placeholder="+6797001001"
                    onChange={(e) => setAuthForm({ ...authForm, mobile: e.target.value })}
                    required
                    autoComplete="tel"
                  />
                </label>
                <label>
                  Email Address
                  <input
                    type="email"
                    value={authForm.email}
                    onChange={(e) => setAuthForm({ ...authForm, email: e.target.value })}
                    required
                    autoComplete="email"
                  />
                </label>
                <label>
                  Password <span className="hint-inline">(min 8 characters)</span>
                  <input
                    type="password"
                    value={authForm.password}
                    onChange={(e) => setAuthForm({ ...authForm, password: e.target.value })}
                    required
                    minLength={8}
                    autoComplete="new-password"
                  />
                </label>
                <label>
                  Confirm Password
                  <input
                    type="password"
                    value={authForm.confirmPassword}
                    onChange={(e) => setAuthForm({ ...authForm, confirmPassword: e.target.value })}
                    required
                    autoComplete="new-password"
                  />
                </label>
                <button type="submit" className="btn-primary">Create Account</button>
              </form>
              {authMessage && <p className="status error">{authMessage}</p>}
              <p className="auth-switch">
                Already have an account?{" "}
                <button type="button" className="link-btn" onClick={() => onAuthViewChange("login")}>
                  Sign in here
                </button>
              </p>
            </>
          )}
        </article>

      </section>

      <SiteFooter currentYear={currentYear} />
    </div>
  );
}
