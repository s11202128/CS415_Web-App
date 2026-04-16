const express = require("express");
const PDFDocument = require("pdfkit");
const { Op } = require("sequelize");
const requirementsData = require("../config/requirementsData");
const { requireAuth, requireAdmin } = require("../middleware/auth");
const { normalizeActivityType, logCustomerActivity } = require("../services/activityLogService");
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
  getSavingsInterestRate,
  setSavingsInterestRate,
  applyMonthlySavingsInterest,
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
  generateRandomAccountPin,
} = require("../store-mysql");
const { Customer, Account, Bill, Investment, Loan, Transaction, OtpVerification, StatementRequest, ActivityLog } = require("../models");

const loanProducts = [
  { id: "LP-001", name: "Personal Loan", annualRate: 0.089, maxAmount: 30000, minTermMonths: 6, maxTermMonths: 60 },
  { id: "LP-002", name: "Home Loan", annualRate: 0.061, maxAmount: 450000, minTermMonths: 60, maxTermMonths: 360 },
  { id: "LP-003", name: "Vehicle Loan", annualRate: 0.074, maxAmount: 90000, minTermMonths: 12, maxTermMonths: 84 },
];

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
  return Number(req.auth?.customerId || req.auth?.userId || 0);
}

function canAccessCustomer(req, customerId) {
  return isAdmin(req) || getAuthenticatedCustomerId(req) === Number(customerId);
}

function parseDateParam(value, fieldName) {
  const raw = String(value || "").trim();
  if (!raw) {
    throw new Error(`${fieldName} is required`);
  }

  const parsed = new Date(raw);
  if (Number.isNaN(parsed.getTime())) {
    throw new Error(`${fieldName} must be a valid date`);
  }

  return parsed;
}

async function resolveStatementPayloadForUser(req) {
  const payload = req.body || {};
  const customerId = getAuthenticatedCustomerId(req);
  if (!Number.isFinite(customerId) || customerId <= 0) {
    throw new Error("Authenticated customer session required");
  }

  const fromDate = parseDateParam(payload.fromDate, "fromDate");
  const toDate = parseDateParam(payload.toDate, "toDate");
  if (fromDate.getTime() > toDate.getTime()) {
    throw new Error("fromDate must be earlier than or equal to toDate");
  }
  toDate.setHours(23, 59, 59, 999);

  const customer = await Customer.findByPk(customerId);
  if (!customer) {
    throw new Error("Customer not found");
  }

  const accounts = await Account.findAll({
    where: { customerId },
    attributes: ["id", "accountNumber"],
    order: [["id", "ASC"]],
  });

  if (!accounts.length) {
    throw new Error("No accounts found for this user");
  }

  const requestedAccountId = Number(payload.accountId || 0);
  if (!Number.isFinite(requestedAccountId) || requestedAccountId <= 0) {
    throw new Error("accountId is required for statement generation");
  }

  const matched = accounts.find((row) => Number(row.id) === requestedAccountId);
  if (!matched) {
    throw new Error("Selected account not found for this user");
  }

  const selectedAccounts = [matched];
  const accountNumbers = selectedAccounts.map((row) => String(row.accountNumber));
  const transactions = await Transaction.findAll({
    where: {
      accountNumber: { [Op.in]: accountNumbers },
      createdAt: { [Op.between]: [fromDate, toDate] },
    },
    order: [["createdAt", "ASC"]],
  });

  const rows = transactions.map((tx) => ({
    id: tx.id,
    user_id: Number(tx.userId || customerId),
    date: tx.date || tx.createdAt,
    description: tx.description || "",
    amount: Number(tx.amount),
    balance: Number(tx.balance || tx.balanceAfter || 0),
    transactionType: tx.transactionType || tx.type,
    accountNumber: String(tx.accountNumber || ""),
  }));

  return {
    customer,
    accountNumber: accountNumbers[0],
    accountNumbers,
    fromDate,
    toDate,
    transactions: rows,
  };
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
  accountPin: a.accountPin,
  customerId: a.customerId,
  accountHolder: a.accountHolder || a.Customer?.fullName || "",
  type: a.accountType,
  balance: Number(a.balance),
  requestedOpeningBalance: a.requestedOpeningBalance == null ? null : Number(a.requestedOpeningBalance),
  approvedOpeningBalance: a.approvedOpeningBalance == null ? null : Number(a.approvedOpeningBalance),
  approvedByAdminId: a.approvedByAdminId || null,
  approvedAt: a.approvedAt || null,
  rejectionReason: a.rejectionReason || null,
  maintenanceFee: a.accountType === "Simple Access" ? 2.5 : 0,
  currency: a.currency,
  status: a.status,
  createdAt: a.createdAt,
});

const toInvestmentResponse = (row) => ({
  id: row.id,
  customerId: row.customerId,
  customerName: row.Customer?.fullName || "Unknown",
  investmentType: row.investmentType,
  amount: Number(row.amount),
  expectedReturn: row.expectedReturn == null ? null : Number(row.expectedReturn),
  maturityDate: row.maturityDate,
  status: row.status,
  createdAt: row.createdAt,
  updatedAt: row.updatedAt,
});

const toStatementRequestResponse = (row) => ({
  id: row.id,
  customerId: row.customerId,
  accountId: row.accountId || null,
  fullName: row.fullName,
  accountHolder: row.accountHolder,
  accountNumber: row.accountNumber,
  fromDate: row.fromDate,
  toDate: row.toDate,
  status: row.status,
  adminNote: row.adminNote,
  reviewedBy: row.reviewedBy,
  reviewedByAdminId: row.reviewedByAdminId || null,
  reviewedAt: row.reviewedAt,
  createdAt: row.createdAt,
  updatedAt: row.updatedAt,
});

