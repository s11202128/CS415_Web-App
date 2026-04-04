/*
 * Automated transfer smoke test.
 * Flow:
 * 1) Register and login a fresh customer
 * 2) Create two customer accounts
 * 3) Deposit into source account
 * 4) Transfer from source to destination
 * 5) Verify balances changed as expected
 */

const BASE_URL = process.env.TEST_BASE_URL || "http://127.0.0.1:4000/api";
const PASSWORD = "Passw0rd!";

function nowStamp() {
  return Date.now();
}

function uniqueCustomer() {
  const n = nowStamp();
  return {
    fullName: `Transfer Test ${n}`,
    email: `transfer.test.${n}@example.com`,
    mobile: `+6799${String(n).slice(-6)}`,
  };
}

async function api(path, options = {}) {
  const res = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {}),
    },
  });

  const raw = await res.text();
  let body = null;
  try {
    body = raw ? JSON.parse(raw) : null;
  } catch (error) {
    body = { raw };
  }

  return { ok: res.ok, status: res.status, body };
}

function assert(condition, message, context) {
  if (!condition) {
    const err = new Error(message);
    err.context = context;
    throw err;
  }
}

function authHeader(token) {
  return { Authorization: `Bearer ${token}` };
}

function toNum(value) {
  return Number(value || 0);
}

(async () => {
  const customer = uniqueCustomer();

  console.log("[1/7] Registering customer...");
  const registerRes = await api("/auth/register", {
    method: "POST",
    body: JSON.stringify({
      fullName: customer.fullName,
      email: customer.email,
      mobile: customer.mobile,
      password: PASSWORD,
      confirmPassword: PASSWORD,
    }),
  });
  assert(registerRes.ok, "Registration failed", registerRes);

  console.log("[2/7] Logging in customer...");
  const loginRes = await api("/auth/login", {
    method: "POST",
    body: JSON.stringify({ email: customer.email, password: PASSWORD }),
  });
  assert(loginRes.ok, "Login failed", loginRes);

  const token = loginRes.body?.token;
  const customerId = Number(loginRes.body?.customerId || loginRes.body?.userId || 0);
  assert(token, "Missing auth token in login response", loginRes.body);
  assert(customerId > 0, "Missing customerId in login response", loginRes.body);

  console.log("[3/7] Creating source and destination accounts...");
  const createSourceRes = await api("/accounts/request", {
    method: "POST",
    headers: authHeader(token),
    body: JSON.stringify({
      customerId,
      type: "Simple Access",
      openingBalance: 0,
      customerName: customer.fullName,
    }),
  });
  assert(createSourceRes.ok, "Source account creation failed", createSourceRes);

  const createDestRes = await api("/accounts/request", {
    method: "POST",
    headers: authHeader(token),
    body: JSON.stringify({
      customerId,
      type: "Savings",
      openingBalance: 0,
      customerName: customer.fullName,
    }),
  });
  assert(createDestRes.ok, "Destination account creation failed", createDestRes);

  const sourceId = Number(createSourceRes.body?.id || 0);
  const destinationId = Number(createDestRes.body?.id || 0);
  assert(sourceId > 0 && destinationId > 0 && sourceId !== destinationId, "Invalid account IDs from create response", {
    source: createSourceRes.body,
    destination: createDestRes.body,
  });

  console.log("[4/7] Depositing funds into source account...");
  const depositAmount = 200;
  const depositRes = await api("/transactions/deposit", {
    method: "POST",
    headers: authHeader(token),
    body: JSON.stringify({
      accountId: sourceId,
      amount: depositAmount,
      note: "Automated smoke test deposit",
    }),
  });
  assert(depositRes.ok, "Deposit failed", depositRes);

  console.log("[5/7] Reading balances before transfer...");
  const beforeAccountsRes = await api("/accounts", {
    method: "GET",
    headers: authHeader(token),
  });
  assert(beforeAccountsRes.ok, "Could not fetch accounts before transfer", beforeAccountsRes);

  const beforeSource = beforeAccountsRes.body.find((a) => Number(a.id) === sourceId);
  const beforeDestination = beforeAccountsRes.body.find((a) => Number(a.id) === destinationId);
  assert(beforeSource && beforeDestination, "Source/destination accounts not found in account list", beforeAccountsRes.body);

  console.log("[6/7] Performing internal transfer...");
  const transferAmount = 50;
  const transferRes = await api("/transactions/transfer", {
    method: "POST",
    headers: authHeader(token),
    body: JSON.stringify({
      fromAccount: sourceId,
      transferType: "internal",
      toAccount: destinationId,
      amount: transferAmount,
      note: "Automated smoke test transfer",
    }),
  });
  assert(transferRes.ok, "Transfer failed", transferRes);
  assert(transferRes.body?.success === true, "Transfer response success flag is false", transferRes.body);

  console.log("[7/7] Verifying balances after transfer...");
  const afterAccountsRes = await api("/accounts", {
    method: "GET",
    headers: authHeader(token),
  });
  assert(afterAccountsRes.ok, "Could not fetch accounts after transfer", afterAccountsRes);

  const afterSource = afterAccountsRes.body.find((a) => Number(a.id) === sourceId);
  const afterDestination = afterAccountsRes.body.find((a) => Number(a.id) === destinationId);
  assert(afterSource && afterDestination, "Source/destination accounts missing after transfer", afterAccountsRes.body);

  const expectedSource = Number((toNum(beforeSource.balance) - transferAmount).toFixed(2));
  const expectedDestination = Number((toNum(beforeDestination.balance) + transferAmount).toFixed(2));

  const actualSource = Number(toNum(afterSource.balance).toFixed(2));
  const actualDestination = Number(toNum(afterDestination.balance).toFixed(2));

  assert(actualSource === expectedSource, "Source balance mismatch after transfer", {
    expectedSource,
    actualSource,
    beforeSource: beforeSource.balance,
    afterSource: afterSource.balance,
  });

  assert(actualDestination === expectedDestination, "Destination balance mismatch after transfer", {
    expectedDestination,
    actualDestination,
    beforeDestination: beforeDestination.balance,
    afterDestination: afterDestination.balance,
  });

  console.log("PASS: Automated transfer smoke test completed successfully.");
  console.log(JSON.stringify({
    customerId,
    sourceId,
    destinationId,
    transferAmount,
    expectedSource,
    expectedDestination,
  }, null, 2));
})().catch((error) => {
  console.error("FAIL: Automated transfer smoke test failed.");
  console.error(error.message || error);
  if (error.context) {
    console.error(JSON.stringify(error.context, null, 2));
  }
  process.exit(1);
});
