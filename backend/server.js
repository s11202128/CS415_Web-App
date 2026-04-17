const express = require('express');
const cors = require('cors');
const dotenv = require('dotenv');
const pool = require('./config/db');

dotenv.config();

const app = express();

// Middleware
app.use(cors());
app.use(express.json());

// Routes
app.use('/api/auth', require('./routes/authRoutes'));
app.use('/api/transaction', require('./routes/transactionRoutes'));

// Test-only endpoint to fetch OTP for a user (for automated testing only)
if (process.env.NODE_ENV !== 'production') {
  app.get('/api/test/get-otp', async (req, res) => {
    const { email, type } = req.query;
    if (!email || !type) return res.status(400).json({ message: 'Missing email or type' });
    try {
      const [rows] = await pool.query(
        'SELECT code FROM otp_codes WHERE email = ? AND type = ? ORDER BY created_at DESC LIMIT 1',
        [email, type]
      );
      if (!rows.length) return res.status(404).json({ message: 'OTP not found' });
      res.json({ otp: rows[0].code });
    } catch (err) {
      res.status(500).json({ message: 'DB error' });
    }
  });
}

// Health check
app.get('/', (req, res) => {
  res.send('OTP Auth Server Running');
});

// Start server after DB connection
const PORT = process.env.PORT || 5000;

pool.getConnection()
  .then(() => {
    app.listen(PORT, () => {
      console.log(`Server running on port ${PORT}`);
    });
  })
  .catch((err) => {
    console.error('Failed to connect to DB:', err);
    process.exit(1);
  });
