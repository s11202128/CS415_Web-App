const express = require("express");
const { Op } = require("sequelize");
const requirementsData = require("../config/requirementsData");
const { requireAuth, requireAdmin } = require("../middleware/auth");
const {
  getAccountTransactions,
  initiateTransfer,
  verifyTransfer,
  postBillPayment,
  scheduleBillPayment,
  runScheduledPayment,
  createInvestment,
  generateRandomAccountNumber,
  generateStatement,
  generateInterestSummaries,
  applyMonthlyFees,
  getHighValueTransferThreshold,
  setHighValueTransferThreshold,
  getNotificationLogs,
  addNotification,
  getDashboard,
  updateProfile,
  getLoginLogs,
  getOtpAttempts,
  reverseTransaction,
} = require("../store-mysql");
const { Customer, Account, Bill, Investment, Loan, Transaction, OtpVerification } = require("../models");

const loanProducts = [
  { id: "LP-001", name: "Personal Loan", annualRate: 0.089, maxAmount: 30000, minTermMonths: 6, maxTermMonths: 60 },
  { id: "LP-002", name: "Home Loan", annualRate: 0.061, maxAmount: 450000, minTermMonths: 60, maxTermMonths: 360 },
  { id: "LP-003", name: "Vehicle Loan", annualRate: 0.074, maxAmount: 90000, minTermMonths: 12, maxTermMonths: 84 },
];

let reserveBankMinSavingsInterestRate = 0.0325;

const router = express.Router();

const asyncHandler = (fn) => async (req, res) => {
  try {
    await fn(req, res);
  } catch (error) {
    res.status(400).json({ error: error.message });
  }
};

function isAdmin(req) {
  return Boolean(req.auth?.isAdmin);
}

function getAuthenticatedCustomerId(req) {
  return Number(req.auth?.userId || 0);
}

function canAccessCustomer(req, customerId) {
  return isAdmin(req) || getAuthenticatedCustomerId(req) === Number(customerId);
}

router.use("/admin", requireAuth, requireAdmin);

const toCustomerResponse = (c) => ({
  id: c.id,
  fullName: c.fullName,
  mobile: c.mobile,
  nationalId: c.nationalId || "",
  residencyStatus: c.residencyStatus || "resident",
  tin: c.tin || "",
  identityVerified: Boolean(c.identityVerified),
  registrationStatus: c.registrationStatus || "approved",
  status: c.status,
  email: c.email,
  emailVerified: Boolean(c.emailVerified),
  failedLoginAttempts: Number(c.failedLoginAttempts || 0),
  lockedUntil: c.lockedUntil,
  lastLoginAt: c.lastLoginAt,
});

const toAccountResponse = (a) => ({
  id: a.id,
  accountNumber: a.accountNumber,
  customerId: a.customerId,
  accountHolder: a.accountHolder || a.Customer?.fullName || "",
  type: a.accountType,
  balance: Number(a.balance),
  maintenanceFee: a.accountType === "Simple Access" ? 2.5 : 0,
  currency: a.currency,
  status: a.status,
  createdAt: a.createdAt,
});

async function getCustomerForAccountPayload(payload, options = {}) {
  const providedCustomerName = String(payload.customerName || "").trim();
  let customerId = payload.customerId;

  if (customerId !== undefined && customerId !== null && customerId !== "") {
    const numericCustomerId = Number(customerId);
    if (!Number.isFinite(numericCustomerId) || numericCustomerId <= 0) {
      throw new Error("customerId must be a positive number");
    }

    const customer = await Customer.findByPk(numericCustomerId);
    if (!customer) {
      throw new Error("Customer not found");
    }

    return customer;
  }

  if (!providedCustomerName) {
    throw new Error("customerName is required when customerId is not provided");
  }

  const normalizedName = providedCustomerName.toLowerCase();
  let customer = await Customer.findOne({
    where: Customer.sequelize.where(
      Customer.sequelize.fn("LOWER", Customer.sequelize.col("fullName")),
      normalizedName
    ),
  });

  if (!customer) {
    const nonce = Date.now();
    const safeName = providedCustomerName
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, ".")
      .replace(/^\.+|\.+$/g, "") || "customer";

    customer = await Customer.create({
      fullName: providedCustomerName,
      mobile: `+6799${String(nonce).slice(-6)}`,
      email: `${safeName}.${nonce}@autocreated.local`,
      password: "temporary-password",
      status: "active",
      tin: "",
      residencyStatus: "resident",
      identityVerified: false,
      registrationStatus: "approved",
      ...options.customerDefaults,
    });
  }

  return customer;
}

