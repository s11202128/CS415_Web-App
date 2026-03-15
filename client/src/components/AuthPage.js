import { useState } from "react";
import { api, setToken } from "../api";
import SiteFooter from "./SiteFooter";

export default function AuthPage({ onLoginSuccess, currentYear }) {
  const [authView, setAuthView] = useState("login");
  const [authForm, setAuthForm] = useState({
    fullName: "",
    mobile: "",
    email: "",
    password: "",
    confirmPassword: "",
  });
  const [verifyForm, setVerifyForm] = useState({ email: "", code: "" });
  const [resetForm, setResetForm] = useState({ email: "", resetId: "", code: "", newPassword: "", confirmPassword: "" });
  const [authMessage, setAuthMessage] = useState("");
  const [authHint, setAuthHint] = useState("");

  function onAuthViewChange(view) {
    setAuthView(view);
    setAuthMessage("");
    setAuthHint("");
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
        email: result.email,
        mobile: result.mobile,
        nationalId: result.nationalId,
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
      const result = await api.register({
        fullName: authForm.fullName,
        mobile: authForm.mobile,
        email: authForm.email,
        password: authForm.password,
        confirmPassword: authForm.confirmPassword,
      });
      setAuthMessage(result.message || "Registration successful. Verify your email before login.");
      setAuthHint(result.simulatedVerificationCode ? `Verification code: ${result.simulatedVerificationCode}` : "");
      setVerifyForm({ email: authForm.email, code: result.simulatedVerificationCode || "" });
      setAuthView("verify");
      setAuthForm({ ...authForm, password: "", confirmPassword: "" });
    } catch (err) {
      setAuthMessage(err.message);
    }
  }

  async function onVerifyEmail(e) {
    e.preventDefault();
    setAuthMessage("");
    try {
      await api.verifyEmail(verifyForm);
      setAuthMessage("Email verified. You can now sign in.");
      setAuthHint("");
      setAuthView("login");
    } catch (err) {
      setAuthMessage(err.message);
    }
  }

  async function onRequestReset(e) {
    e.preventDefault();
    setAuthMessage("");
    try {
      const result = await api.requestPasswordReset({ email: resetForm.email });
      setAuthMessage(result.message || "Password reset code generated.");
      setAuthHint(result.simulatedResetCode ? `Reset ID: ${result.resetId} | Code: ${result.simulatedResetCode}` : "");
      setResetForm((prev) => ({
        ...prev,
        resetId: result.resetId || "",
        code: result.simulatedResetCode || "",
      }));
      setAuthView("reset");
    } catch (err) {
      setAuthMessage(err.message);
    }
  }

  async function onResetPassword(e) {
    e.preventDefault();
    setAuthMessage("");
    if (resetForm.newPassword !== resetForm.confirmPassword) {
      setAuthMessage("Passwords do not match");
      return;
    }
    try {
      await api.resetPassword({
        email: resetForm.email,
        resetId: resetForm.resetId,
        otp: resetForm.code,
        newPassword: resetForm.newPassword,
      });
      setAuthMessage("Password reset complete. Sign in with your new password.");
      setAuthHint("");
      setAuthView("login");
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
              <p className="auth-switch">
                Forgot your password?{" "}
                <button type="button" className="link-btn" onClick={() => onAuthViewChange("forgot")}>
                  Reset it here
                </button>
              </p>
            </>
          ) : authView === "register" ? (
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
              {authHint && <p className="otp-notice">{authHint}</p>}
              <p className="auth-switch">
                Already have an account?{" "}
                <button type="button" className="link-btn" onClick={() => onAuthViewChange("login")}>
                  Sign in here
                </button>
              </p>
            </>
          ) : authView === "verify" ? (
            <>
              <h2>Verify Email</h2>
              <form onSubmit={onVerifyEmail}>
                <label>
                  Email Address
                  <input
                    type="email"
                    value={verifyForm.email}
                    onChange={(e) => setVerifyForm({ ...verifyForm, email: e.target.value })}
                    required
                  />
                </label>
                <label>
                  Verification Code
                  <input
                    value={verifyForm.code}
                    onChange={(e) => setVerifyForm({ ...verifyForm, code: e.target.value })}
                    required
                  />
                </label>
                <button type="submit" className="btn-primary">Verify Email</button>
              </form>
              {authMessage && <p className="status error">{authMessage}</p>}
              {authHint && <p className="otp-notice">{authHint}</p>}
              <p className="auth-switch">
                Back to{" "}
                <button type="button" className="link-btn" onClick={() => onAuthViewChange("login")}>
                  Sign in
                </button>
              </p>
            </>
          ) : authView === "forgot" ? (
            <>
              <h2>Forgot Password</h2>
              <form onSubmit={onRequestReset}>
                <label>
                  Email Address
                  <input
                    type="email"
                    value={resetForm.email}
                    onChange={(e) => setResetForm({ ...resetForm, email: e.target.value })}
                    required
                  />
                </label>
                <button type="submit" className="btn-primary">Send Reset Code</button>
              </form>
              {authMessage && <p className="status error">{authMessage}</p>}
              {authHint && <p className="otp-notice">{authHint}</p>}
              <p className="auth-switch">
                Remembered it?{" "}
                <button type="button" className="link-btn" onClick={() => onAuthViewChange("login")}>
                  Sign in here
                </button>
              </p>
            </>
          ) : (
            <>
              <h2>Reset Password</h2>
              <form onSubmit={onResetPassword}>
                <label>
                  Email Address
                  <input
                    type="email"
                    value={resetForm.email}
                    onChange={(e) => setResetForm({ ...resetForm, email: e.target.value })}
                    required
                  />
                </label>
                <label>
                  Reset ID
                  <input
                    value={resetForm.resetId}
                    onChange={(e) => setResetForm({ ...resetForm, resetId: e.target.value })}
                    required
                  />
                </label>
                <label>
                  Reset Code
                  <input
                    value={resetForm.code}
                    onChange={(e) => setResetForm({ ...resetForm, code: e.target.value })}
                    required
                  />
                </label>
                <label>
                  New Password
                  <input
                    type="password"
                    value={resetForm.newPassword}
                    onChange={(e) => setResetForm({ ...resetForm, newPassword: e.target.value })}
                    required
                  />
                </label>
                <label>
                  Confirm New Password
                  <input
                    type="password"
                    value={resetForm.confirmPassword}
                    onChange={(e) => setResetForm({ ...resetForm, confirmPassword: e.target.value })}
                    required
                  />
                </label>
                <button type="submit" className="btn-primary">Reset Password</button>
              </form>
              {authMessage && <p className="status error">{authMessage}</p>}
              {authHint && <p className="otp-notice">{authHint}</p>}
              <p className="auth-switch">
                Back to{" "}
                <button type="button" className="link-btn" onClick={() => onAuthViewChange("login")}>
                  Sign in
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
