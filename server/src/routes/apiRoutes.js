const express = require("express");
const requirementsData = require("../config/requirementsData");
const {
  HIGH_VALUE_OTP_THRESHOLD,
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
} = require("../store-mysql");
const { Customer, Account, Bill, Investment, Loan } = require("../models");

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

const toCustomerResponse = (c) => ({
  id: c.id,
  fullName: c.fullName,
  mobile: c.mobile,
  residencyStatus: "resident",
  tin: "",
});

const toAccountResponse = (a) => ({
  id: a.id,
  customerId: a.customerId,
  type: a.accountType,
  balance: Number(a.balance),
  maintenanceFee: a.accountType === "Simple Access" ? 2.5 : 0,
  currency: a.currency,
  createdAt: a.createdAt,
});

router.get("/health", (req, res) => {
  res.json({ status: "ok", service: "BoF Banking API", at: new Date().toISOString() });
});

router.get("/requirements", (req, res) => {
  res.json(requirementsData);
});

router.get("/customers", asyncHandler(async (req, res) => {
  const rows = await Customer.findAll({ order: [["createdAt", "ASC"]] });
  res.json(rows.map(toCustomerResponse));
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
  });

  res.status(201).json({
    ...toCustomerResponse(customer),
    residencyStatus: payload.residencyStatus || "resident",
    tin: payload.tin || "",
  });
}));

router.get("/accounts", asyncHandler(async (req, res) => {
  const rows = await Account.findAll({ order: [["createdAt", "ASC"]] });
  res.json(rows.map(toAccountResponse));
}));

router.post("/accounts", asyncHandler(async (req, res) => {
  const payload = req.body || {};
  const providedAccountNumber = String(payload.accountNumber || "").trim();
  if (!payload.customerId || !payload.type) {
    return res.status(400).json({ error: "customerId and type are required" });
  }
  if (!["Simple Access", "Savings"].includes(payload.type)) {
    return res.status(400).json({ error: "type must be Simple Access or Savings" });
  }
  if (providedAccountNumber && !/^\d{12}$/.test(providedAccountNumber)) {
    return res.status(400).json({ error: "Reenter 12 digit number" });
  }

  const account = await Account.create({
    customerId: payload.customerId,
    accountNumber: providedAccountNumber || await generateRandomAccountNumber(),
    accountType: payload.type,
    balance: Number(payload.openingBalance || 0),
    currency: "FJD",
    status: "active",
  });

  res.status(201).json(toAccountResponse(account));
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

router.post("/transfers/initiate", asyncHandler(async (req, res) => {
  const result = await initiateTransfer(req.body || {});
  res.json({ highValueThreshold: HIGH_VALUE_OTP_THRESHOLD, ...result });
}));

router.post("/transfers/verify", asyncHandler(async (req, res) => {
  const result = await verifyTransfer(req.body || {});
  res.json(result);
}));

router.post("/bills/manual", asyncHandler(async (req, res) => {
  const payload = req.body || {};
  const payment = await postBillPayment({
    accountId: payload.accountId,
    payee: payload.payee,
    amount: Number(payload.amount),
    mode: "manual",
  });
  res.status(201).json(payment);
}));

router.post("/bills/scheduled", asyncHandler(async (req, res) => {
  const payload = req.body || {};
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
      accountId: null,
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

router.get("/notifications/:customerId", (req, res) => {
  // Notifications are not persisted yet in MySQL. Keep API stable.
  res.json([]);
});

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
    loanProductId: payload.loanProductId,
    requestedAmount: Number(loan.principal),
    termMonths: Number(payload.termMonths),
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
      loanProductId: null,
      requestedAmount: Number(l.principal),
      termMonths: null,
      purpose: l.loanType,
      monthlyIncome: 0,
      employmentStatus: "unknown",
      status: l.status,
      createdAt: l.createdAt,
    }))
  );
}));

module.exports = router;