async function getCustomerForAccountPayload(payload, options = {}) {
  const allowAutoCreate = options.allowAutoCreate === true;
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

    if (providedCustomerName && String(customer.fullName || "").trim().toLowerCase() !== providedCustomerName.toLowerCase()) {
      throw new Error("customerId and customerName do not match");
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

  if (!customer && !allowAutoCreate) {
    throw new Error("Customer not found. Use an existing customer ID.");
  }

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

function parsePaidBillAccountId(description) {
  const text = String(description || "");
  const match = text.match(/account:(\d+)/i);
  return match ? Number(match[1]) : null;
}

async function mapTransactionRows(rows) {
  const accountNumbers = Array.from(new Set(rows.map((row) => String(row.accountNumber || "")).filter(Boolean)));
  const accounts = accountNumbers.length
    ? await Account.findAll({ where: { accountNumber: { [Op.in]: accountNumbers } }, attributes: ["id", "accountNumber"] })
    : [];
  const accountIdByNumber = new Map(accounts.map((a) => [String(a.accountNumber), Number(a.id)]));

  return rows.map((t) => ({
    id: t.id,
    accountId: Number(t.accountId) || accountIdByNumber.get(String(t.accountNumber)) || null,
    accountNumber: t.accountNumber,
    kind: t.type,
    amount: Number(t.amount),
    description: t.description,
    status: t.status,
    suspicious: Number(t.amount) >= getHighValueTransferThreshold() || String(t.status || "").toLowerCase() === "reversed",
    createdAt: t.createdAt,
  }));
}

async function resolveBillAccountFromPayload(payload) {
  const accountNumber = String(payload?.accountNumber || "").trim();
  if (accountNumber) {
    const byNumber = await Account.findOne({ where: { accountNumber } });
    if (!byNumber) {
      throw new Error("Account not found");
    }
    return byNumber;
  }

  const accountId = Number(payload?.accountId);
  if (!Number.isFinite(accountId) || accountId <= 0) {
    throw new Error("Valid accountNumber or accountId is required");
  }
  const byId = await Account.findByPk(accountId);
  if (!byId) {
    throw new Error("Account not found");
  }
  return byId;
}

router.get("/health", (req, res) => {
  res.json({ status: "ok", service: "BoF Banking API", at: new Date().toISOString() });
});

router.post("/activity", requireAuth, asyncHandler(async (req, res) => {
  const userId = getAuthenticatedCustomerId(req);
  if (!Number.isFinite(userId) || userId <= 0) {
    return res.status(401).json({ error: "Authenticated customer session required" });
  }

  const payload = req.body || {};
  const activityType = normalizeActivityType(payload.activityType || "CLIENT_EVENT");
  const description = String(payload.description || "User activity").trim();
  const status = String(payload.status || "success").trim().toLowerCase();

  if (!description) {
    return res.status(400).json({ error: "description is required" });
  }

  const row = await ActivityLog.create({
    userId,
    activityType,
    description,
    status: status || "success",
  });

  res.status(201).json({
    id: row.id,
    user_id: row.userId,
    activity_type: row.activityType,
    description: row.description,
    timestamp: row.timestamp,
    status: row.status,
  });
}));

router.get("/activity", requireAuth, asyncHandler(async (req, res) => {
  const userId = getAuthenticatedCustomerId(req);
  if (!Number.isFinite(userId) || userId <= 0) {
    return res.status(401).json({ error: "Authenticated customer session required" });
  }

  const fromDate = String(req.query.fromDate || "").trim();
  const toDate = String(req.query.toDate || "").trim();
  const activityType = String(req.query.activityType || "").trim();
  const limit = Math.min(Math.max(Number(req.query.limit || 200), 1), 1000);

  const where = { userId };

  if (activityType) {
    where.activityType = normalizeActivityType(activityType);
  }

  if (fromDate || toDate) {
    const range = {};
    if (fromDate) {
      const parsed = new Date(fromDate);
      if (Number.isNaN(parsed.getTime())) {
        return res.status(400).json({ error: "fromDate must be a valid date" });
      }
      range[Op.gte] = parsed;
    }
    if (toDate) {
      const parsed = new Date(toDate);
      if (Number.isNaN(parsed.getTime())) {
        return res.status(400).json({ error: "toDate must be a valid date" });
      }
      parsed.setHours(23, 59, 59, 999);
      range[Op.lte] = parsed;
    }
    where.timestamp = range;
  }

  const rows = await ActivityLog.findAll({
    where,
    order: [["timestamp", "DESC"]],
    limit,
  });

  res.json(rows.map((row) => ({
    id: row.id,
    user_id: row.userId,
    activity_type: row.activityType,
    description: row.description,
    timestamp: row.timestamp,
    status: row.status,
  })));
}));

router.get("/report", requireAuth, asyncHandler(async (req, res) => {
  const customerId = getAuthenticatedCustomerId(req);
  if (!Number.isFinite(customerId) || customerId <= 0) {
    return res.status(401).json({ error: "Authenticated customer session required" });
  }

  const customer = await Customer.findByPk(customerId);
  if (!customer) {
    return res.status(404).json({ error: "Customer not found" });
  }

  const accounts = await Account.findAll({
    where: { customerId },
    attributes: ["accountNumber", "balance"],
    order: [["id", "ASC"]],
  });

  const accountNumbers = accounts.map((x) => String(x.accountNumber || "")).filter(Boolean);

  const transactions = accountNumbers.length
    ? await Transaction.findAll({
        where: {
          accountNumber: { [Op.in]: accountNumbers },
        },
        order: [["createdAt", "ASC"]],
      })
    : [];

  const bucket = new Map();
  transactions.forEach((tx) => {
    const txDate = new Date(tx.date || tx.createdAt);
    const period = Number.isNaN(txDate.getTime())
      ? "unknown"
      : `${txDate.getFullYear()}-${String(txDate.getMonth() + 1).padStart(2, "0")}`;

    if (!bucket.has(period)) {
      bucket.set(period, { period, credit: 0, debit: 0, total: 0 });
    }

    const row = bucket.get(period);
    const amount = Math.abs(Number(tx.amount || 0));
    const type = String(tx.transactionType || tx.type || "").toLowerCase();
    const isCredit = type.includes("credit") || type.includes("deposit") || type.includes("receive") || type.includes("income");

    if (isCredit) {
      row.credit += amount;
      row.total += amount;
    } else {
      row.debit += amount;
      row.total -= amount;
    }
  });

  const points = Array.from(bucket.values())
    .sort((a, b) => String(a.period).localeCompare(String(b.period)))
    .map((x) => ({
      period: x.period,
      credit: Number(x.credit.toFixed(2)),
      debit: Number(x.debit.toFixed(2)),
      total: Number(x.total.toFixed(2)),
    }));

  const totalBalance = accounts.reduce((sum, acc) => sum + Number(acc.balance || 0), 0);

  res.json({
    accountOverview: {
      customerName: customer.fullName,
      accountNumber: accountNumbers[0] || "N/A",
      currentBalance: Number(totalBalance.toFixed(2)),
      totalTransactions: transactions.length,
    },
    points,
  });
}));

router.post("/statement", requireAuth, asyncHandler(async (req, res) => {
  const statement = await resolveStatementPayloadForUser(req);
  res.json({
    bankName: "Bank of Fiji",
    customerName: statement.customer.fullName,
    accountNumber: statement.accountNumber,
    dateRange: {
      fromDate: statement.fromDate.toISOString(),
      toDate: statement.toDate.toISOString(),
    },
    transactions: statement.transactions,
  });
}));

router.post("/statement/download", requireAuth, asyncHandler(async (req, res) => {
  const statement = await resolveStatementPayloadForUser(req);

  const doc = new PDFDocument({ margin: 40, size: "A4" });
  const filenameDate = new Date().toISOString().slice(0, 10);
  res.setHeader("Content-Type", "application/pdf");
  res.setHeader(
    "Content-Disposition",
    `attachment; filename=bank-statement-${filenameDate}.pdf`
  );

  doc.pipe(res);

  doc.fontSize(18).text("Bank of Fiji", { align: "center" });
  doc.moveDown(0.6);
  doc.fontSize(14).text("Bank Statement", { align: "center" });
  doc.moveDown(1.2);

  doc.fontSize(11).text(`Customer Name: ${statement.customer.fullName}`);
  doc.text(`Primary Account Number: ${statement.accountNumber}`);
  doc.text(`Accounts Included: ${statement.accountNumbers.join(", ")}`);
  doc.text(`Date Range: ${statement.fromDate.toISOString().slice(0, 10)} to ${statement.toDate.toISOString().slice(0, 10)}`);
  doc.moveDown(1);

  if (!statement.transactions.length) {
    doc
      .fontSize(12)
      .fillColor("#555555")
      .text("No transactions found for the selected period", { align: "left" });
    doc.moveDown(0.6);
    doc.fillColor("#000000");
  } else {
    const columns = {
      date: 40,
      description: 140,
      amount: 360,
      balance: 455,
    };

    doc.fontSize(10).text("Date", columns.date, doc.y);
    doc.text("Description", columns.description, doc.y);
    doc.text("Amount", columns.amount, doc.y, { width: 80, align: "right" });
    doc.text("Balance", columns.balance, doc.y, { width: 90, align: "right" });
    doc.moveDown(0.3);
    doc.moveTo(40, doc.y).lineTo(555, doc.y).stroke();
    doc.moveDown(0.4);

    statement.transactions.forEach((row) => {
      if (doc.y > 760) {
        doc.addPage();
      }

      const txDate = new Date(row.date).toISOString().slice(0, 10);
      const amountText = Number(row.amount).toFixed(2);
      const balanceText = Number(row.balance).toFixed(2);
      const descriptionText = `${row.transactionType || "transaction"}: ${row.description || "-"}`;

      doc.text(txDate, columns.date, doc.y, { width: 90 });
      doc.text(descriptionText, columns.description, doc.y, { width: 205 });
      doc.text(amountText, columns.amount, doc.y, { width: 80, align: "right" });
      doc.text(balanceText, columns.balance, doc.y, { width: 90, align: "right" });
      doc.moveDown(0.4);
    });
  }

  doc.end();
}));

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
  const startedAt = Date.now();
  const requestedCustomerId = Number(req.query.customerId);
  const fallbackCustomerId = getAuthenticatedCustomerId(req);
  const customerId = Number.isFinite(requestedCustomerId) && requestedCustomerId > 0
    ? requestedCustomerId
    : fallbackCustomerId;

  if (!Number.isFinite(customerId) || customerId <= 0) {
    return res.status(400).json({ error: "Valid customerId query is required" });
  }
  if (!canAccessCustomer(req, customerId)) {
    return res.status(403).json({ error: "Forbidden" });
  }

  const result = await Promise.race([
    getDashboard(customerId),
    new Promise((_, reject) => {
      setTimeout(() => reject(new Error("Dashboard request timed out")), 8000);
    }),
  ]);

  console.info(`[dashboard] customerId=${customerId} loaded in ${Date.now() - startedAt}ms`);
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
  let rows = [];

  if (isAdmin(req)) {
    rows = await Account.findAll({ order: [["createdAt", "ASC"]] });
  } else {
    const authenticatedCustomerId = getAuthenticatedCustomerId(req);
    const customerIds = new Set();
    if (Number.isFinite(authenticatedCustomerId) && authenticatedCustomerId > 0) {
      customerIds.add(Number(authenticatedCustomerId));
    }

    if (customerIds.size > 0) {
      rows = await Account.findAll({ where: { customerId: { [Op.in]: Array.from(customerIds) } }, order: [["createdAt", "ASC"]] });
    }

    // Legacy compatibility: include accounts tied to duplicate customer rows that share the same identity.
    if (rows.length === 0) {
      const tokenEmail = String(req.auth?.email || "").trim().toLowerCase();
      const tokenFullName = String(req.auth?.fullName || "").trim();

      let authCustomer = null;
      if (Number.isFinite(authenticatedCustomerId) && authenticatedCustomerId > 0) {
        authCustomer = await Customer.findByPk(authenticatedCustomerId);
      }
      if (!authCustomer && tokenEmail) {
        authCustomer = await Customer.findOne({ where: { email: tokenEmail } });
      }
      if (!authCustomer && tokenFullName) {
        authCustomer = await Customer.findOne({ where: { fullName: tokenFullName } });
      }

      const normalizedEmail = String(authCustomer?.email || "").trim().toLowerCase();
      const normalizedMobile = String(authCustomer?.mobile || "").trim();
      const normalizedFullName = String(authCustomer?.fullName || tokenFullName || "").trim();

      const identityFilters = [];
      if (tokenEmail) identityFilters.push({ email: tokenEmail });
      if (normalizedEmail && normalizedEmail !== tokenEmail) identityFilters.push({ email: normalizedEmail });
      if (normalizedMobile) identityFilters.push({ mobile: normalizedMobile });
      if (normalizedFullName) identityFilters.push({ fullName: normalizedFullName });

      if (identityFilters.length > 0) {
        const relatedCustomers = await Customer.findAll({
          attributes: ["id"],
          where: { [Op.or]: identityFilters },
        });
        relatedCustomers.forEach((c) => customerIds.add(Number(c.id)));
      }

      const accountOrFilters = [];
      if (customerIds.size > 0) {
        accountOrFilters.push({ customerId: { [Op.in]: Array.from(customerIds) } });
      }
      if (normalizedFullName) {
        accountOrFilters.push(Customer.sequelize.where(
          Customer.sequelize.fn("LOWER", Customer.sequelize.fn("TRIM", Customer.sequelize.col("accountHolder"))),
          normalizedFullName.toLowerCase()
        ));
      }

      if (accountOrFilters.length > 0) {
        rows = await Account.findAll({
          where: { [Op.or]: accountOrFilters },
          order: [["createdAt", "ASC"]],
        });
      }

      // Final fallback: normalize and match via related customer identity + account holder text.
      if (rows.length === 0) {
        const normalizedIdentity = {
          email: tokenEmail || normalizedEmail,
          mobile: normalizedMobile,
          fullName: normalizedFullName.toLowerCase(),
        };

        const candidates = await Account.findAll({
          include: [{ model: Customer, attributes: ["id", "email", "mobile", "fullName"] }],
          order: [["createdAt", "ASC"]],
        });

        const matchesIdentity = (account) => {
          const customer = account.Customer || {};
          const customerEmail = String(customer.email || "").trim().toLowerCase();
          const customerMobile = String(customer.mobile || "").trim();
          const customerName = String(customer.fullName || "").trim().toLowerCase();
          const holderName = String(account.accountHolder || "").trim().toLowerCase();

          if (normalizedIdentity.email && customerEmail && customerEmail === normalizedIdentity.email) return true;
          if (normalizedIdentity.mobile && customerMobile && customerMobile === normalizedIdentity.mobile) return true;
          if (normalizedIdentity.fullName && customerName && customerName === normalizedIdentity.fullName) return true;
          if (normalizedIdentity.fullName && holderName && holderName === normalizedIdentity.fullName) return true;

          return false;
        };

        rows = candidates.filter(matchesIdentity);
      }
    }
  }

  res.json(rows.map(toAccountResponse));
}));

