const { deposit, withdraw, verifyWithdrawalOtp } = require("../services/transactionService");

/**
 * POST /api/transactions/deposit
 * Body: { accountId: number, amount: number }
 */
async function handleDeposit(req, res) {
  try {
    const { accountId, amount, note } = req.body;

    // Validation
    if (!accountId || accountId <= 0) {
      return res.status(400).json({ success: false, message: "Invalid account ID" });
    }

    if (!amount || amount <= 0) {
      return res.status(400).json({ success: false, message: "Amount must be greater than 0" });
    }

    const numericAmount = Number(amount);
    if (!Number.isFinite(numericAmount)) {
      return res.status(400).json({ success: false, message: "Invalid amount format" });
    }

    // Call service
    const result = await deposit({ accountId, amount: numericAmount, note });

    res.status(200).json(result);
  } catch (error) {
    const statusCode = error.message.includes("not found") ? 404 : error.message.includes("Insufficient") ? 402 : 400;
    res.status(statusCode).json({
      success: false,
      message: error.message,
    });
  }
}

/**
 * POST /api/transactions/withdraw
 * Body: { accountId: number, amount: number }
 */
async function handleWithdraw(req, res) {
  try {
    const { accountId, amount } = req.body;

    // Validation
    if (!accountId || accountId <= 0) {
      return res.status(400).json({ success: false, message: "Invalid account ID" });
    }

    if (!amount || amount <= 0) {
      return res.status(400).json({ success: false, message: "Amount must be greater than 0" });
    }

    const numericAmount = Number(amount);
    if (!Number.isFinite(numericAmount)) {
      return res.status(400).json({ success: false, message: "Invalid amount format" });
    }

    // Call service
    const result = await withdraw({ accountId, amount: numericAmount });

    res.status(200).json(result);
  } catch (error) {
    const statusCode = error.message.includes("not found") ? 404 : error.message.includes("Insufficient") ? 402 : 400;
    res.status(statusCode).json({
      success: false,
      message: error.message,
    });
  }
}

/**
 * POST /api/transactions/verify-withdrawal
 * Body: { withdrawalId: string, otp: string }
 */
async function handleVerifyWithdrawal(req, res) {
  try {
    const { withdrawalId, otp } = req.body;

    // Validation
    if (!withdrawalId || !withdrawalId.trim()) {
      return res.status(400).json({ success: false, message: "Withdrawal ID is required" });
    }

    if (!otp || !otp.trim()) {
      return res.status(400).json({ success: false, message: "OTP is required" });
    }

    // Call service
    const result = await verifyWithdrawalOtp({ withdrawalId, otp });

    res.status(200).json(result);
  } catch (error) {
    const statusCode = error.message.includes("not found") ? 404 : error.message.includes("Insufficient") ? 402 : 400;
    res.status(statusCode).json({
      success: false,
      message: error.message,
    });
  }
}

module.exports = {
  handleDeposit,
  handleWithdraw,
  handleVerifyWithdrawal,
};