async function syncCustomerAccountHolders(customerId, fullName) {
  await Account.update(
    { accountHolder: String(fullName || "").trim() },
    { where: { customerId: Number(customerId) } }
  );
}

function parseScheduledAccountId(description) {
  const text = String(description || "");
  const match = text.match(/scheduled_account:(\d+)/i);
  return match ? Number(match[1]) : null;
}

router.get("/health", (req, res) => {
  res.json({ status: "ok", service: "BoF Banking API", at: new Date().toISOString() });
});

router.get("/requirements", (req, res) => {
  res.json(requirementsData);
});

router.get("/customers", requireAuth, asyncHandler(async (req, res) => {
  const search = String(req.query.q || "").trim();
  const baseWhere = isAdmin(req) ? {} : { id: getAuthenticatedCustomerId(req) };
  const where = search
    ? {
        ...baseWhere,
        [Op.or]: [
          { fullName: { [Op.like]: `%${search}%` } },
          { email: { [Op.like]: `%${search}%` } },
          { mobile: { [Op.like]: `%${search}%` } },
          { nationalId: { [Op.like]: `%${search}%` } },
        ],
      }
    : baseWhere;
  const rows = await Customer.findAll({ where, order: [["createdAt", "ASC"]] });
  res.json(rows.map(toCustomerResponse));
}));

router.get("/admin/customers", asyncHandler(async (req, res) => {
  const search = String(req.query.q || "").trim();
  const where = search
    ? {
        [Op.or]: [
          { fullName: { [Op.like]: `%${search}%` } },
          { email: { [Op.like]: `%${search}%` } },
          { mobile: { [Op.like]: `%${search}%` } },
          { nationalId: { [Op.like]: `%${search}%` } },
        ],
      }
    : undefined;
  const rows = await Customer.findAll({ where, order: [["createdAt", "ASC"]] });
  res.json(rows.map(toCustomerResponse));
}));

router.get("/dashboard", requireAuth, asyncHandler(async (req, res) => {
  const customerId = Number(req.query.customerId);
  if (!Number.isFinite(customerId) || customerId <= 0) {
    return res.status(400).json({ error: "Valid customerId query is required" });
  }
  if (!canAccessCustomer(req, customerId)) {
    return res.status(403).json({ error: "Forbidden" });
  }
  const result = await getDashboard(customerId);
  res.json(result);
}));

router.get("/profile/:customerId", requireAuth, asyncHandler(async (req, res) => {
  if (!canAccessCustomer(req, req.params.customerId)) {
    return res.status(403).json({ error: "Forbidden" });
  }
  const customer = await Customer.findByPk(req.params.customerId);
  if (!customer) {
    return res.status(404).json({ error: "Customer not found" });
  }
  res.json(toCustomerResponse(customer));
}));

router.put("/update-profile", requireAuth, asyncHandler(async (req, res) => {
  const customerId = Number(req.body?.customerId);
  if (!Number.isFinite(customerId) || customerId <= 0) {
    return res.status(400).json({ error: "Valid customerId is required" });
  }
  if (!canAccessCustomer(req, customerId)) {
    return res.status(403).json({ error: "Forbidden" });
  }
  const result = await updateProfile(customerId, req.body || {});
  res.json(result);
}));

router.post("/customers", asyncHandler(async (req, res) => {
  const payload = req.body || {};
  if (!payload.fullName || !payload.mobile) {
    return res.status(400).json({ error: "fullName and mobile are required" });
  }

  const customer = await Customer.create({
    fullName: payload.fullName,
    mobile: payload.mobile,
    email: payload.email || `customer-${Date.now()}@example.com`,
    password: payload.password || "temporary-password",
    status: "active",
    tin: payload.tin || "",
    residencyStatus: payload.residencyStatus || "resident",
    identityVerified: Boolean(payload.identityVerified),
    registrationStatus: payload.registrationStatus || "approved",
  });

  res.status(201).json(toCustomerResponse(customer));
}));

router.patch("/admin/customers/:id", asyncHandler(async (req, res) => {
  const customer = await Customer.findByPk(req.params.id);
  if (!customer) {
    return res.status(404).json({ error: "Customer not found" });
  }

  const payload = req.body || {};
  const updates = {};
  const allowed = [
    "fullName",
    "mobile",
    "email",
    "nationalId",
    "status",
    "tin",
    "residencyStatus",
    "identityVerified",
    "registrationStatus",
    "emailVerified",
    "lockedUntil",
    "failedLoginAttempts",
  ];

  allowed.forEach((field) => {
    if (payload[field] !== undefined) {
      updates[field] = payload[field];
    }
  });

  await customer.update(updates);
  if (updates.fullName !== undefined) {
    await syncCustomerAccountHolders(customer.id, customer.fullName);
  }
  res.json(toCustomerResponse(customer));
}));