// DEBUG ENDPOINT: Show token identity and matched accounts (remove after diagnosing)
router.get("/debug/my-accounts", requireAuth, asyncHandler(async (req, res) => {
  const authenticatedCustomerId = getAuthenticatedCustomerId(req);
  const tokenEmail = String(req.auth?.email || "").trim().toLowerCase();
  const tokenFullName = String(req.auth?.fullName || "").trim();
  
  let authCustomer = null;
  let resolveMethod = "none";
  
  if (Number.isFinite(authenticatedCustomerId) && authenticatedCustomerId > 0) {
    authCustomer = await Customer.findByPk(authenticatedCustomerId);
    resolveMethod = "customerId";
  }
  if (!authCustomer && tokenEmail) {
    authCustomer = await Customer.findOne({ where: { email: tokenEmail } });
    resolveMethod = "email";
  }
  if (!authCustomer && tokenFullName) {
    authCustomer = await Customer.findOne({ where: { fullName: tokenFullName } });
    resolveMethod = "fullName";
  }
  
  const allAccounts = await Account.findAll({
    include: [{ model: Customer, attributes: ["id", "email", "mobile", "fullName"] }],
  });
  
  const matchedAccounts = allAccounts.filter((account) => {
    const customer = account.Customer || {};
    const customerEmail = String(customer.email || "").trim().toLowerCase();
    const customerMobile = String(customer.mobile || "").trim();
    const customerName = String(customer.fullName || "").trim().toLowerCase();
    const holderName = String(account.accountHolder || "").trim().toLowerCase();
    const normalizedFullName = (tokenFullName || authCustomer?.fullName || "").trim().toLowerCase();
    
    if (account.customerId === authenticatedCustomerId) return true;
    if (tokenEmail && customerEmail && customerEmail === tokenEmail) return true;
    if (customerMobile && customerMobile === String(authCustomer?.mobile || "").trim()) return true;
    if (normalizedFullName && customerName && customerName === normalizedFullName) return true;
    if (normalizedFullName && holderName && holderName === normalizedFullName) return true;
    return false;
  });
  
  res.json({
    token: {
      userId: req.auth?.userId,
      customerId: req.auth?.customerId,
      email: req.auth?.email,
      fullName: req.auth?.fullName,
      isAdmin: req.auth?.isAdmin,
    },
    authenticatedCustomerId,
    resolvedCustomer: authCustomer ? {
      id: authCustomer.id,
      fullName: authCustomer.fullName,
      email: authCustomer.email,
      mobile: authCustomer.mobile,
    } : null,
    resolveMethod,
    totalAccounts: allAccounts.length,
    matchedAccountCount: matchedAccounts.length,
    matchedAccounts: matchedAccounts.map((a) => ({
      id: a.id,
      customerId: a.customerId,
      accountNumber: a.accountNumber,
      accountHolder: a.accountHolder,
      accountType: a.accountType,
      status: a.status,
      balance: a.balance,
      createdAt: a.createdAt,
    })),
  });
}));

