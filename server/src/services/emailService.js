const nodemailer = require('nodemailer');

function requireEmailConfig() {
  const user = process.env.EMAIL_USER;
  const pass = process.env.EMAIL_PASS;
  if (!user || !pass) {
    throw new Error('Email credentials are not configured');
  }
  return { user, pass };
}

function buildTransport() {
  const { user, pass } = requireEmailConfig();
  const host = process.env.EMAIL_HOST;
  const port = Number(process.env.EMAIL_PORT) || 587;
  const secure = String(process.env.EMAIL_SECURE || '').toLowerCase() === 'true' || port === 465;

  if (host) {
    return nodemailer.createTransport({
      host,
      port,
      secure,
      auth: { user, pass },
    });
  }

  return nodemailer.createTransport({
    service: 'gmail',
    auth: { user, pass },
  });
}

function resolveFromAddress() {
  return process.env.EMAIL_FROM || process.env.EMAIL_USER;
}

async function sendEmail({ to, subject, text, html }) {
  if (!to) {
    throw new Error('Recipient email is required');
  }
  if (!subject) {
    throw new Error('Email subject is required');
  }

  const transporter = buildTransport();
  const from = resolveFromAddress();

  await transporter.sendMail({
    from,
    to,
    subject,
    text,
    html,
  });
}

function buildVerificationUrl(token) {
  const baseUrl = process.env.BASE_URL || 'http://localhost:4000';
  const trimmed = baseUrl.replace(/\/$/, '');
  return `${trimmed}/api/auth/verify/${encodeURIComponent(token)}`;
}

async function sendVerificationEmail({ to, token }) {
  const verificationUrl = buildVerificationUrl(token);
  const subject = 'Verify your email';
  const text = `Welcome! Please verify your email by visiting: ${verificationUrl}`;
  const html = `<p>Welcome!</p><p>Please verify your email by clicking the link below:</p><p><a href="${verificationUrl}">${verificationUrl}</a></p>`;

  await sendEmail({ to, subject, text, html });

  return {
    to,
    verificationUrl,
  };
}

module.exports = {
  sendEmail,
  sendVerificationEmail,
  buildVerificationUrl,
};