router.get("/accounts", requireAuth, asyncHandler(async (req, res) => {
  const where = isAdmin(req) ? undefined : { customerId: getAuthenticatedCustomerId(req) };
  const rows = await Account.findAll({ where, order: [["createdAt", "ASC"]] });
  res.json(rows.map(toAccountResponse));
}));

router.post("/accounts", asyncHandler(async (req, res) => {
  const payload = req.body || {};
  const providedAccountNumber = String(payload.accountNumber || "").trim();
  if (!payload.type) {
    return res.status(400).json({ error: "type is required" });
  }
  if (!["Simple Access", "Savings"].includes(payload.type)) {
    return res.status(400).json({ error: "type must be Simple Access or Savings" });
  }
  if (providedAccountNumber && !/^\d{12}$/.test(providedAccountNumber)) {
    return res.status(400).json({ error: "Reenter 12 digit number" });
  }

  const customer = await getCustomerForAccountPayload(payload);

  const account = await Account.create({
    customerId: customer.id,
    accountNumber: providedAccountNumber || await generateRandomAccountNumber(),
    accountHolder: customer.fullName,
    accountType: payload.type,
    balance: Number(payload.openingBalance || 0),
    currency: "FJD",
    status: "active",
  });

  res.status(201).json(toAccountResponse(account));
}));

router.post("/accounts/request", asyncHandler(async (req, res) => {
  const payload = req.body || {};
  if (!payload.type) {
    return res.status(400).json({ error: "type is required" });
  }
  if (!["Simple Access", "Savings"].includes(payload.type)) {
    return res.status(400).json({ error: "type must be Simple Access or Savings" });
  }

  const customerId = Number(payload.customerId);
  if (!Number.isFinite(customerId) || customerId <= 0) {
    return res.status(400).json({ error: "Valid customerId is required" });
  }

  const customer = await Customer.findByPk(customerId);
  if (!customer) {
    return res.status(404).json({ error: "Customer not found" });
  }

  const providedAccountNumber = String(payload.accountNumber || "").trim();
  if (providedAccountNumber && !/^\d{12}$/.test(providedAccountNumber)) {
    return res.status(400).json({ error: "Reenter 12 digit number" });
  }

  const account = await Account.create({
    customerId,
    accountNumber: providedAccountNumber || await generateRandomAccountNumber(),
    accountHolder: customer.fullName,
    accountType: payload.type,
    balance: Number(payload.openingBalance || 0),
    currency: "FJD",
    status: "pending_approval",
  });

  res.status(201).json(toAccountResponse(account));
}));

router.patch("/admin/accounts/:id", asyncHandler(async (req, res) => {
  const account = await Account.findByPk(req.params.id);
  if (!account) {
    return res.status(404).json({ error: "Account not found" });
  }

  const payload = req.body || {};
  const updates = {};
  if (payload.type !== undefined) {
    if (!["Simple Access", "Savings"].includes(payload.type)) {
      return res.status(400).json({ error: "type must be Simple Access or Savings" });
    }
    updates.accountType = payload.type;
  }
  if (payload.status !== undefined) {
    updates.status = payload.status;
  }
  if (payload.accountNumber !== undefined) {
    const candidate = String(payload.accountNumber || "").trim();
    if (!/^\d{12}$/.test(candidate)) {
      return res.status(400).json({ error: "Reenter 12 digit number" });
    }
    updates.accountNumber = candidate;
  }
  if (payload.accountHolder !== undefined) {
    const candidateHolder = String(payload.accountHolder || "").trim();
    if (!candidateHolder) {
      return res.status(400).json({ error: "accountHolder cannot be empty" });
    }
    updates.accountHolder = candidateHolder;
  }

  await account.update(updates);
  res.json(toAccountResponse(account));
}));

router.post("/admin/accounts/:id/freeze", asyncHandler(async (req, res) => {
  const account = await Account.findByPk(req.params.id);
  if (!account) {
    return res.status(404).json({ error: "Account not found" });
  }
  await account.update({ status: "frozen" });
  res.json(toAccountResponse(account));
}));

