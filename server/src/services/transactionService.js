const { Account, Transaction, OtpVerification } = require("../models");
const { addNotification } = require("../store-mysql");
const sequelize = require("../config/database");
const crypto = require("crypto");

const OTP_EXPIRY_MINUTES = 5;
const OTP_MAX_ATTEMPTS = 3;
const TRANSFER_OTP_THRESHOLD = 1000;
const TRANSFER_DAILY_LIMIT = 10000;

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
async function withdraw({ accountId, amount, note }) {
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
    return await initiateWithdrawalOtp({ accountId, amount, account, note });
  }

  // Direct withdrawal (no OTP needed)
  return await completeWithdrawal({ accountId, amount, account, note });
}

/**
 * Initiate withdrawal with OTP verification for amounts > 1000
 */
async function initiateWithdrawalOtp({ accountId, amount, account, note }) {
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
        note: String(note || "").trim(),
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
async function completeWithdrawal({ accountId, amount, account, note }) {
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
    description: String(note || "").trim() || "Withdrawal",
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
  return await completeWithdrawal({ accountId, amount, account, note: safeParseMetadata(pending.metadata).note });
}

function resolveTransferMode(payload) {
  const explicitMode = String(payload.transferType || payload.mode || payload.transferMode || "").trim().toLowerCase();
  if (explicitMode === "external" || explicitMode === "other" || explicitMode === "other_accounts") {
    return "external";
  }
  return "internal";
}

function buildTransferNote(payload, transferMode) {
  const note = String(payload.note || payload.description || "").trim();
  if (note) {
    return note;
  }

  if (transferMode === "external") {
    return `Transfer to ${String(payload.recipientName || "external recipient").trim()}`;
  }

  return "Transfer";
}

async function completeTransferNow({
  fromAccountId,
  toAccountId,
  transferMode,
  amount,
  note,
  recipientName,
  bankName,
  accountNumber,
}) {
  let sourceCustomerId = null;
  let destinationCustomerId = null;
  let debitTransactionId = null;
  let creditTransactionId = null;
  let sourceBalanceAfter = null;

  await sequelize.transaction(async (dbTx) => {
    const sourceAccount = await Account.findByPk(fromAccountId, {
      transaction: dbTx,
      lock: dbTx.LOCK.UPDATE,
    });

    if (!sourceAccount) {
      throw new Error("Source account not found");
    }

    if (["frozen", "suspended", "closed"].includes(String(sourceAccount.status || "").toLowerCase())) {
      throw new Error("Source account is not available for transfers");
    }

    if (parseFloat(sourceAccount.balance) < amount) {
      throw new Error("Insufficient funds");
    }

    sourceBalanceAfter = Number((parseFloat(sourceAccount.balance) - amount).toFixed(2));
    await sourceAccount.update({ balance: sourceBalanceAfter }, { transaction: dbTx });

    sourceCustomerId = sourceAccount.customerId;

    const sourceDescription =
      transferMode === "external"
        ? `${note || `Transfer to ${recipientName || accountNumber || bankName || "external recipient"}`}`
        : `${note || `Transfer to account ${toAccountId}`}`;

    const debitTx = await Transaction.create(
      {
        accountId: sourceAccount.id,
        accountNumber: sourceAccount.accountNumber,
        type: "debit",
        amount,
        description: sourceDescription,
        status: "completed",
        balanceAfter: sourceBalanceAfter,
      },
      { transaction: dbTx }
    );
    debitTransactionId = debitTx.id;

    if (transferMode === "internal") {
      const destinationAccount = await Account.findByPk(toAccountId, {
        transaction: dbTx,
        lock: dbTx.LOCK.UPDATE,
      });

      if (!destinationAccount) {
        throw new Error("Destination account not found");
      }

      if (destinationAccount.id === sourceAccount.id) {
        throw new Error("Transfer accounts must be different");
      }

      const destinationBalanceAfter = Number((parseFloat(destinationAccount.balance) + amount).toFixed(2));
      await destinationAccount.update({ balance: destinationBalanceAfter }, { transaction: dbTx });

      const creditTx = await Transaction.create(
        {
          accountId: destinationAccount.id,
          accountNumber: destinationAccount.accountNumber,
          type: "credit",
          amount,
          description: note || `Transfer from account ${sourceAccount.id}`,
          status: "completed",
          balanceAfter: destinationBalanceAfter,
        },
        { transaction: dbTx }
      );
      creditTransactionId = creditTx.id;
      destinationCustomerId = destinationAccount.customerId;
    }
  });

  if (sourceCustomerId) {
    await addNotification(
      sourceCustomerId,
      transferMode === "external"
        ? `Transfer of FJD ${amount.toFixed(2)} to ${recipientName || accountNumber || "external recipient"} processed.`
        : `Transfer of FJD ${amount.toFixed(2)} from account ${fromAccountId} processed.`,
      "TRANSFER_SENT"
    );
  }

  if (transferMode === "internal" && destinationCustomerId) {
    await addNotification(
      destinationCustomerId,
      `You received FJD ${amount.toFixed(2)} into account ${toAccountId}.`,
      "MONEY_RECEIVED"
    );
  }

  return {
    success: true,
    message: "Transfer successful",
    requiresOtp: false,
    transferId: null,
    transactionId: debitTransactionId,
    creditTransactionId,
    balanceAfter: sourceBalanceAfter,
    amount: Number(amount),
  };
}

async function initiateBankTransfer(payload) {
  const amount = Number(payload.amount || 0);
  const fromAccountId = Number(payload.fromAccount || payload.fromAccountId || 0);
  const transferMode = resolveTransferMode(payload);
  const note = buildTransferNote(payload, transferMode);

  if (!Number.isFinite(fromAccountId) || fromAccountId <= 0) {
    throw new Error("From account is required");
  }
  if (!Number.isFinite(amount) || amount <= 0) {
    throw new Error("Amount must be greater than 0");
  }
  if (amount > TRANSFER_DAILY_LIMIT) {
    throw new Error("Amount exceeds daily transfer limit");
  }

  if (transferMode === "internal") {
    const toAccountId = Number(payload.toAccount || payload.toAccountId || 0);
    if (!Number.isFinite(toAccountId) || toAccountId <= 0) {
      throw new Error("Destination account is required for internal transfers");
    }

    if (amount > TRANSFER_OTP_THRESHOLD) {
      return await createTransferOtp({
        fromAccountId,
        transferMode,
        toAccountId,
        amount,
        note,
      });
    }

    return await completeTransferNow({ fromAccountId, toAccountId, transferMode, amount, note });
  }

  const recipientName = String(payload.recipientName || "").trim();
  const bankName = String(payload.bankName || "").trim();
  const accountNumber = String(payload.accountNumber || payload.toAccountNumber || "").trim();

  if (!recipientName) throw new Error("Recipient name is required for external transfers");
  if (!bankName) throw new Error("Bank name is required for external transfers");
  if (!accountNumber) throw new Error("Account number is required for external transfers");

  if (amount > TRANSFER_OTP_THRESHOLD) {
    return await createTransferOtp({
      fromAccountId,
      transferMode,
      amount,
      note,
      recipientName,
      bankName,
      accountNumber,
    });
  }

  return await completeTransferNow({
    fromAccountId,
    transferMode,
    amount,
    note,
    recipientName,
    bankName,
    accountNumber,
  });
}

async function createTransferOtp({ fromAccountId, transferMode, toAccountId, amount, note, recipientName, bankName, accountNumber }) {
  const transferId = `TRF-${Date.now()}-${Math.random().toString(36).substring(2, 8).toUpperCase()}`;
  const otp = makeSixDigitCode();
  const hashedOtp = hashOtp(otp);
  const sourceAccount = await Account.findByPk(fromAccountId);

  if (!sourceAccount) {
    throw new Error("Source account not found");
  }

  await OtpVerification.create({
    referenceCode: transferId,
    customerId: sourceAccount.customerId,
    otp: hashedOtp,
    transactionType: "transfer_money",
    amount,
    metadata: JSON.stringify({
      fromAccountId,
      transferMode,
      toAccountId: transferMode === "internal" ? toAccountId : null,
      recipientName: recipientName || null,
      bankName: bankName || null,
      accountNumber: accountNumber || null,
      note: note || "",
    }),
    expiresAt: new Date(Date.now() + OTP_EXPIRY_MINUTES * 60 * 1000),
    verified: false,
    attempts: 0,
    maxAttempts: OTP_MAX_ATTEMPTS,
  });

  await addNotification(
    sourceAccount.customerId,
    `OTP ${otp} for transfer of FJD ${amount.toFixed(2)} from account ${sourceAccount.id}.`,
    "OTP_VERIFICATION"
  );

  return {
    success: true,
    message: "OTP verification required",
    requiresOtp: true,
    transferId,
    otp,
    attemptsRemaining: OTP_MAX_ATTEMPTS,
  };
}

async function verifyBankTransferOtp({ transferId, otp }) {
  const pending = await OtpVerification.findOne({
    where: {
      referenceCode: transferId,
      transactionType: "transfer_money",
    },
  });

  if (!pending) {
    throw new Error("Pending transfer not found");
  }
  if (pending.verified) {
    throw new Error("Transfer already verified");
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
    await pending.update({ attempts: nextAttempts, lastAttemptAt: new Date() });
    const remaining = Math.max(Number(pending.maxAttempts || OTP_MAX_ATTEMPTS) - nextAttempts, 0);
    if (remaining <= 0) {
      throw new Error("OTP attempts exceeded");
    }
    throw new Error(`Invalid OTP. ${remaining} attempt(s) remaining.`);
  }

  const metadata = safeParseMetadata(pending.metadata);
  const result = await completeTransferNow({
    fromAccountId: metadata.fromAccountId,
    toAccountId: metadata.toAccountId,
    transferMode: metadata.transferMode,
    amount: Number(pending.amount),
    note: metadata.note,
    recipientName: metadata.recipientName,
    bankName: metadata.bankName,
    accountNumber: metadata.accountNumber,
  });

  await pending.update({ verified: true, attempts: Number(pending.attempts || 0) + 1, lastAttemptAt: new Date() });
  return {
    success: true,
    message: "Transfer verified successfully",
    transferId: pending.referenceCode,
    ...result,
  };
}

module.exports = {
  deposit,
  withdraw,
  verifyWithdrawalOtp,
  initiateBankTransfer,
  verifyBankTransferOtp,
};
