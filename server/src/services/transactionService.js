const { Account, Transaction, OtpVerification } = require("../models");
const { addNotification } = require("../store-mysql");
const sequelize = require("../config/database");
const crypto = require("crypto");

const OTP_EXPIRY_MINUTES = 5;
const OTP_MAX_ATTEMPTS = 3;

function makeSixDigitCode() {
  return String(crypto.randomInt(0, 1000000)).padStart(6, "0");
}

function hashOtp(otp) {
  return crypto.createHash("sha256").update(String(otp || "")).digest("hex");
}

function safeParseMetadata(metadata) {
  if (!metadata) return {};
  if (typeof metadata === "string") {
    try {
      return JSON.parse(metadata);
    } catch (e) {
      return {};
    }
  }
  return metadata;
}

/**
 * Deposit money into an account
 * @param {number} accountId - Account ID
 * @param {number} amount - Amount to deposit
 * @returns {Promise<Object>} Transaction result with new balance
 */
async function deposit({ accountId, amount, note }) {
  if (!accountId || accountId <= 0) {
    throw new Error("Invalid account ID");
  }

  if (!amount || amount <= 0) {
    throw new Error("Amount must be greater than 0");
  }
  const trimmedNote = String(note || "").trim();
  let accountIdForNotification = null;
  let customerIdForNotification = null;
  let newBalanceForNotification = 0;
  const result = await sequelize.transaction(async (dbTx) => {
    // Lock account row so balance update + transaction write is atomic under concurrency.
    const account = await Account.findByPk(accountId, {
      transaction: dbTx,
      lock: dbTx.LOCK.UPDATE,
    });

    if (!account) {
      throw new Error("Account not found");
    }

    if (["frozen", "suspended", "closed"].includes(String(account.status || "").toLowerCase())) {
      throw new Error("Account is not available for deposits");
    }

    const newBalance = Number((parseFloat(account.balance) + amount).toFixed(2));

    await account.update({ balance: newBalance }, { transaction: dbTx });

    const txRecord = await Transaction.create(
      {
        accountId: account.id,
        accountNumber: account.accountNumber,
        type: "deposit",
        amount: Number(amount),
        description: trimmedNote || "Deposit",
        status: "completed",
        balanceAfter: newBalance,
      },
      { transaction: dbTx }
    );

    accountIdForNotification = account.id;
    customerIdForNotification = account.customerId;
    newBalanceForNotification = newBalance;

    return {
      success: true,
      message: "Deposit successful",
      transactionId: txRecord.id,
      balanceAfter: newBalance,
      amount: Number(amount),
    };
  });

  if (customerIdForNotification) {
    await addNotification(
      customerIdForNotification,
      `Deposit of FJD ${Number(amount).toFixed(2)} to account ${accountIdForNotification} completed. New balance: FJD ${newBalanceForNotification.toFixed(2)}.`,
      "DEPOSIT"
    );
  }

  return result;
}

/**
 * Withdraw money from an account
 * @param {number} accountId - Account ID
 * @param {number} amount - Amount to withdraw
 * @returns {Promise<Object>} Status and transfer ID if OTP required
 */
async function withdraw({ accountId, amount }) {
  if (!accountId || accountId <= 0) {
    throw new Error("Invalid account ID");
  }

  if (!amount || amount <= 0) {
    throw new Error("Amount must be greater than 0");
  }

  // Validate account exists
  const account = await Account.findByPk(accountId);
  if (!account) {
    throw new Error("Account not found");
  }

  if (["frozen", "suspended", "closed"].includes(String(account.status || "").toLowerCase())) {
    throw new Error("Account is not available for withdrawals");
  }

  // Check sufficient funds
  if (parseFloat(account.balance) < amount) {
    throw new Error("Insufficient funds");
  }

  // If withdrawal > 1000, require OTP
  if (amount > 1000) {
    return await initiateWithdrawalOtp({ accountId, amount, account });
  }

  // Direct withdrawal (no OTP needed)
  return await completeWithdrawal({ accountId, amount, account });
}

/**
 * Initiate withdrawal with OTP verification for amounts > 1000
 */
