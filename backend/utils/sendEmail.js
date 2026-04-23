const { Resend } = require('resend');
const resend = new Resend(process.env.RESEND_API_KEY);

async function sendVerificationEmail(email, token) {
  const link = `http://localhost:3000/verify?token=${token}`;
  const html = `<p>Please verify your email by clicking the link below:</p><a href="${link}">${link}</a>`;
  await resend.emails.send({
    from: 'onboarding@resend.dev',
    to: email,
    subject: 'Verify your email',
    html
  });
}

module.exports = sendVerificationEmail;
