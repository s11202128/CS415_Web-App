const { v4: uuid } = require("uuid");
const bcrypt = require("bcryptjs");

const HIGH_VALUE_OTP_THRESHOLD = 1000;
const WITHHOLDING_TAX_RATE = 0.15;

const db = {
  config: {
    reserveBankMinSavingsInterestRate: 0.0325,
  },
  customers: [
    {
      id: "CUST-001",
      fullName: "Litia Naivalu",
      mobile: "+6797001001",
      residencyStatus: "resident",
      tin: "TIN1001",
    },
    {
      id: "CUST-002",
      fullName: "Aman Patel",
      mobile: "+6797002002",
      residencyStatus: "non-resident",
      tin: "",
    },
  ],
  accounts: [
    {
      id: "ACC-001",
      customerId: "CUST-001",
      type: "Simple Access",
      balance: 4200,
      maintenanceFee: 2.5,
      currency: "FJD",
      createdAt: "2026-01-10T00:00:00.000Z",
    },
    {
      id: "ACC-002",
      customerId: "CUST-001",
      type: "Savings",
      balance: 11200,
      maintenanceFee: 0,
      currency: "FJD",
      createdAt: "2026-01-10T00:00:00.000Z",
    },
    {
      id: "ACC-003",
      customerId: "CUST-002",
      type: "Savings",
      balance: 6800,
      maintenanceFee: 0,
      currency: "FJD",
      createdAt: "2026-01-15T00:00:00.000Z",
    },
  ],
  transactions: [],
  pendingTransfers: [],
  billPayments: [],
  scheduledBillPayments: [],
  investments: [],
  statements: [],
  notifications: [],
  loanProducts: [
    {
      id: "LP-001",
      name: "Personal Loan",
      annualRate: 0.089,
      maxAmount: 30000,
      minTermMonths: 6,
      maxTermMonths: 60,
    },
    {
      id: "LP-002",
      name: "Home Loan",
      annualRate: 0.061,
      maxAmount: 450000,
      minTermMonths: 60,
      maxTermMonths: 360,
    },
    {
      id: "LP-003",
      name: "Vehicle Loan",
      annualRate: 0.074,
      maxAmount: 90000,
      minTermMonths: 12,
      maxTermMonths: 84,
    },
  ],
  loanApplications: [],
  interestSummaries: [],
  users: [],
};

function nowIso() {
  return new Date().toISOString();
}

function makeId(prefix) {
  return `${prefix}-${uuid().slice(0, 8).toUpperCase()}`;
}

function getCustomer(customerId) {
  return db.customers.find((c) => c.id === customerId);
}

function getAccount(accountId) {
  return db.accounts.find((a) => a.id === accountId);
}

function addNotification(customerId, message, type = "SMS") {
  const row = {
    id: makeId("NTF"),
    customerId,
    type,
    message,
    createdAt: nowIso(),
  };
  db.notifications.push(row);
  return row;
}

function createTransaction({ accountId, kind, amount, description, counterpartyAccountId, metadata = {} }) {
  const account = getAccount(accountId);
  if (!account) {
    throw new Error("Account not found");
  }

  const signedAmount = kind === "debit" ? -Math.abs(amount) : Math.abs(amount);
  account.balance = Number((account.balance + signedAmount).toFixed(2));

  const tx = {
    id: makeId("TXN"),
    accountId,
    kind,
    amount: Math.abs(amount),
    signedAmount,
    description,
    counterpartyAccountId: counterpartyAccountId || null,
    metadata,
    createdAt: nowIso(),
  };
  db.transactions.push(tx);

  db.statements.push({
    id: makeId("STM"),
    accountId,
    transactionId: tx.id,
    createdAt: tx.createdAt,
    line: `${tx.createdAt} | ${tx.kind.toUpperCase()} | FJD ${tx.amount.toFixed(2)} | ${tx.description}`,
  });

  return tx;
}

function getAccountTransactions(accountId) {
  return db.transactions
    .filter((t) => t.accountId === accountId)
    .sort((a, b) => (a.createdAt < b.createdAt ? 1 : -1));
}

