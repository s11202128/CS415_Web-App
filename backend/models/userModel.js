const pool = require('../config/db');

/**
 * Find user by email
 */
async function findUserByEmail(email) {
  const [rows] = await pool.query('SELECT * FROM users WHERE email = ?', [email]);
  return rows[0];
}

/**
 * Create a new user
 */
async function createUser(email, hashedPassword) {
  await pool.query(
    'INSERT INTO users (email, password, is_verified) VALUES (?, ?, ?)',
    [email, hashedPassword, false]
  );
}

/**
 * Set user as verified
 */
async function verifyUser(email) {
  await pool.query('UPDATE users SET is_verified = ? WHERE email = ?', [true, email]);
}

module.exports = {
  findUserByEmail,
  createUser,
  verifyUser
};