router.post("/admin/create-account", asyncHandler(async (req, res) => {
  const payload = req.body || {};
  const providedAccountNumber = String(payload.accountNumber || "").trim();
  if (!payload.type) {
    return res.status(400).json({ error: "type is required" });
  }
  if (!["Simple Access", "Savings", "Current"].includes(payload.type)) {
    return res.status(400).json({ error: "type must be Simple Access, Current or Savings" });
  }
  if (providedAccountNumber && !/^\d{12}$/.test(providedAccountNumber)) {
    return res.status(400).json({ error: "Reenter 12 digit number" });
  }

  const customer = await getCustomerForAccountPayload(payload, {
    customerDefaults: {
      nationalId: `AUTO-${Date.now()}`,
      emailVerified: true,
    },
  });

  const account = await Account.create({
    customerId: customer.id,
    accountNumber: providedAccountNumber || await generateRandomAccountNumber(),
    accountHolder: customer.fullName,
    accountType: payload.type,
    balance: Number(payload.openingBalance || 0),
    currency: "FJD",
    status: "active",
  });

  res.status(201).json(toAccountResponse(account));
}));

router.put("/admin/freeze-account", asyncHandler(async (req, res) => {
  const account = await Account.findByPk(req.body?.accountId);
  if (!account) {
    return res.status(404).json({ error: "Account not found" });
  }
  await account.update({ status: "frozen" });
  res.json(toAccountResponse(account));
}));

router.get("/admin/transactions", asyncHandler(async (req, res) => {
  const accountNumber = String(req.query.accountNumber || "").trim();
  let accountIdFilter = null;

  if (accountNumber) {
    const account = await Account.findOne({ where: { accountNumber } });
    if (!account) {
      return res.json([]);
    }
    accountIdFilter = account.id;
  }

  const rows = await Transaction.findAll({
    where: accountIdFilter ? { accountId: accountIdFilter } : undefined,
    order: [["createdAt", "DESC"]],
    limit: 500,
  });

  res.json(rows.map((t) => ({
    id: t.id,
    accountId: t.accountId,
    kind: t.type,
    amount: Number(t.amount),
    description: t.description,
    status: t.status,
    suspicious: Number(t.amount) >= getHighValueTransferThreshold() || String(t.status || "").toLowerCase() === "reversed",
    createdAt: t.createdAt,
  })));
}));

router.get("/admin/login-logs", asyncHandler(async (req, res) => {
  const limit = Number(req.query.limit || 200);
  const rows = await getLoginLogs(limit);
  res.json(rows);
}));

router.post("/admin/transactions/:id/reverse", asyncHandler(async (req, res) => {
  const result = await reverseTransaction(req.params.id);
  res.json(result);
}));

router.get("/transactions", asyncHandler(async (req, res) => {
  const { accountId } = req.query;
  if (!accountId) {
    return res.status(400).json({ error: "accountId query is required" });
  }
  const rows = await getAccountTransactions(accountId);
  res.json(rows.map((t) => ({
    id: t.id,
    accountId: t.accountId,
    kind: t.type,
    amount: Number(t.amount),
    description: t.description,
    counterpartyAccountId: null,
    createdAt: t.createdAt,
  })));
}));

router.post("/transfers/initiate", requireAuth, asyncHandler(async (req, res) => {
  const payload = req.body || {};
  const fromAccount = await Account.findByPk(payload.fromAccountId);
  if (!fromAccount) {
    return res.status(404).json({ error: "Source account not found" });
  }
  if (!isAdmin(req) && fromAccount.customerId !== getAuthenticatedCustomerId(req)) {
    return res.status(403).json({ error: "Forbidden" });
  }
  const result = await initiateTransfer(payload);
  res.json({ highValueThreshold: getHighValueTransferThreshold(), ...result });
}));

router.post("/transaction/initiate", requireAuth, asyncHandler(async (req, res) => {
  const payload = req.body || {};
  const fromAccount = await Account.findByPk(payload.fromAccountId);
  if (!fromAccount) {
    return res.status(404).json({ error: "Source account not found" });
  }
  if (!isAdmin(req) && fromAccount.customerId !== getAuthenticatedCustomerId(req)) {
    return res.status(403).json({ error: "Forbidden" });
  }
  const result = await initiateTransfer(payload);
  res.json({ highValueThreshold: getHighValueTransferThreshold(), ...result });
}));

router.post("/transfer", requireAuth, asyncHandler(async (req, res) => {
  const payload = req.body || {};
  const fromAccount = await Account.findByPk(payload.fromAccountId);
  if (!fromAccount) {
    return res.status(404).json({ error: "Source account not found" });
  }
  if (!isAdmin(req) && fromAccount.customerId !== getAuthenticatedCustomerId(req)) {
    return res.status(403).json({ error: "Forbidden" });
  }
  const result = await initiateTransfer(payload);
  res.json({ highValueThreshold: getHighValueTransferThreshold(), ...result });
}));

