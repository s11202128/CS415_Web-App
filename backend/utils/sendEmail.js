const { Resend } = require('resend');
require('dotenv').config();

const resend = new Resend(process.env.RESEND_API_KEY);

/**
 * Send an email using Resend API
 * @param {string} to - Recipient email
 * @param {string} subject - Email subject
 * @param {string} html - HTML content
 */
async function sendEmail(to, subject, html) {
  try {
    await resend.emails.send({
      from: 'noreply@yourdomain.com',
      to,
      subject,
      html
    });
  } catch (err) {
    console.error('Error sending email:', err);
    throw new Error('Failed to send email');
  }
}

module.exports = sendEmail;
