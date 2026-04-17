const express = require('express');
const router = express.Router();
const authController = require('../controllers/authController');

// Signup
router.post('/signup', authController.signup);

// Verify signup OTP
router.post('/verify-signup', authController.verifySignup);

// Login
router.post('/login', authController.login);

// Verify login OTP
router.post('/verify-login', authController.verifyLogin);

module.exports = router;
