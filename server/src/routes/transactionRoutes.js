const express = require("express");
const { requireAuth } = require("../middleware/auth");
const {
  handleDeposit,
  handleWithdraw,
  handleVerifyWithdrawal,
  handleTransfer,
  handleVerifyTransfer,
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

/**
 * POST /api/transactions/transfer
 * Internal or external transfer
 */
router.post("/transfer", handleTransfer);

/**
 * POST /api/transactions/initiate-transfer
 * Internal or external transfer initiation
 */
router.post("/initiate-transfer", handleTransfer);

/**
 * POST /api/transactions/transfer/verify
 * Verify OTP for a high-value transfer
 */
router.post("/transfer/verify", handleVerifyTransfer);

/**
 * POST /api/transactions/confirm-transfer
 * Verify OTP for a high-value transfer (alias)
 */
router.post("/confirm-transfer", handleVerifyTransfer);

module.exports = router;