function transfer({ fromAccountId, toAccountId, amount, description }) {
  if (amount <= 0) {
    throw new Error("Amount must be greater than 0");
  }

  const fromAccount = getAccount(fromAccountId);
  const toAccount = getAccount(toAccountId);

  if (!fromAccount || !toAccount) {
    throw new Error("Both accounts must exist");
  }
  if (fromAccount.id === toAccount.id) {
    throw new Error("Transfer accounts must be different");
  }
  if (fromAccount.balance < amount) {
    throw new Error("Insufficient funds");
  }

  const debitTx = createTransaction({
    accountId: fromAccountId,
    kind: "debit",
    amount,
    description: description || "Transfer sent",
    counterpartyAccountId: toAccountId,
  });
  const creditTx = createTransaction({
    accountId: toAccountId,
    kind: "credit",
    amount,
    description: description || "Transfer received",
    counterpartyAccountId: fromAccountId,
  });
  
  const fromCustomer = getCustomer(fromAccount.customerId);
  const toCustomer = getCustomer(toAccount.customerId);

  if (toCustomer) {
    addNotification(toCustomer.id, `You received FJD ${amount.toFixed(2)} into account ${toAccount.id}.`);
  }
  if (fromCustomer) {
    addNotification(fromCustomer.id, `Transfer of FJD ${amount.toFixed(2)} from account ${fromAccount.id} processed.`);
  }

  return { debitTx, creditTx };
}

