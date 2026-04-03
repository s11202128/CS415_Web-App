const { deposit, withdraw, verifyWithdrawalOtp, initiateBankTransfer, verifyBankTransferOtp } = require("../services/transactionService");
const { Account, OtpVerification } = require("../models");

function isAdmin(req) {
  return Boolean(req.auth?.isAdmin);
}

function getAuthenticatedCustomerId(req) {
  return Number(req.auth?.customerId || 0);
}

function canAccessCustomer(req, customerId) {
  return isAdmin(req) || Number(customerId) === getAuthenticatedCustomerId(req);
}

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

    const account = await Account.findByPk(Number(accountId));
    if (!account) {
      return res.status(404).json({ success: false, message: "Account not found" });
    }
    if (!canAccessCustomer(req, account.customerId)) {
      return res.status(403).json({ success: false, message: "Forbidden" });
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

    const account = await Account.findByPk(Number(accountId));
    if (!account) {
      return res.status(404).json({ success: false, message: "Account not found" });
    }
    if (!canAccessCustomer(req, account.customerId)) {
      return res.status(403).json({ success: false, message: "Forbidden" });
    }

    // Call service
    const result = await withdraw({ accountId, amount: numericAmount, note });

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

    const pending = await OtpVerification.findOne({
      where: {
        referenceCode: String(withdrawalId).trim(),
        transactionType: "withdrawal",
      },
    });
    if (!pending) {
      return res.status(404).json({ success: false, message: "Pending withdrawal not found" });
    }
    if (!canAccessCustomer(req, pending.customerId)) {
      return res.status(403).json({ success: false, message: "Forbidden" });
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

/**
 * POST /api/transactions/transfer
 * Body: internal { fromAccount, toAccount, amount, note? }
 * Body: external { fromAccount, recipientName, bankName, accountNumber, amount, note? }
 */
async function handleTransfer(req, res) {
  try {
    const payload = req.body || {};
    const fromAccountId = Number(payload.fromAccount || payload.fromAccountId || 0);
    if (!Number.isFinite(fromAccountId) || fromAccountId <= 0) {
      return res.status(400).json({ success: false, message: "From account is required" });
    }

    const fromAccount = await Account.findByPk(fromAccountId);
    if (!fromAccount) {
      return res.status(404).json({ success: false, message: "Source account not found" });
    }
    if (!canAccessCustomer(req, fromAccount.customerId)) {
      return res.status(403).json({ success: false, message: "Forbidden" });
    }

    const result = await initiateBankTransfer(payload);
    res.status(200).json(result);
  } catch (error) {
    const statusCode = error.message.includes("not found") ? 404 : error.message.includes("Insufficient") ? 402 : 400;
    res.status(statusCode).json({ success: false, message: error.message });
  }
}

/**
 * POST /api/transactions/transfer/verify
 * Body: { transferId: string, otp: string }
 */
async function handleVerifyTransfer(req, res) {
  try {
    const { transferId, otp } = req.body || {};
    if (!transferId || !String(transferId).trim()) {
      return res.status(400).json({ success: false, message: "Transfer ID is required" });
    }
    if (!otp || !String(otp).trim()) {
      return res.status(400).json({ success: false, message: "OTP is required" });
    }

    const pending = await OtpVerification.findOne({
      where: {
        referenceCode: String(transferId).trim(),
        transactionType: "transfer_money",
      },
    });
    if (!pending) {
      return res.status(404).json({ success: false, message: "Pending transfer not found" });
    }
    if (!canAccessCustomer(req, pending.customerId)) {
      return res.status(403).json({ success: false, message: "Forbidden" });
    }

    const result = await verifyBankTransferOtp({ transferId, otp });
    res.status(200).json(result);
  } catch (error) {
    const statusCode = error.message.includes("not found") ? 404 : error.message.includes("Insufficient") ? 402 : 400;
    res.status(statusCode).json({ success: false, message: error.message });
  }
}

module.exports = {
  handleDeposit,
  handleWithdraw,
  handleVerifyWithdrawal,
  handleTransfer,
  handleVerifyTransfer,
};