router.get("/accounts/lookup", requireAuth, asyncHandler(async (req, res) => {
  const accountNumber = String(req.query.accountNumber || "").trim();
  const purpose = String(req.query.purpose || "source").trim().toLowerCase();

  if (!/^\d{12}$/.test(accountNumber)) {
    return res.status(400).json({ error: "accountNumber must be a 12-digit value" });
  }

  if (!["source", "destination"].includes(purpose)) {
    return res.status(400).json({ error: "purpose must be source or destination" });
  }

  const account = await Account.findOne({
    where: { accountNumber },
    include: [{ model: Customer, attributes: ["id", "fullName"] }],
  });

  if (!account) {
    return res.status(404).json({ error: "Account not found" });
  }

  if (!isAdmin(req) && purpose === "source" && Number(account.customerId) !== getAuthenticatedCustomerId(req)) {
    return res.status(403).json({ error: "Forbidden" });
  }

  if (!isAdmin(req) && purpose === "destination") {
    const status = String(account.status || "").toLowerCase();
    if (status !== "active") {
      return res.status(404).json({ error: "Destination account not available" });
    }
  }

  res.json({
    accountId: Number(account.id),
    accountNumber: account.accountNumber,
    accountHolder: account.accountHolder || account.Customer?.fullName || "",
    accountType: account.accountType,
    status: account.status,
  });
}));

router.post("/accounts", requireAuth, requireAdmin, asyncHandler(async (req, res) => {
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

  const customer = await getCustomerForAccountPayload(payload);

  const account = await Account.create({
    customerId: customer.id,
    accountNumber: providedAccountNumber || await generateRandomAccountNumber(),
    accountPin: generateRandomAccountPin(),
    accountHolder: customer.fullName,
    accountType: payload.type,
    balance: Number(payload.openingBalance || 0),
    currency: "FJD",
    status: "active",
  });

  res.status(201).json(toAccountResponse(account));
}));

router.post("/accounts/request", requireAuth, asyncHandler(async (req, res) => {
  const payload = req.body || {};
  if (!payload.type) {
    return res.status(400).json({ error: "type is required" });
  }
  if (!["Simple Access", "Savings", "Current"].includes(payload.type)) {
    return res.status(400).json({ error: "type must be Simple Access, Current or Savings" });
  }

  const authenticatedCustomerId = getAuthenticatedCustomerId(req);
  const requestedCustomerId = Number(payload.customerId);
  const customerId = isAdmin(req)
    ? requestedCustomerId
    : authenticatedCustomerId;

  if (!Number.isFinite(customerId) || customerId <= 0) {
    return res.status(400).json({ error: "Valid customerId is required" });
  }

  const customer = await Customer.findByPk(customerId);
  if (!customer) {
    return res.status(404).json({ error: "Customer not found" });
  }

  if (!canAccessCustomer(req, customerId)) {
    return res.status(403).json({ error: "Forbidden" });
  }

  const providedAccountNumber = String(payload.accountNumber || "").trim();
  if (providedAccountNumber && !/^\d{12}$/.test(providedAccountNumber)) {
    return res.status(400).json({ error: "Reenter 12 digit number" });
  }

  const account = await Account.create({
    customerId,
    accountNumber: providedAccountNumber || await generateRandomAccountNumber(),
    accountPin: generateRandomAccountPin(),
    accountHolder: customer.fullName,
    accountType: payload.type,
    requestedOpeningBalance: Number(payload.openingBalance || 0),
    approvedOpeningBalance: null,
    approvedByAdminId: null,
    approvedAt: null,
    rejectionReason: null,
    balance: 0,
    currency: "FJD",
    status: "pending_approval",
  });

  res.status(201).json(toAccountResponse(account));
}));

router.patch("/admin/accounts/:id/approve", asyncHandler(async (req, res) => {
  const account = await Account.findByPk(req.params.id);
  if (!account) {
    return res.status(404).json({ error: "Account not found" });
  }

  const approvedOpeningBalance = Number(req.body?.approvedOpeningBalance);
  if (!Number.isFinite(approvedOpeningBalance) || approvedOpeningBalance < 0) {
    return res.status(400).json({ error: "Valid approvedOpeningBalance is required" });
  }

  await account.update({
    status: "active",
    balance: approvedOpeningBalance,
    approvedOpeningBalance,
    approvedByAdminId: Number(req.auth?.userId || 0) || null,
    approvedAt: new Date(),
    rejectionReason: null,
  });

  res.json(toAccountResponse(account));
}));

router.patch("/admin/accounts/:id/reject", asyncHandler(async (req, res) => {
  const account = await Account.findByPk(req.params.id);
  if (!account) {
    return res.status(404).json({ error: "Account not found" });
  }

  const reason = String(req.body?.rejectionReason || "").trim();
  if (!reason) {
    return res.status(400).json({ error: "rejectionReason is required" });
  }

  await account.update({
    status: "rejected",
    approvedOpeningBalance: null,
    approvedByAdminId: Number(req.auth?.userId || 0) || null,
    approvedAt: new Date(),
    rejectionReason: reason,
    balance: 0,
  });

  res.json(toAccountResponse(account));
}));

