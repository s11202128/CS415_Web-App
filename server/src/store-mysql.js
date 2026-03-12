const { v4: uuid } = require("uuid");
const bcrypt = require("bcryptjs");
const { Op } = require("sequelize");
const {
  Customer,
  Account,
  Transaction,
  Bill,
  Investment,
  OtpVerification,
  Registration,
} = require("./models");

let HIGH_VALUE_OTP_THRESHOLD = 1000;
const WITHHOLDING_TAX_RATE = 0.15;
const SAVINGS_INTEREST_RATE = 0.0325;
const ADMIN_EMAIL = String(process.env.ADMIN_EMAIL || "admin@bof.fj").toLowerCase();
const ADMIN_PASSWORD = String(process.env.ADMIN_PASSWORD || "admin12345");
const notificationLogs = [];

function nowIso() {
  return new Date().toISOString();
}

function makeId(prefix) {
  return `${prefix}-${uuid().slice(0, 8).toUpperCase()}`;
}

function createRandom12DigitNumber() {
  let value = "";
  for (let i = 0; i < 12; i += 1) {
    value += Math.floor(Math.random() * 10);
  }
  return value;
}

async function generateRandomAccountNumber() {
  const maxAttempts = 20;
  for (let i = 0; i < maxAttempts; i += 1) {
    const accountNumber = createRandom12DigitNumber();
    const existing = await Account.findOne({ where: { accountNumber } });
    if (!existing) {
      return accountNumber;
    }
  }
  throw new Error("Unable to generate unique account number. Please try again.");
}

// Get customer by ID
async function getCustomer(customerId) {
  return await Customer.findByPk(customerId);
}

// Get account by ID
async function getAccount(accountId) {
  return await Account.findByPk(accountId);
}

// Add notification (for now, just log it)
async function addNotification(customerId, message, type = "SMS") {
  console.log(`[${type}] Customer ${customerId}: ${message}`);
  const logEntry = {
    id: makeId("NTF"),
    customerId,
    type,
    message,
    createdAt: nowIso(),
  };
  notificationLogs.unshift(logEntry);
  if (notificationLogs.length > 1000) {
    notificationLogs.pop();
  }
  // In future, store notifications in a Notifications table
  return logEntry;
}

function getHighValueTransferThreshold() {
  return HIGH_VALUE_OTP_THRESHOLD;
}

function setHighValueTransferThreshold(value) {
  const threshold = Number(value);
  if (!Number.isFinite(threshold) || threshold <= 0) {
    throw new Error("High-value transfer limit must be a positive number");
  }
  HIGH_VALUE_OTP_THRESHOLD = threshold;
  return HIGH_VALUE_OTP_THRESHOLD;
}

function getNotificationLogs(limit = 200) {
  return notificationLogs.slice(0, Number(limit) || 200);
}

// Create a transaction in the database
async function createTransaction({ accountId, kind, amount, description, counterpartyAccountId, metadata = {} }) {
  const account = await getAccount(accountId);
  if (!account) {
    throw new Error("Account not found");
  }

  const signedAmount = kind === "debit" ? -Math.abs(amount) : Math.abs(amount);
  const newBalance = Number((parseFloat(account.balance) + signedAmount).toFixed(2));

  // Update account balance
  await account.update({ balance: newBalance });

  // Create transaction record
  const tx = await Transaction.create({
    accountId,
    type: kind,
    amount: Math.abs(amount),
    description,
    status: "completed",
    balanceAfter: newBalance,
  });

  return {
    id: tx.id,
    accountId,
    kind,
    amount: Math.abs(amount),
    signedAmount,
    description,
    counterpartyAccountId: counterpartyAccountId || null,
    metadata,
    createdAt: tx.createdAt,
  };
}

// Get all transactions for an account
async function getAccountTransactions(accountId) {
  const transactions = await Transaction.findAll({
    where: { accountId },
    order: [["createdAt", "DESC"]],
  });
  return transactions;
}

// Perform a transfer between two accounts
async function transfer({ fromAccountId, toAccountId, amount, description }) {
  if (amount <= 0) {
    throw new Error("Amount must be greater than 0");
  }

  const fromAccount = await getAccount(fromAccountId);
  const toAccount = await getAccount(toAccountId);

  if (!fromAccount || !toAccount) {
    throw new Error("Both accounts must exist");
  }
  if (fromAccount.id === toAccount.id) {
    throw new Error("Transfer accounts must be different");
  }
  if (parseFloat(fromAccount.balance) < amount) {
    throw new Error("Insufficient funds");
  }

  const debitTx = await createTransaction({
    accountId: fromAccountId,
    kind: "debit",
    amount,
    description: description || "Transfer sent",
    counterpartyAccountId: toAccountId,
  });

  const creditTx = await createTransaction({
    accountId: toAccountId,
    kind: "credit",
    amount,
    description: description || "Transfer received",
    counterpartyAccountId: fromAccountId,
  });

  const fromCustomer = await getCustomer(fromAccount.customerId);
  const toCustomer = await getCustomer(toAccount.customerId);

  if (toCustomer) {
    await addNotification(toCustomer.id, `You received FJD ${amount.toFixed(2)} into account ${toAccount.id}.`);
  }
  if (fromCustomer) {
    await addNotification(fromCustomer.id, `Transfer of FJD ${amount.toFixed(2)} from account ${fromAccount.id} processed.`);
  }

  return { debitTx, creditTx };
}