router.post("/otp/send", requireAuth, asyncHandler(async (req, res) => {
  const payload = req.body || {};
  const transferPayload = payload.transaction || payload;
  const amount = Number(transferPayload.amount || 0);
  if (!Number.isFinite(amount) || amount < getHighValueTransferThreshold()) {
    return res.status(400).json({ error: `OTP is only required for amounts >= ${getHighValueTransferThreshold()}` });
  }
  const fromAccount = await Account.findByPk(transferPayload.fromAccountId);
  if (!fromAccount) {
    return res.status(404).json({ error: "Source account not found" });
  }
  if (!isAdmin(req) && fromAccount.customerId !== getAuthenticatedCustomerId(req)) {
    return res.status(403).json({ error: "Forbidden" });
  }
  const result = await initiateTransfer(transferPayload);
  res.json({ highValueThreshold: getHighValueTransferThreshold(), ...result });
}));

router.post("/otp/verify", requireAuth, asyncHandler(async (req, res) => {
  const transferId = String(req.body?.transferId || "").trim();
  const pending = await OtpVerification.findOne({ where: { referenceCode: transferId, transactionType: "transfer" } });
  if (!pending) {
    return res.status(404).json({ error: "Pending transfer not found" });
  }
  if (!isAdmin(req) && pending.customerId !== getAuthenticatedCustomerId(req)) {
    return res.status(403).json({ error: "Forbidden" });
  }
  const result = await verifyTransfer(req.body || {});
  res.json(result);
}));

router.get("/admin/transfers", asyncHandler(async (req, res) => {
  const rows = await Transaction.findAll({
    where: { description: { [Op.in]: ["Transfer sent", "Transfer received"] } },
    order: [["createdAt", "DESC"]],
    limit: 500,
  });

  res.json(rows.map((t) => ({
    id: t.id,
    accountId: t.accountId,
    kind: t.type,
    amount: Number(t.amount),
    description: t.description,
    createdAt: t.createdAt,
  })));
}));

router.get("/admin/transfer-limit", (req, res) => {
  res.json({ highValueTransferLimit: getHighValueTransferThreshold() });
});

router.put("/admin/transfer-limit", (req, res) => {
  const updated = setHighValueTransferThreshold(req.body?.highValueTransferLimit);
  res.json({ highValueTransferLimit: updated });
});

router.get("/admin/dashboard-report", asyncHandler(async (req, res) => {
  const [totalCustomers, totalAccounts, totalDepositsRaw, pendingLoans, frozenAccounts] = await Promise.all([
    Customer.count(),
    Account.count(),
    Account.sum("balance"),
    Loan.count({ where: { status: "submitted" } }),
    Account.count({ where: { status: { [Op.in]: ["frozen", "suspended"] } } }),
  ]);

  const totalDeposits = Number(totalDepositsRaw || 0);
  const sevenDaysAgo = new Date();
  sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 6);
  sevenDaysAgo.setHours(0, 0, 0, 0);

  const dailyRows = await Transaction.findAll({
    attributes: [
      [Account.sequelize.fn("DATE", Account.sequelize.col("createdAt")), "day"],
      [Account.sequelize.fn("COUNT", Account.sequelize.col("id")), "count"],
      [Account.sequelize.fn("SUM", Account.sequelize.col("amount")), "totalAmount"],
    ],
    where: {
      createdAt: { [Op.gte]: sevenDaysAgo },
    },
    group: [Account.sequelize.fn("DATE", Account.sequelize.col("createdAt"))],
    order: [[Account.sequelize.fn("DATE", Account.sequelize.col("createdAt")), "ASC"]],
    raw: true,
  });

  const dailyMap = {};
  dailyRows.forEach((row) => {
    dailyMap[String(row.day)] = {
      count: Number(row.count || 0),
      totalAmount: Number(row.totalAmount || 0),
    };
  });

  const transactionsByDay = [];
  for (let i = 0; i < 7; i += 1) {
    const date = new Date(sevenDaysAgo);
    date.setDate(sevenDaysAgo.getDate() + i);
    const key = date.toISOString().slice(0, 10);
    transactionsByDay.push({
      day: key,
      count: dailyMap[key]?.count || 0,
      totalAmount: Number((dailyMap[key]?.totalAmount || 0).toFixed(2)),
    });
  }

  const accountTypeRows = await Account.findAll({
    attributes: [
      [Account.sequelize.col("accountType"), "label"],
      [Account.sequelize.fn("COUNT", Account.sequelize.col("id")), "value"],
    ],
    group: [Account.sequelize.col("accountType")],
    raw: true,
  });

  const loanStatusRows = await Loan.findAll({
    attributes: [
      [Loan.sequelize.col("status"), "label"],
      [Loan.sequelize.fn("COUNT", Loan.sequelize.col("id")), "value"],
    ],
    group: [Loan.sequelize.col("status")],
    raw: true,
  });

  const latestTransactions = await Transaction.findAll({
    order: [["createdAt", "DESC"]],
    limit: 8,
  });

  const recentTransactionRows = latestTransactions.map((t) => ({
    id: t.id,
    accountId: t.accountId,
    kind: t.type,
    amount: Number(t.amount),
    description: t.description,
    createdAt: t.createdAt,
  }));

  const todayStart = new Date();
  todayStart.setHours(0, 0, 0, 0);
  const todaysTransactions = await Transaction.count({
    where: { createdAt: { [Op.gte]: todayStart } },
  });

  res.json({
    generatedAt: new Date().toISOString(),
    metrics: {
      totalCustomers,
      totalAccounts,
      totalDeposits: Number(totalDeposits.toFixed(2)),
      pendingLoans,
      frozenAccounts,
      todaysTransactions,
    },
    transactionsByDay,
    accountTypeBreakdown: accountTypeRows.map((x) => ({ label: x.label || "Unknown", value: Number(x.value || 0) })),
    loanStatusBreakdown: loanStatusRows.map((x) => ({ label: x.label || "unknown", value: Number(x.value || 0) })),
    recentTransactions: recentTransactionRows,
  });
}));

