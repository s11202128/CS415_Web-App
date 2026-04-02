const express = require("express");
const { requireAuth } = require("../middleware/auth");
const {
  handleDeposit,
  handleWithdraw,
  handleVerifyWithdrawal,
} = require("../controllers/transactionController");

const router = express.Router();

// All transaction endpoints require authentication
router.use(requireAuth);

/**
 * POST /api/transactions/deposit
 * Deposit money into an account
 */
router.post("/deposit", handleDeposit);

/**
 * POST /api/transactions/withdraw
 * Withdraw money from an account (may require OTP if amount > 1000)
 */
router.post("/withdraw", handleWithdraw);

/**
 * POST /api/transactions/verify-withdrawal
 * Verify OTP for withdrawal
 */
router.post("/verify-withdrawal", handleVerifyWithdrawal);

module.exports = router;