// Initiate a transfer (may require OTP)
async function initiateTransfer(payload) {
  const amount = Number(payload.amount || 0);
  const requiresOtp = amount >= HIGH_VALUE_OTP_THRESHOLD;

  if (!requiresOtp) {
    const result = await transfer({
      fromAccountId: payload.fromAccountId,
      toAccountId: payload.toAccountId,
      amount,
      description: payload.description,
    });
    return {
      status: "completed",
      requiresOtp: false,
      transferId: null,
      otp: null,
      result,
    };
  }

  const transferId = makeId("TRF");
  const otp = `${Math.floor(100000 + Math.random() * 900000)}`;

  // Store pending transfer as OtpVerification record
  await OtpVerification.create({
    id: transferId,
    customerId: (await getAccount(payload.fromAccountId)).customerId,
    otp,
    transactionType: "transfer",
    amount,
    expiresAt: new Date(Date.now() + 5 * 60 * 1000), // 5 minutes
    verified: false,
  });

  const fromAccount = await getAccount(payload.fromAccountId);
  if (fromAccount) {
    await addNotification(
      fromAccount.customerId,
      `OTP ${otp} for high-value transfer of FJD ${amount.toFixed(2)} from account ${fromAccount.id}.`
    );
  }

  return {
    status: "pending_verification",
    requiresOtp: true,
    transferId,
    otp,
  };
}

// Verify and complete a transfer using OTP
async function verifyTransfer({ transferId, otp }) {
  const pending = await OtpVerification.findByPk(transferId);
  if (!pending) {
    throw new Error("Pending transfer not found");
  }
  if (pending.verified) {
    throw new Error("Transfer already verified");
  }
  if (pending.otp !== String(otp)) {
    throw new Error("Invalid OTP");
  }

  await pending.update({ verified: true });

  // For now, we'll just return success. Complete transfer logic would go here.
  return { status: "completed", transferId: pending.id };
}

// Post a bill payment
async function postBillPayment({ accountId, payee, amount, mode, scheduledDate }) {
  const account = await getAccount(accountId);
  if (!account) {
    throw new Error("Account not found");
  }
  if (amount <= 0) {
    throw new Error("Amount must be greater than 0");
  }

  await createTransaction({
    accountId,
    kind: "debit",
    amount,
    description: `Bill payment to ${payee}`,
    metadata: { mode },
  });

  const payment = await Bill.create({
    customerId: account.customerId,
    billType: payee,
    amount,
    status: "paid",
    description: `Payment via ${mode}`,
    dueDate: scheduledDate || null,
  });

  await addNotification(account.customerId, `Bill payment of FJD ${amount.toFixed(2)} to ${payee} processed.`);
  return {
    id: payment.id,
    accountId,
    payee,
    amount,
    mode,
    status: "processed",
    createdAt: payment.createdAt,
  };
}

// Schedule a bill payment
async function scheduleBillPayment({ accountId, payee, amount, scheduledDate }) {
  const account = await getAccount(accountId);
  if (!account) {
    throw new Error("Account not found");
  }

  const bill = await Bill.create({
    customerId: account.customerId,
    billType: payee,
    amount,
    status: "scheduled",
    dueDate: new Date(scheduledDate),
  });

  return {
    id: bill.id,
    accountId,
    payee,
    amount,
    scheduledDate,
    status: "scheduled",
    createdAt: bill.createdAt,
  };
}

// Run a scheduled bill payment
async function runScheduledPayment(id) {
  const scheduled = await Bill.findByPk(id);
  if (!scheduled) {
    throw new Error("Scheduled payment not found");
  }
  if (scheduled.status === "paid") {
    throw new Error("Scheduled payment already processed");
  }

  // Find the account for this customer
  const account = await Account.findOne({
    where: { customerId: scheduled.customerId },
  });

  if (!account) {
    throw new Error("Account not found");
  }

  const payment = await postBillPayment({
    accountId: account.id,
    payee: scheduled.billType,
    amount: scheduled.amount,
    mode: "scheduled",
    scheduledDate: scheduled.dueDate,
  });

  await scheduled.update({ status: "paid" });
  return { scheduled, payment };
}

// Create an investment
async function createInvestment({ customerId, name, amount, annualRate }) {
  const customer = await getCustomer(customerId);
  if (!customer) {
    throw new Error("Customer not found");
  }

  const investment = await Investment.create({
    customerId,
    investmentType: name,
    amount,
    expectedReturn: annualRate,
    status: "active",
  });

  return {
    id: investment.id,
    customerId,
    name,
    amount,
    annualRate,
    createdAt: investment.createdAt,
  };
}