async function initiateWithdrawalOtp({ accountId, amount, account }) {
  const withdrawalId = `WTH-${Date.now()}-${Math.random().toString(36).substring(7).toUpperCase()}`;
  const otp = makeSixDigitCode();
  const hashedOtp = hashOtp(otp);

  // Store pending withdrawal
  await OtpVerification.create({
    referenceCode: withdrawalId,
    customerId: account.customerId,
    otp: hashedOtp,
    transactionType: "withdrawal",
    amount: Number(amount),
    metadata: JSON.stringify({
      accountId,
      amount: Number(amount),
    }),
    expiresAt: new Date(Date.now() + OTP_EXPIRY_MINUTES * 60 * 1000),
    verified: false,
    attempts: 0,
    maxAttempts: OTP_MAX_ATTEMPTS,
  });

  // Send OTP notification
  const customer = await account.getCustomer();
  if (customer) {
    await addNotification(
      customer.id,
      `OTP ${otp} for withdrawal of FJD ${Number(amount).toFixed(2)} from account ${account.id}. This OTP will expire in ${OTP_EXPIRY_MINUTES} minutes.`,
      "OTP_VERIFICATION"
    );
  }

  return {
    success: false,
    message: "OTP verification required",
    requiresOtp: true,
    withdrawalId,
    expiresInSeconds: OTP_EXPIRY_MINUTES * 60,
    attemptsRemaining: OTP_MAX_ATTEMPTS,
  };
}

/**
 * Complete withdrawal after OTP verification or for small amounts
 */
async function completeWithdrawal({ accountId, amount, account }) {
  // Calculate new balance
  const newBalance = Number((parseFloat(account.balance) - amount).toFixed(2));

  // Update account balance
  await account.update({ balance: newBalance });

  // Record transaction
  const transaction = await Transaction.create({
    accountId: account.id,
    accountNumber: account.accountNumber,
    type: "withdraw",
    amount: Number(amount),
    description: "Withdrawal",
    status: "completed",
    balanceAfter: newBalance,
  });

  // Send notification
  const customer = await account.getCustomer();
  if (customer) {
    await addNotification(
      customer.id,
      `Withdrawal of FJD ${Number(amount).toFixed(2)} from account ${account.id} completed. New balance: FJD ${newBalance.toFixed(2)}.`,
      "WITHDRAWAL"
    );
  }

  return {
    success: true,
    message: "Withdrawal successful",
    transactionId: transaction.id,
    balanceAfter: newBalance,
    amount: Number(amount),
  };
}

/**
 * Verify OTP and complete withdrawal
 */
async function verifyWithdrawalOtp({ withdrawalId, otp }) {
  const pending = await OtpVerification.findOne({
    where: {
      referenceCode: withdrawalId,
      transactionType: "withdrawal",
    },
  });

  if (!pending) {
    throw new Error("Pending withdrawal not found");
  }

  if (pending.verified) {
    throw new Error("Withdrawal already verified");
  }

  if (new Date(pending.expiresAt).getTime() < Date.now()) {
    throw new Error("OTP has expired");
  }

  if (Number(pending.attempts || 0) >= Number(pending.maxAttempts || OTP_MAX_ATTEMPTS)) {
    throw new Error("OTP attempts exceeded");
  }

  const submittedHash = hashOtp(String(otp || "").trim());
  if (pending.otp !== submittedHash) {
    const nextAttempts = Number(pending.attempts || 0) + 1;
    await pending.update({
      attempts: nextAttempts,
      lastAttemptAt: new Date(),
    });

    const remaining = Math.max(Number(pending.maxAttempts || OTP_MAX_ATTEMPTS) - nextAttempts, 0);
    if (remaining <= 0) {
      throw new Error("OTP attempts exceeded");
    }
    throw new Error(`Invalid OTP. ${remaining} attempt(s) remaining.`);
  }

  // OTP verified, mark as verified
  await pending.update({ verified: true });

  // Get account and complete withdrawal
  const metadata = safeParseMetadata(pending.metadata);
  const accountId = metadata.accountId;
  const amount = Number(pending.amount);

  const account = await Account.findByPk(accountId);
  if (!account) {
    throw new Error("Account not found");
  }

  // Check balance again
  if (parseFloat(account.balance) < amount) {
    throw new Error("Insufficient funds (balance changed)");
  }

  // Complete withdrawal
  return await completeWithdrawal({ accountId, amount, account });
}

module.exports = {
  deposit,
  withdraw,
  verifyWithdrawalOtp,
};