router.post("/transfers/verify", requireAuth, asyncHandler(async (req, res) => {
  const transferId = String(req.body?.transferId || "").trim();
  const pending = await OtpVerification.findOne({ where: { referenceCode: transferId, transactionType: "transfer" } });
  if (!pending) {
    return res.status(404).json({ error: "Pending transfer not found" });
  }
  if (!isAdmin(req) && pending.customerId !== getAuthenticatedCustomerId(req)) {
    return res.status(403).json({ error: "Forbidden" });
  }
  const result = await verifyTransfer(req.body || {});
  res.json(result);
}));

router.post("/bills/manual", requireAuth, asyncHandler(async (req, res) => {
  const payload = req.body || {};
  const account = await Account.findByPk(payload.accountId);
  if (!account) {
    return res.status(404).json({ error: "Account not found" });
  }
  if (!isAdmin(req) && account.customerId !== getAuthenticatedCustomerId(req)) {
    return res.status(403).json({ error: "Forbidden" });
  }
  const payment = await postBillPayment({
    accountId: payload.accountId,
    payee: payload.payee,
    amount: Number(payload.amount),
    mode: "manual",
  });
  res.status(201).json(payment);
}));

router.post("/pay-bill", requireAuth, asyncHandler(async (req, res) => {
  const payload = req.body || {};
  const account = await Account.findByPk(payload.accountId);
  if (!account) {
    return res.status(404).json({ error: "Account not found" });
  }
  if (!isAdmin(req) && account.customerId !== getAuthenticatedCustomerId(req)) {
    return res.status(403).json({ error: "Forbidden" });
  }
  const payment = await postBillPayment({
    accountId: payload.accountId,
    payee: payload.payee,
    amount: Number(payload.amount),
    mode: payload.mode || "manual",
    scheduledDate: payload.scheduledDate || null,
  });
  res.status(201).json(payment);
}));

router.post("/bills/scheduled", requireAuth, asyncHandler(async (req, res) => {
  const payload = req.body || {};
  const account = await Account.findByPk(payload.accountId);
  if (!account) {
    return res.status(404).json({ error: "Account not found" });
  }
  if (!isAdmin(req) && account.customerId !== getAuthenticatedCustomerId(req)) {
    return res.status(403).json({ error: "Forbidden" });
  }
  const row = await scheduleBillPayment({
    accountId: payload.accountId,
    payee: payload.payee,
    amount: Number(payload.amount),
    scheduledDate: payload.scheduledDate,
  });
  res.status(201).json(row);
}));