function initiateTransfer(payload) {
  const amount = Number(payload.amount || 0);
  const requiresOtp = amount >= HIGH_VALUE_OTP_THRESHOLD;

  if (!requiresOtp) {
    const result = transfer({
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

  db.pendingTransfers.push({
    id: transferId,
    fromAccountId: payload.fromAccountId,
    toAccountId: payload.toAccountId,
    amount,
    description: payload.description || "High value transfer",
    otp,
    verified: false,
    createdAt: nowIso(),
  });

  const fromAccount = getAccount(payload.fromAccountId);
  if (fromAccount) {
    addNotification(
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

function verifyTransfer({ transferId, otp }) {
  const pending = db.pendingTransfers.find((p) => p.id === transferId);
  if (!pending) {
    throw new Error("Pending transfer not found");
  }
  if (pending.verified) {
    throw new Error("Transfer already verified");
  }
  if (pending.otp !== String(otp)) {
    throw new Error("Invalid OTP");
  }

  pending.verified = true;
  const result = transfer({
    fromAccountId: pending.fromAccountId,
    toAccountId: pending.toAccountId,
    amount: pending.amount,
    description: pending.description,
  });

  return { status: "completed", transferId: pending.id, result };
}

function postBillPayment({ accountId, payee, amount, mode, scheduledDate }) {
  const account = getAccount(accountId);
  if (!account) {
    throw new Error("Account not found");
  }
  if (amount <= 0) {
    throw new Error("Amount must be greater than 0");
  }

  const payment = {
    id: makeId("BILL"),
    accountId,
    payee,
    amount,
    mode,
    status: "processed",
    scheduledDate: scheduledDate || null,
    createdAt: nowIso(),
  };

  createTransaction({
    accountId,
    kind: "debit",
    amount,
    description: `Bill payment to ${payee}`,
    metadata: { paymentId: payment.id, mode },
  });

  db.billPayments.push(payment);
  addNotification(account.customerId, `Bill payment of FJD ${amount.toFixed(2)} to ${payee} processed.`);
  return payment;
}

function scheduleBillPayment({ accountId, payee, amount, scheduledDate }) {
  const account = getAccount(accountId);
  if (!account) {
    throw new Error("Account not found");
  }

  const row = {
    id: makeId("SBP"),
    accountId,
    payee,
    amount,
    scheduledDate,
    status: "scheduled",
    createdAt: nowIso(),
  };
  db.scheduledBillPayments.push(row);
  return row;
}

function runScheduledPayment(id) {
  const scheduled = db.scheduledBillPayments.find((x) => x.id === id);
  if (!scheduled) {
    throw new Error("Scheduled payment not found");
  }
  if (scheduled.status === "processed") {
    throw new Error("Scheduled payment already processed");
  }

  const payment = postBillPayment({
    accountId: scheduled.accountId,
    payee: scheduled.payee,
    amount: scheduled.amount,
    mode: "scheduled",
    scheduledDate: scheduled.scheduledDate,
  });

  scheduled.status = "processed";
  scheduled.processedAt = nowIso();
  return { scheduled, payment };
}

function createInvestment({ customerId, name, amount, annualRate }) {
  if (!getCustomer(customerId)) {
    throw new Error("Customer not found");
  }
  const row = {
    id: makeId("INV"),
    customerId,
    name,
    amount,
    annualRate,
    createdAt: nowIso(),
  };
  db.investments.push(row);
  return row;
}

function generateStatement(accountId, from, to) {
  const fromDate = from ? new Date(from) : null;
  const toDate = to ? new Date(to) : null;

  return db.transactions
    .filter((t) => t.accountId === accountId)
    .filter((t) => {
      const d = new Date(t.createdAt);
      if (fromDate && d < fromDate) return false;
      if (toDate && d > toDate) return false;
      return true;
    })
    .sort((a, b) => (a.createdAt > b.createdAt ? 1 : -1));
}

function calculateInterestForAccount(account, year) {
  if (account.type !== "Savings") {
    return 0;
  }
  const rate = db.config.reserveBankMinSavingsInterestRate;
  const baseInterest = account.balance * rate;
  // Prototype simplification: no daily compounding yet.
  return Number(baseInterest.toFixed(2));
}

function generateInterestSummaries(year) {
  const summaries = db.accounts.map((account) => {
    const customer = getCustomer(account.customerId);
    const grossInterest = calculateInterestForAccount(account, year);
    const taxableAsWithholding = !customer.tin || customer.residencyStatus === "non-resident";
    const withholdingTax = taxableAsWithholding ? Number((grossInterest * WITHHOLDING_TAX_RATE).toFixed(2)) : 0;
    const netInterest = Number((grossInterest - withholdingTax).toFixed(2));

    return {
      id: makeId("INT"),
      year,
      accountId: account.id,
      customerId: customer.id,
      customerName: customer.fullName,
      residencyStatus: customer.residencyStatus,
      tin: customer.tin || null,
      grossInterest,
      withholdingTax,
      netInterest,
      status: "submitted_to_frcs",
      submittedAt: nowIso(),
    };
  });

  db.interestSummaries = summaries;
  return summaries;
}

function applyMonthlyFees() {
  const charged = [];
  db.accounts.forEach((account) => {
    if (account.maintenanceFee > 0) {
      createTransaction({
        accountId: account.id,
        kind: "debit",
        amount: account.maintenanceFee,
        description: "Monthly maintenance fee",
      });
      charged.push(account.id);
    }
  });
  return charged;
}

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
  if (db.users.find((u) => u.email.toLowerCase() === email.toLowerCase())) {
    throw new Error("Email already registered");
  }

  const passwordHash = await bcrypt.hash(password, 10);
  const userId = makeId("USR");
  const user = {
    id: userId,
    fullName,
    mobile,
    email: email.toLowerCase(),
    passwordHash,
    createdAt: nowIso(),
  };
  db.users.push(user);

  const customerId = `CUST-${String(db.customers.length + 1).padStart(3, "0")}`;
  const customer = { id: customerId, fullName, mobile, residencyStatus: "resident", tin: "" };
  db.customers.push(customer);

  return { userId, customerId, fullName, email: user.email };
}

async function loginUser({ email, password }) {
  if (!email || !password) {
    throw new Error("email and password are required");
  }
  const user = db.users.find((u) => u.email === email.toLowerCase());
  if (!user) {
    throw new Error("Invalid email or password");
  }
  const valid = await bcrypt.compare(password, user.passwordHash);
  if (!valid) {
    throw new Error("Invalid email or password");
  }
  const customer = db.customers.find((c) => c.mobile === user.mobile);
  return { userId: user.id, fullName: user.fullName, email: user.email, customerId: customer?.id };
}

module.exports = {
  db,
  HIGH_VALUE_OTP_THRESHOLD,
  WITHHOLDING_TAX_RATE,
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
  registerUser,
  loginUser,
};