// Generate statement for an account
async function generateStatement(accountId, from, to) {
  const fromDate = from ? new Date(from) : null;
  const toDate = to ? new Date(to) : null;

  const where = { accountId };
  if (fromDate || toDate) {
    if (fromDate && toDate) {
      where.createdAt = { [Op.between]: [fromDate, toDate] };
    } else if (fromDate) {
      where.createdAt = { [Op.gte]: fromDate };
    } else if (toDate) {
      where.createdAt = { [Op.lte]: toDate };
    }
  }

  const transactions = await Transaction.findAll({
    where,
    order: [["createdAt", "ASC"]],
  });

  return transactions;
}

// Calculate interest for a savings account
async function calculateInterestForAccount(account) {
  if (account.accountType !== "Savings") {
    return 0;
  }
  const rate = SAVINGS_INTEREST_RATE;
  const baseInterest = parseFloat(account.balance) * rate;
  return Number(baseInterest.toFixed(2));
}

// Generate interest summaries for all accounts
async function generateInterestSummaries(year) {
  const accounts = await Account.findAll({
    include: { association: "Customer", attributes: ["id", "fullName"] },
  });

  const summaries = [];
  for (const account of accounts) {
    const grossInterest = await calculateInterestForAccount(account);
    const withholdingTax = Number((grossInterest * WITHHOLDING_TAX_RATE).toFixed(2));
    const netInterest = Number((grossInterest - withholdingTax).toFixed(2));

    summaries.push({
      id: makeId("INT"),
      year,
      accountId: account.id,
      customerId: account.customerId,
      customerName: account.Customer.fullName,
      residencyStatus: "resident", // Would be from customer table in future
      tin: null,
      grossInterest,
      withholdingTax,
      netInterest,
      status: "submitted_to_frcs",
      submittedAt: nowIso(),
    });
  }

  return summaries;
}

// Apply monthly fees to accounts
async function applyMonthlyFees() {
  const charged = [];
  // For now, no monthly fees in new setup
  // This can be extended if we track account types with fees
  return charged;
}

// Register a new user
async function registerUser({ fullName, mobile, email, password }) {
  if (!fullName || !mobile || !email || !password) {
    throw new Error("fullName, mobile, email and password are required");
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    throw new Error("Invalid email format");
  }
  if (!/^\+?\d{7,15}$/.test(mobile.replace(/[\s\-]/g, ""))) {
    throw new Error("Invalid mobile number — use digits only, optionally starting with +");
  }
  if (password.length < 8) {
    throw new Error("Password must be at least 8 characters");
  }

  const existingUser = await Customer.findOne({
    where: { email: email.toLowerCase() },
  });
  if (existingUser) {
    throw new Error("Email already registered");
  }

  const passwordHash = await bcrypt.hash(password, 10);

  await Registration.create({
    fullName,
    mobile,
    email: email.toLowerCase(),
    password: passwordHash,
  });

  const customer = await Customer.create({
    fullName,
    mobile,
    email: email.toLowerCase(),
    password: passwordHash,
    status: "active",
  });

  // Create a default account for the new customer
  const accountNumber = await generateRandomAccountNumber();
  await Account.create({
    customerId: customer.id,
    accountNumber,
    accountType: "Savings",
    balance: 0,
    currency: "FJD",
    status: "active",
  });

  return { userId: customer.id, customerId: customer.id, fullName, email: customer.email };
}

// Login a user
async function loginUser({ email, password }) {
  if (!email || !password) {
    throw new Error("email and password are required");
  }

  if (email.toLowerCase() === ADMIN_EMAIL && password === ADMIN_PASSWORD) {
    return {
      userId: "admin",
      fullName: "System Admin",
      email: ADMIN_EMAIL,
      customerId: null,
      isAdmin: true,
    };
  }

  const customer = await Customer.findOne({
    where: { email: email.toLowerCase() },
  });

  if (!customer) {
    throw new Error("Invalid email or password");
  }

  const valid = await bcrypt.compare(password, customer.password);
  if (!valid) {
    throw new Error("Invalid email or password");
  }

  return {
    userId: customer.id,
    fullName: customer.fullName,
    email: customer.email,
    customerId: customer.id,
    isAdmin: false,
  };
}

async function verifyAdminCredentials({ email, password }) {
  if (!email || !password) {
    throw new Error("email and password are required");
  }

  const normalizedEmail = String(email).toLowerCase();
  if (normalizedEmail !== ADMIN_EMAIL || password !== ADMIN_PASSWORD) {
    throw new Error("Invalid admin email or password");
  }

  return {
    email: ADMIN_EMAIL,
    fullName: "System Admin",
    isAdmin: true,
  };
}

module.exports = {
  HIGH_VALUE_OTP_THRESHOLD,
  WITHHOLDING_TAX_RATE,
  SAVINGS_INTEREST_RATE,
  getCustomer,
  getAccount,
  getAccountTransactions,
  addNotification,
  createTransaction,
  initiateTransfer,
  verifyTransfer,
  postBillPayment,
  scheduleBillPayment,
  runScheduledPayment,
  createInvestment,
  generateStatement,
  generateInterestSummaries,
  applyMonthlyFees,
  getHighValueTransferThreshold,
  setHighValueTransferThreshold,
  getNotificationLogs,
  generateRandomAccountNumber,
  registerUser,
  loginUser,
  verifyAdminCredentials,
};