router.patch("/admin/accounts/:id", asyncHandler(async (req, res) => {
  const account = await Account.findByPk(req.params.id);
  if (!account) {
    return res.status(404).json({ error: "Account not found" });
  }

  const payload = req.body || {};
  const updates = {};
  if (payload.type !== undefined) {
    if (!["Simple Access", "Savings", "Current"].includes(payload.type)) {
      return res.status(400).json({ error: "type must be Simple Access, Current or Savings" });
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
    allowAutoCreate: false,
    customerDefaults: {
      nationalId: `AUTO-${Date.now()}`,
      emailVerified: true,
    },
  });

  const account = await Account.create({
    customerId: customer.id,
    accountNumber: providedAccountNumber || await generateRandomAccountNumber(),
    accountPin: generateRandomAccountPin(),
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
  let accountNumberFilter = null;

  if (accountNumber) {
    const account = await Account.findOne({ where: { accountNumber } });
    if (!account) {
      return res.json([]);
    }
    accountNumberFilter = account.accountNumber;
  }

  const rows = await Transaction.findAll({
    where: accountNumberFilter ? { accountNumber: accountNumberFilter } : undefined,
    order: [["createdAt", "DESC"]],
    limit: 500,
  });

  res.json(await mapTransactionRows(rows));
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

router.get("/transactions", requireAuth, asyncHandler(async (req, res) => {
  const accountId = String(req.query.accountId || "").trim();
  const accountNumber = String(req.query.accountNumber || "").trim();
  if (!accountId && !accountNumber) {
    return res.status(400).json({ error: "accountId or accountNumber query is required" });
  }
  const account = accountId
    ? await Account.findByPk(Number(accountId))
    : await Account.findOne({ where: { accountNumber } });

  if (!account) {
    return res.status(404).json({ error: "Account not found" });
  }

  if (!isAdmin(req) && Number(account.customerId) !== getAuthenticatedCustomerId(req)) {
    return res.status(403).json({ error: "Forbidden" });
  }

  const rows = await getAccountTransactions(account.id);
  const mappedRows = await mapTransactionRows(rows);
  res.json(mappedRows.map((row) => ({
    ...row,
    counterpartyAccountId: null,
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

router.post("/transfers/validate-destination", requireAuth, asyncHandler(async (req, res) => {
  const payload = req.body || {};
  const accountNumber = String(payload.toAccountNumber || "").trim();
  const fromAccountId = Number(payload.fromAccountId || 0);

  if (!fromAccountId) {
    return res.status(400).json({ error: "fromAccountId is required" });
  }
  if (!/^\d{12}$/.test(accountNumber)) {
    return res.status(400).json({ error: "Destination account number must be 12 digits" });
  }

  const fromAccount = await Account.findByPk(fromAccountId);
  if (!fromAccount) {
    return res.status(404).json({ error: "Source account not found" });
  }
  if (!isAdmin(req) && fromAccount.customerId !== getAuthenticatedCustomerId(req)) {
    return res.status(403).json({ error: "Forbidden" });
  }

  const destination = await Account.findOne({
    where: { accountNumber },
    include: [{ model: Customer, attributes: ["id", "fullName"] }],
  });

  if (!destination) {
    return res.status(404).json({ error: "Destination account not found" });
  }
  if (destination.id === fromAccount.id) {
    return res.status(400).json({ error: "Destination account must be different from source account" });
  }

  res.json({
    accountId: destination.id,
    accountNumber: destination.accountNumber,
    customerId: destination.customerId,
    customerName: destination.Customer?.fullName || destination.accountHolder || "Unknown customer",
  });
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

  const mapped = await mapTransactionRows(rows);
  res.json(mapped.map((row) => ({
    id: row.id,
    accountId: row.accountId,
    accountNumber: row.accountNumber,
    kind: row.kind,
    amount: row.amount,
    description: row.description,
    createdAt: row.createdAt,
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

  const recentTransactionRows = (await mapTransactionRows(latestTransactions)).map((row) => ({
    id: row.id,
    accountId: row.accountId,
    accountNumber: row.accountNumber,
    kind: row.kind,
    amount: row.amount,
    description: row.description,
    createdAt: row.createdAt,
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
  const account = await resolveBillAccountFromPayload(payload);
  if (!isAdmin(req) && account.customerId !== getAuthenticatedCustomerId(req)) {
    return res.status(403).json({ error: "Forbidden" });
  }
  const payment = await postBillPayment({
    accountId: account.id,
    payee: payload.payee,
    amount: Number(payload.amount),
    mode: "manual",
  });
  res.status(201).json({ ...payment, accountNumber: account.accountNumber });
}));

router.post("/bill-payment", requireAuth, asyncHandler(async (req, res) => {
  const payload = req.body || {};
  const account = await resolveBillAccountFromPayload(payload);
  if (!isAdmin(req) && account.customerId !== getAuthenticatedCustomerId(req)) {
    return res.status(403).json({ error: "Forbidden" });
  }
  const payment = await postBillPayment({
    accountId: account.id,
    payee: payload.payee,
    amount: Number(payload.amount),
    mode: payload.mode || "manual",
    scheduledDate: payload.scheduledDate || null,
  });
  res.status(201).json({ ...payment, accountNumber: account.accountNumber });
}));

router.post("/pay-bill", requireAuth, asyncHandler(async (req, res) => {
  const payload = req.body || {};
  const account = await resolveBillAccountFromPayload(payload);
  if (!isAdmin(req) && account.customerId !== getAuthenticatedCustomerId(req)) {
    return res.status(403).json({ error: "Forbidden" });
  }
  const payment = await postBillPayment({
    accountId: account.id,
    payee: payload.payee,
    amount: Number(payload.amount),
    mode: payload.mode || "manual",
    scheduledDate: payload.scheduledDate || null,
  });
  res.status(201).json({ ...payment, accountNumber: account.accountNumber });
}));

router.post("/bills/scheduled", requireAuth, asyncHandler(async (req, res) => {
  const payload = req.body || {};
  const account = await resolveBillAccountFromPayload(payload);
  if (!isAdmin(req) && account.customerId !== getAuthenticatedCustomerId(req)) {
    return res.status(403).json({ error: "Forbidden" });
  }
  const row = await scheduleBillPayment({
    accountId: account.id,
    payee: payload.payee,
    amount: Number(payload.amount),
    scheduledDate: payload.scheduledDate,
  });
  res.status(201).json({ ...row, accountNumber: account.accountNumber });
}));

router.post("/schedule-payment", requireAuth, asyncHandler(async (req, res) => {
  const payload = req.body || {};
  const account = await resolveBillAccountFromPayload(payload);
  if (!isAdmin(req) && account.customerId !== getAuthenticatedCustomerId(req)) {
    return res.status(403).json({ error: "Forbidden" });
  }
  const row = await scheduleBillPayment({
    accountId: account.id,
    payee: payload.payee,
    amount: Number(payload.amount),
    scheduledDate: payload.scheduledDate,
  });
  res.status(201).json({ ...row, accountNumber: account.accountNumber });
}));

router.get("/bills/scheduled", requireAuth, asyncHandler(async (req, res) => {
  const where = isAdmin(req)
    ? { status: "scheduled" }
    : { status: "scheduled", customerId: getAuthenticatedCustomerId(req) };
  const rows = await Bill.findAll({ where, order: [["createdAt", "DESC"]] });
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

router.get("/bills/history", requireAuth, asyncHandler(async (req, res) => {
  const where = isAdmin(req)
    ? { status: "paid" }
    : { status: "paid", customerId: getAuthenticatedCustomerId(req) };

  const rows = await Bill.findAll({ where, order: [["createdAt", "DESC"]] });
  res.json(
    rows.map((b) => ({
      id: b.id,
      accountId: parsePaidBillAccountId(b.description),
      customerId: b.customerId,
      payee: b.billType,
      amount: Number(b.amount),
      scheduledDate: b.dueDate,
      status: b.status,
      createdAt: b.createdAt,
      description: b.description,
    }))
  );
}));

router.post("/bills/scheduled/:id/run", requireAuth, asyncHandler(async (req, res) => {
  const scheduled = await Bill.findByPk(req.params.id);
  if (!scheduled) {
    return res.status(404).json({ error: "Scheduled payment not found" });
  }
  if (!isAdmin(req) && Number(scheduled.customerId) !== getAuthenticatedCustomerId(req)) {
    return res.status(403).json({ error: "Forbidden" });
  }
  const result = await runScheduledPayment(req.params.id);
  res.json(result);
}));

router.get("/investments", requireAuth, asyncHandler(async (req, res) => {
  const requestedCustomerId = Number(req.query.customerId || 0);
  const customerId = requestedCustomerId > 0 ? requestedCustomerId : getAuthenticatedCustomerId(req);
  if (!canAccessCustomer(req, customerId)) {
    return res.status(403).json({ error: "Forbidden" });
  }

  const rows = await Investment.findAll({ where: { customerId }, order: [["createdAt", "DESC"]] });
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

router.post("/investments", requireAuth, asyncHandler(async (req, res) => {
  const payload = req.body || {};
  const customerId = Number(payload.customerId || 0);
  if (!Number.isFinite(customerId) || customerId <= 0) {
    return res.status(400).json({ error: "Valid customerId is required" });
  }
  if (!canAccessCustomer(req, customerId)) {
    return res.status(403).json({ error: "Forbidden" });
  }

  const row = await createInvestment({
    customerId,
    name: payload.name,
    amount: Number(payload.amount),
    annualRate: Number(payload.annualRate),
  });
  res.status(201).json(row);
}));

router.post("/investment", requireAuth, asyncHandler(async (req, res) => {
  const payload = req.body || {};
  const customerId = getAuthenticatedCustomerId(req);
  if (!Number.isFinite(customerId) || customerId <= 0) {
    return res.status(401).json({ error: "Authenticated customer session required" });
  }

  const amount = Number(payload.amount);
  const investmentType = String(payload.investmentType || "").trim();
  const durationMonths = Number(payload.durationMonths || 0);
  const notes = String(payload.notes || "").trim();

  if (!investmentType) {
    return res.status(400).json({ error: "investmentType is required" });
  }
  if (!Number.isFinite(amount) || amount <= 0) {
    return res.status(400).json({ error: "amount must be greater than 0" });
  }
  if (!Number.isFinite(durationMonths) || durationMonths <= 0) {
    return res.status(400).json({ error: "durationMonths must be greater than 0" });
  }

  const maturityDate = new Date();
  maturityDate.setMonth(maturityDate.getMonth() + durationMonths);

  const row = await Investment.create({
    customerId,
    investmentType,
    amount,
    expectedReturn: null,
    maturityDate,
    notes: notes || null,
    status: "pending",
  });

  res.status(201).json({
    id: row.id,
    customerId: row.customerId,
    investmentType: row.investmentType,
    amount: Number(row.amount),
    durationMonths,
    notes: row.notes,
    status: row.status,
    createdAt: row.createdAt,
  });
}));

router.get("/admin/investments", requireAuth, requireAdmin, asyncHandler(async (req, res) => {
  const customerId = Number(req.query.customerId || 0);
  const status = String(req.query.status || "").trim().toLowerCase();

  const where = {};
  if (Number.isFinite(customerId) && customerId > 0) {
    where.customerId = customerId;
  }
  if (status) {
    where.status = status;
  }

  const rows = await Investment.findAll({
    where,
    include: [{ model: Customer, attributes: ["id", "fullName"] }],
    order: [["createdAt", "DESC"]],
    limit: 500,
  });

  res.json(rows.map(toInvestmentResponse));
}));

router.post("/admin/investments", requireAuth, requireAdmin, asyncHandler(async (req, res) => {
  const payload = req.body || {};
  const customer = await getCustomerForAccountPayload(payload, { allowAutoCreate: false });

  const amount = Number(payload.amount);
  const investmentType = String(payload.investmentType || "").trim();
  const expectedReturn = payload.expectedReturn === undefined || payload.expectedReturn === null || String(payload.expectedReturn).trim() === ""
    ? null
    : Number(payload.expectedReturn);
  const maturityDate = payload.maturityDate ? new Date(payload.maturityDate) : null;
  const status = String(payload.status || "pending").trim().toLowerCase();

  if (!investmentType) {
    return res.status(400).json({ error: "investmentType is required" });
  }
  if (!Number.isFinite(amount) || amount <= 0) {
    return res.status(400).json({ error: "amount must be greater than 0" });
  }
  if (expectedReturn !== null && !Number.isFinite(expectedReturn)) {
    return res.status(400).json({ error: "expectedReturn must be a number when provided" });
  }
  if (maturityDate && Number.isNaN(maturityDate.getTime())) {
    return res.status(400).json({ error: "maturityDate must be a valid date" });
  }

  const row = await Investment.create({
    customerId: customer.id,
    investmentType,
    amount,
    expectedReturn,
    maturityDate,
    notes: payload.notes ? String(payload.notes).trim() : null,
    status,
  });

  const saved = await Investment.findByPk(row.id, {
    include: [{ model: Customer, attributes: ["id", "fullName"] }],
  });

  res.status(201).json(toInvestmentResponse(saved));
}));

router.patch("/admin/investments/:id", requireAuth, requireAdmin, asyncHandler(async (req, res) => {
  const row = await Investment.findByPk(req.params.id);
  if (!row) {
    return res.status(404).json({ error: "Investment not found" });
  }

  const payload = req.body || {};
  const updates = {};

  if (payload.investmentType !== undefined) {
    const investmentType = String(payload.investmentType || "").trim();
    if (!investmentType) {
      return res.status(400).json({ error: "investmentType cannot be blank" });
    }
    updates.investmentType = investmentType;
  }

  if (payload.amount !== undefined) {
    const amount = Number(payload.amount);
    if (!Number.isFinite(amount) || amount <= 0) {
      return res.status(400).json({ error: "amount must be greater than 0" });
    }
    updates.amount = amount;
  }

  if (payload.expectedReturn !== undefined) {
    if (payload.expectedReturn === null || String(payload.expectedReturn).trim() === "") {
      updates.expectedReturn = null;
    } else {
      const expectedReturn = Number(payload.expectedReturn);
      if (!Number.isFinite(expectedReturn)) {
        return res.status(400).json({ error: "expectedReturn must be a number" });
      }
      updates.expectedReturn = expectedReturn;
    }
  }

  if (payload.maturityDate !== undefined) {
    if (payload.maturityDate === null || String(payload.maturityDate).trim() === "") {
      updates.maturityDate = null;
    } else {
      const maturityDate = new Date(payload.maturityDate);
      if (Number.isNaN(maturityDate.getTime())) {
        return res.status(400).json({ error: "maturityDate must be a valid date" });
      }
      updates.maturityDate = maturityDate;
    }
  }

  if (payload.status !== undefined) {
    const status = String(payload.status || "").trim().toLowerCase();
    if (!status) {
      return res.status(400).json({ error: "status cannot be blank" });
    }
    updates.status = status;
  }

  if (payload.notes !== undefined) {
    updates.notes = payload.notes == null ? null : String(payload.notes).trim();
  }

  if (!Object.keys(updates).length) {
    return res.status(400).json({ error: "No valid update fields provided" });
  }

  await row.update(updates);

  const saved = await Investment.findByPk(row.id, {
    include: [{ model: Customer, attributes: ["id", "fullName"] }],
  });

  res.json(toInvestmentResponse(saved));
}));

router.post("/loan", requireAuth, asyncHandler(async (req, res) => {
  const payload = req.body || {};
  const customerId = getAuthenticatedCustomerId(req);
  if (!Number.isFinite(customerId) || customerId <= 0) {
    return res.status(401).json({ error: "Authenticated customer session required" });
  }

  const loanAmount = Number(payload.loanAmount);
  const loanType = String(payload.loanType || "").trim();
  const repaymentPeriodMonths = Number(payload.repaymentPeriodMonths || 0);
  const purpose = String(payload.purpose || "").trim();
  const details = String(payload.details || "").trim();

  if (!loanType) {
    return res.status(400).json({ error: "loanType is required" });
  }
  if (!Number.isFinite(loanAmount) || loanAmount <= 0) {
    return res.status(400).json({ error: "loanAmount must be greater than 0" });
  }
  if (!Number.isFinite(repaymentPeriodMonths) || repaymentPeriodMonths <= 0) {
    return res.status(400).json({ error: "repaymentPeriodMonths must be greater than 0" });
  }
  if (!purpose) {
    return res.status(400).json({ error: "purpose is required" });
  }

  const maturityDate = new Date();
  maturityDate.setMonth(maturityDate.getMonth() + repaymentPeriodMonths);

  const sanitizedLoanType = loanType.toUpperCase().replace(/[^A-Z0-9]+/g, "_").replace(/^_+|_+$/g, "");
  const row = await Loan.create({
    customerId,
    loanProductId: `CUSTOM_${sanitizedLoanType || "LOAN"}`,
    termMonths: repaymentPeriodMonths,
    loanType,
    principal: loanAmount,
    interestRate: 0,
    disbursedAmount: 0,
    maturityDate,
    purpose,
    details: details || null,
    status: "pending",
  });

  res.status(201).json({
    id: row.id,
    customerId: row.customerId,
    loanType: row.loanType,
    loanAmount: Number(row.principal),
    repaymentPeriodMonths: Number(row.termMonths),
    purpose: row.purpose,
    details: row.details,
    status: row.status,
    createdAt: row.createdAt,
  });
}));

router.post("/statements/request", requireAuth, asyncHandler(async (req, res) => {
  const payload = req.body || {};
  const accountId = Number(payload.accountId);
  const requestedAccountNumber = String(payload.accountNumber || "").trim();
  const fromDate = String(payload.fromDate || "").trim();
  const toDate = String(payload.toDate || "").trim();

  if ((!Number.isFinite(accountId) || accountId <= 0) && !requestedAccountNumber) {
    return res.status(400).json({ error: "Valid accountId or accountNumber is required" });
  }
  if (!fromDate || !toDate) {
    return res.status(400).json({ error: "fromDate and toDate are required" });
  }
  if (new Date(fromDate).getTime() > new Date(toDate).getTime()) {
    return res.status(400).json({ error: "fromDate must be earlier than or equal to toDate" });
  }

  const account = (Number.isFinite(accountId) && accountId > 0)
    ? await Account.findByPk(accountId)
    : await Account.findOne({ where: { accountNumber: requestedAccountNumber } });
  if (!account) {
    return res.status(404).json({ error: "Account not found" });
  }

  if (!isAdmin(req) && account.customerId !== getAuthenticatedCustomerId(req)) {
    return res.status(403).json({ error: "Forbidden" });
  }

  const customer = await Customer.findByPk(account.customerId);
  if (!customer) {
    return res.status(404).json({ error: "Customer not found" });
  }

  const requestRow = await StatementRequest.create({
    customerId: customer.id,
    accountId: account.id,
    fullName: payload.fullName || customer.fullName,
    accountHolder: payload.accountHolder || account.accountHolder || customer.fullName,
    accountNumber: payload.accountNumber || account.accountNumber,
    fromDate,
    toDate,
    status: "pending",
  });

  res.status(201).json(toStatementRequestResponse(requestRow));
}));

router.get("/statements/requests", requireAuth, asyncHandler(async (req, res) => {
  const where = isAdmin(req) ? undefined : { customerId: getAuthenticatedCustomerId(req) };
  const rows = await StatementRequest.findAll({
    where,
    order: [["createdAt", "DESC"]],
    limit: 500,
  });

  res.json(rows.map(toStatementRequestResponse));
}));

router.get("/admin/statement-requests", requireAuth, requireAdmin, asyncHandler(async (req, res) => {
  const rows = await StatementRequest.findAll({
    order: [["createdAt", "DESC"]],
    limit: 500,
  });

  res.json(rows.map(toStatementRequestResponse));
}));

router.patch("/admin/statement-requests/:id", requireAuth, requireAdmin, asyncHandler(async (req, res) => {
  const payload = req.body || {};
  const status = String(payload.status || "").trim().toLowerCase();
  if (!status || !["approved", "rejected", "pending"].includes(status)) {
    return res.status(400).json({ error: "status must be approved, rejected or pending" });
  }

  const requestRow = await StatementRequest.findByPk(req.params.id);
  if (!requestRow) {
    return res.status(404).json({ error: "Statement request not found" });
  }

  const updates = {
    status,
    adminNote: payload.adminNote ? String(payload.adminNote).trim() : null,
    reviewedBy: req.auth?.email || "admin",
    reviewedByAdminId: Number(req.auth?.userId) || null,
    reviewedAt: new Date(),
  };

  if (status === "pending") {
    updates.reviewedBy = null;
    updates.reviewedByAdminId = null;
    updates.reviewedAt = null;
    updates.adminNote = null;
  }

  await requestRow.update(updates);
  res.json(toStatementRequestResponse(requestRow));
}));

async function resolveApprovedStatementRequest(req, res) {
  const requestId = Number(req.params.requestId);
  if (!Number.isFinite(requestId) || requestId <= 0) {
    res.status(400).json({ error: "Valid requestId is required" });
    return null;
  }

  const requestRow = await StatementRequest.findByPk(requestId);
  if (!requestRow) {
    res.status(404).json({ error: "Statement request not found" });
    return null;
  }

  if (!isAdmin(req) && requestRow.customerId !== getAuthenticatedCustomerId(req)) {
    res.status(403).json({ error: "Forbidden" });
    return null;
  }

  if (requestRow.status !== "approved") {
    res.status(403).json({ error: "Statement request is not approved yet" });
    return null;
  }

  return requestRow;
}

router.get("/statements/request/:requestId", requireAuth, asyncHandler(async (req, res) => {
  const requestRow = await resolveApprovedStatementRequest(req, res);
  if (!requestRow) {
    return;
  }
  const rows = await generateStatement(requestRow.accountNumber, requestRow.fromDate, requestRow.toDate);
  res.json(rows);
}));

router.get("/statements/request/:requestId/download", requireAuth, asyncHandler(async (req, res) => {
  const requestRow = await resolveApprovedStatementRequest(req, res);
  if (!requestRow) {
    return;
  }

  const rows = await generateStatement(requestRow.accountNumber, requestRow.fromDate, requestRow.toDate);
  const lines = ["transactionId,createdAt,kind,amount,description,counterparty"];
  rows.forEach((r) => {
    lines.push(
      `${r.id},${r.createdAt},${r.type || r.kind},${r.amount},"${(r.description || "").replace(/\"/g, '""')}",${r.counterpartyAccountId || ""}`
    );
  });

  const csv = lines.join("\n");
  res.setHeader("Content-Type", "text/csv");
  res.setHeader("Content-Disposition", `attachment; filename=statement-request-${requestRow.id}.csv`);
  res.send(csv);
}));

router.get("/statements/:accountId", requireAuth, asyncHandler(async (req, res) => {
  const accountId = Number(req.params.accountId);
  if (!Number.isFinite(accountId) || accountId <= 0) {
    return res.status(400).json({ error: "Valid accountId is required" });
  }

  const account = await Account.findByPk(accountId);
  if (!account) {
    return res.status(404).json({ error: "Account not found" });
  }

  if (!isAdmin(req) && account.customerId !== getAuthenticatedCustomerId(req)) {
    return res.status(403).json({ error: "Forbidden" });
  }

  const approvedRequest = await StatementRequest.findOne({
    where: {
      [Op.or]: [
        { accountId: account.id },
        { accountNumber: account.accountNumber },
      ],
      status: "approved",
      ...(isAdmin(req) ? {} : { customerId: getAuthenticatedCustomerId(req) }),
    },
    order: [["reviewedAt", "DESC"]],
  });

  if (!approvedRequest) {
    return res.status(403).json({ error: "Statement request has not been approved for this account" });
  }

  const rows = await generateStatement(accountId, req.query.from, req.query.to);
  res.json(rows);
}));

router.get("/statements/:accountId/download", requireAuth, asyncHandler(async (req, res) => {
  const accountId = Number(req.params.accountId);
  if (!Number.isFinite(accountId) || accountId <= 0) {
    return res.status(400).json({ error: "Valid accountId is required" });
  }

  const account = await Account.findByPk(accountId);
  if (!account) {
    return res.status(404).json({ error: "Account not found" });
  }

  if (!isAdmin(req) && account.customerId !== getAuthenticatedCustomerId(req)) {
    return res.status(403).json({ error: "Forbidden" });
  }

  const approvedRequest = await StatementRequest.findOne({
    where: {
      [Op.or]: [
        { accountId: account.id },
        { accountNumber: account.accountNumber },
      ],
      status: "approved",
      ...(isAdmin(req) ? {} : { customerId: getAuthenticatedCustomerId(req) }),
    },
    order: [["reviewedAt", "DESC"]],
  });

  if (!approvedRequest) {
    return res.status(403).json({ error: "Statement request has not been approved for this account" });
  }

  const rows = await generateStatement(accountId, req.query.from, req.query.to);
  const lines = ["transactionId,createdAt,kind,amount,description,counterparty"];
  rows.forEach((r) => {
    lines.push(
      `${r.id},${r.createdAt},${r.type || r.kind},${r.amount},"${(r.description || "").replace(/\"/g, '""')}",${r.counterpartyAccountId || ""}`
    );
  });

  const csv = lines.join("\n");
  res.setHeader("Content-Type", "text/csv");
  res.setHeader("Content-Disposition", `attachment; filename=statement-${accountId}.csv`);
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
  res.json({ reserveBankMinSavingsInterestRate: getSavingsInterestRate() });
});

router.put("/config/interest-rate", (req, res) => {
  const rate = Number(req.body?.reserveBankMinSavingsInterestRate);
  if (!Number.isFinite(rate) || rate < 0) {
    return res.status(400).json({ error: "Valid non-negative rate is required" });
  }
  const updatedRate = setSavingsInterestRate(rate);
  res.json({ reserveBankMinSavingsInterestRate: updatedRate });
});

router.post("/year-end/apply-monthly-interest", requireAuth, requireAdmin, asyncHandler(async (req, res) => {
  const credited = await applyMonthlySavingsInterest();
  const totals = credited.reduce(
    (acc, row) => {
      acc.grossInterest += Number(row.grossInterest || 0);
      acc.withholdingTax += Number(row.withholdingTax || 0);
      acc.netInterest += Number(row.netInterest || 0);
      return acc;
    },
    { grossInterest: 0, withholdingTax: 0, netInterest: 0 }
  );

  res.json({
    creditedAccounts: credited,
    count: credited.length,
    totals: {
      grossInterest: Number(totals.grossInterest.toFixed(2)),
      withholdingTax: Number(totals.withholdingTax.toFixed(2)),
      netInterest: Number(totals.netInterest.toFixed(2)),
    },
    annualRate: getSavingsInterestRate(),
  });
}));

router.post("/year-end/interest-summaries", requireAuth, requireAdmin, asyncHandler(async (req, res) => {
  const year = Number(req.body?.year || new Date().getFullYear());
  const rows = await generateInterestSummaries(year);
  res.json(rows);
}));

router.get("/year-end/interest-summaries", requireAuth, requireAdmin, asyncHandler(async (req, res) => {
  const year = Number(req.query?.year || new Date().getFullYear());
  const summaries = await generateInterestSummaries(year);
  res.json(summaries);
}));

router.post("/accounts/apply-maintenance-fees", requireAuth, requireAdmin, asyncHandler(async (req, res) => {
  const charged = await applyMonthlyFees();
  res.json({ chargedAccounts: charged, count: charged.length });
}));

router.get("/loan-products", (req, res) => {
  res.json(loanProducts);
});

router.post("/loan-applications", requireAuth, asyncHandler(async (req, res) => {
  const payload = req.body || {};
  const required = ["customerId", "loanProductId", "requestedAmount", "termMonths", "purpose"];
  const missing = required.filter((k) => payload[k] === undefined || payload[k] === null || payload[k] === "");
  if (missing.length > 0) {
    return res.status(400).json({ error: `Missing fields: ${missing.join(", ")}` });
  }

  if (!canAccessCustomer(req, payload.customerId)) {
    return res.status(403).json({ error: "Forbidden" });
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

router.get("/loan-applications", requireAuth, asyncHandler(async (req, res) => {
  const where = isAdmin(req) ? undefined : { customerId: getAuthenticatedCustomerId(req) };
  const rows = await Loan.findAll({ where, order: [["createdAt", "DESC"]] });
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