router.get("/bills/scheduled", asyncHandler(async (req, res) => {
  const rows = await Bill.findAll({ where: { status: "scheduled" }, order: [["createdAt", "DESC"]] });
  res.json(
    rows.map((b) => ({
      id: b.id,
      accountId: parseScheduledAccountId(b.description),
      customerId: b.customerId,
      payee: b.billType,
      amount: Number(b.amount),
      scheduledDate: b.dueDate,
      status: "scheduled",
      createdAt: b.createdAt,
    }))
  );
}));

router.post("/bills/scheduled/:id/run", asyncHandler(async (req, res) => {
  const result = await runScheduledPayment(req.params.id);
  res.json(result);
}));

router.get("/investments", asyncHandler(async (req, res) => {
  const rows = await Investment.findAll({ order: [["createdAt", "DESC"]] });
  res.json(
    rows.map((x) => ({
      id: x.id,
      customerId: x.customerId,
      name: x.investmentType,
      amount: Number(x.amount),
      annualRate: Number(x.expectedReturn || 0),
      createdAt: x.createdAt,
    }))
  );
}));

router.post("/investments", asyncHandler(async (req, res) => {
  const payload = req.body || {};
  const row = await createInvestment({
    customerId: payload.customerId,
    name: payload.name,
    amount: Number(payload.amount),
    annualRate: Number(payload.annualRate),
  });
  res.status(201).json(row);
}));

router.get("/statements/:accountId", asyncHandler(async (req, res) => {
  const rows = await generateStatement(req.params.accountId, req.query.from, req.query.to);
  res.json(rows);
}));

router.get("/statements/:accountId/download", asyncHandler(async (req, res) => {
  const rows = await generateStatement(req.params.accountId, req.query.from, req.query.to);
  const lines = ["transactionId,createdAt,kind,amount,description,counterparty"];
  rows.forEach((r) => {
    lines.push(
      `${r.id},${r.createdAt},${r.type || r.kind},${r.amount},"${(r.description || "").replace(/\"/g, '""')}",${r.counterpartyAccountId || ""}`
    );
  });

  const csv = lines.join("\n");
  res.setHeader("Content-Type", "text/csv");
  res.setHeader("Content-Disposition", `attachment; filename=statement-${req.params.accountId}.csv`);
  res.send(csv);
}));

router.get("/notifications/:customerId", requireAuth, asyncHandler(async (req, res) => {
  const customerId = Number(req.params.customerId);
  if (!canAccessCustomer(req, customerId)) {
    return res.status(403).json({ error: "Forbidden" });
  }
  const limit = Number(req.query.limit || 200);
  const logs = await getNotificationLogs(limit, customerId);
  res.json(logs);
}));

router.post("/notifications/send", requireAuth, asyncHandler(async (req, res) => {
  const customerId = Number(req.body?.customerId);
  const message = String(req.body?.message || "").trim();
  const notificationType = String(req.body?.notificationType || "SMS_ALERT").trim();

  if (!Number.isFinite(customerId) || customerId <= 0) {
    return res.status(400).json({ error: "Valid customerId is required" });
  }
  if (!message) {
    return res.status(400).json({ error: "message is required" });
  }
  if (!canAccessCustomer(req, customerId)) {
    return res.status(403).json({ error: "Forbidden" });
  }

  const log = await addNotification(customerId, message, notificationType);
  res.status(201).json(log);
}));

router.get("/notifications/history", requireAuth, asyncHandler(async (req, res) => {
  const limit = Number(req.query.limit || 200);
  const requestedCustomerId = req.query.customerId ? Number(req.query.customerId) : null;

  if (requestedCustomerId && !canAccessCustomer(req, requestedCustomerId)) {
    return res.status(403).json({ error: "Forbidden" });
  }

  const customerId = requestedCustomerId || (isAdmin(req) ? null : getAuthenticatedCustomerId(req));
  const logs = await getNotificationLogs(limit, customerId);
  res.json(logs);
}));

router.get("/admin/notifications/logs", asyncHandler(async (req, res) => {
  const limit = Number(req.query.limit || 200);
  res.json(await getNotificationLogs(limit));
}));

router.get("/admin/otp-attempts", asyncHandler(async (req, res) => {
  const limit = Number(req.query.limit || 200);
  res.json(await getOtpAttempts(limit));
}));

router.get("/config/interest-rate", (req, res) => {
  res.json({ reserveBankMinSavingsInterestRate });
});

router.put("/config/interest-rate", (req, res) => {
  const rate = Number(req.body?.reserveBankMinSavingsInterestRate);
  if (!Number.isFinite(rate) || rate < 0) {
    return res.status(400).json({ error: "Valid non-negative rate is required" });
  }
  reserveBankMinSavingsInterestRate = rate;
  res.json({ reserveBankMinSavingsInterestRate: rate });
});

