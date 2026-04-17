const express = require('express');
const router = express.Router();
const transactionController = require('../controllers/transactionController');
const authMiddleware = require('../middleware/authMiddleware');

// Request transaction OTP (protected)
router.post('/request-otp', authMiddleware, transactionController.requestTransactionOTP);

// Confirm transaction (protected)
router.post('/confirm', authMiddleware, transactionController.confirmTransaction);

module.exports = router;
