const pool = require('../config/db');

/**
 * Save OTP code
 */
async function saveOTP(email, code, type, expiresAt) {
  await pool.query(
    'INSERT INTO otp_codes (email, code, type, expires_at) VALUES (?, ?, ?, ?)',
    [email, code, type, expiresAt]
  );
}

/**
 * Get latest OTP for email and type
 */
async function getOTP(email, type) {
  const [rows] = await pool.query(
    'SELECT * FROM otp_codes WHERE email = ? AND type = ? ORDER BY created_at DESC LIMIT 1',
    [email, type]
  );
  return rows[0];
}

/**
 * Delete OTP by id
 */
async function deleteOTP(id) {
  await pool.query('DELETE FROM otp_codes WHERE id = ?', [id]);
}

module.exports = {
  saveOTP,
  getOTP,
  deleteOTP
};