router.post("/year-end/interest-summaries", asyncHandler(async (req, res) => {
  const year = Number(req.body?.year || new Date().getFullYear());
  const rows = await generateInterestSummaries(year);
  res.json(rows);
}));

router.get("/year-end/interest-summaries", asyncHandler(async (req, res) => {
  const year = Number(req.query?.year || new Date().getFullYear());
  const rows = await Account.findAll({
    include: [{ model: Customer, attributes: ["id", "fullName"] }],
  });

  const summaries = rows.map((account) => {
    const grossInterest = account.accountType === "Savings" ? Number((Number(account.balance) * reserveBankMinSavingsInterestRate).toFixed(2)) : 0;
    const withholdingTax = Number((grossInterest * 0.15).toFixed(2));
    const netInterest = Number((grossInterest - withholdingTax).toFixed(2));
    return {
      year,
      accountId: account.id,
      customerId: account.customerId,
      customerName: account.Customer?.fullName || "Unknown",
      grossInterest,
      withholdingTax,
      netInterest,
      status: "submitted_to_frcs",
    };
  });

  res.json(summaries);
}));

router.post("/accounts/apply-maintenance-fees", asyncHandler(async (req, res) => {
  const charged = await applyMonthlyFees();
  res.json({ chargedAccounts: charged, count: charged.length });
}));

router.get("/loan-products", (req, res) => {
  res.json(loanProducts);
});

router.post("/loan-applications", asyncHandler(async (req, res) => {
  const payload = req.body || {};
  const required = ["customerId", "loanProductId", "requestedAmount", "termMonths", "purpose"];
  const missing = required.filter((k) => payload[k] === undefined || payload[k] === null || payload[k] === "");
  if (missing.length > 0) {
    return res.status(400).json({ error: `Missing fields: ${missing.join(", ")}` });
  }

  const product = loanProducts.find((x) => x.id === payload.loanProductId);
  if (!product) {
    return res.status(400).json({ error: "Invalid loanProductId" });
  }

  const loan = await Loan.create({
    customerId: payload.customerId,
    loanProductId: payload.loanProductId,
    termMonths: Number(payload.termMonths),
    loanType: product.name,
    principal: Number(payload.requestedAmount),
    interestRate: Number((product.annualRate * 100).toFixed(2)),
    disbursedAmount: 0,
    maturityDate: new Date(new Date().setMonth(new Date().getMonth() + Number(payload.termMonths))),
    status: "submitted",
  });

  const row = {
    id: loan.id,
    customerId: loan.customerId,
    loanProductId: loan.loanProductId,
    requestedAmount: Number(loan.principal),
    termMonths: Number(loan.termMonths),
    purpose: payload.purpose,
    monthlyIncome: Number(payload.monthlyIncome || 0),
    employmentStatus: payload.employmentStatus || "unknown",
    status: loan.status,
    createdAt: loan.createdAt,
  };
  res.status(201).json(row);
}));

router.get("/loan-applications", asyncHandler(async (req, res) => {
  const rows = await Loan.findAll({ order: [["createdAt", "DESC"]] });
  res.json(
    rows.map((l) => ({
      id: l.id,
      customerId: l.customerId,
      loanProductId: l.loanProductId,
      requestedAmount: Number(l.principal),
      termMonths: Number(l.termMonths),
      purpose: l.loanType,
      monthlyIncome: 0,
      employmentStatus: "unknown",
      status: l.status,
      createdAt: l.createdAt,
    }))
  );
}));

router.patch("/admin/loan-applications/:id", asyncHandler(async (req, res) => {
  const loan = await Loan.findByPk(req.params.id);
  if (!loan) {
    return res.status(404).json({ error: "Loan application not found" });
  }

  const payload = req.body || {};
  const updates = {};
  if (payload.status !== undefined) {
    updates.status = payload.status;
  }
  if (payload.interestRate !== undefined) {
    const rate = Number(payload.interestRate);
    if (!Number.isFinite(rate) || rate < 0) {
      return res.status(400).json({ error: "interestRate must be a non-negative number" });
    }
    updates.interestRate = rate;
  }
  await loan.update(updates);

  res.json({
    id: loan.id,
    customerId: loan.customerId,
    loanProductId: loan.loanProductId,
    requestedAmount: Number(loan.principal),
    termMonths: Number(loan.termMonths),
    status: loan.status,
    interestRate: Number(loan.interestRate || 0),
    createdAt: loan.createdAt,
  });
}));

module.exports = router;
