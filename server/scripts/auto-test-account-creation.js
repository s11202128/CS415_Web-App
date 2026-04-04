/*
 * Automated account creation smoke test.
 * Flow:
 * 1) Register and login a fresh customer
 * 2) Submit /accounts/request
 * 3) Verify response shape/status
 * 4) Verify account appears in GET /accounts for same customer
 */

const BASE_URL = process.env.TEST_BASE_URL || "http://127.0.0.1:4000/api";
const PASSWORD = "Passw0rd!";

function uniqueCustomer() {
  const n = Date.now();
  return {
    fullName: `Account Test ${n}`,
    email: `account.test.${n}@example.com`,
    mobile: `+6798${String(n).slice(-6)}`,
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

  const text = await res.text();
  let body;
  try {
    body = text ? JSON.parse(text) : null;
  } catch (error) {
    body = { raw: text };
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

(async () => {
  const customer = uniqueCustomer();

  console.log("[1/5] Registering customer...");
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

  console.log("[2/5] Logging in customer...");
  const loginRes = await api("/auth/login", {
    method: "POST",
    body: JSON.stringify({ email: customer.email, password: PASSWORD }),
  });
  assert(loginRes.ok, "Login failed", loginRes);

  const token = loginRes.body?.token;
  const customerId = Number(loginRes.body?.customerId || loginRes.body?.userId || 0);
  assert(token, "Token missing in login response", loginRes.body);
  assert(customerId > 0, "customerId missing/invalid in login response", loginRes.body);

  console.log("[3/5] Creating account request...");
  const createRes = await api("/accounts/request", {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body: JSON.stringify({
      type: "Simple Access",
      openingBalance: 0,
      customerId,
      customerName: customer.fullName,
    }),
  });
  assert(createRes.ok, "Account request failed", createRes);

  const created = createRes.body || {};
  const createdId = Number(created.id || 0);
  assert(createdId > 0, "Account ID not returned", created);
  assert(created.customerId === customerId, "Account customerId mismatch", { created, customerId });
  assert(typeof created.accountNumber === "string" && created.accountNumber.length === 12, "Invalid account number", created);
  assert(created.status === "pending_approval" || created.status === "active", "Unexpected account status", created);

  console.log("[4/5] Fetching customer accounts...");
  const listRes = await api("/accounts", {
    method: "GET",
    headers: { Authorization: `Bearer ${token}` },
  });
  assert(listRes.ok, "Could not fetch account list", listRes);
  assert(Array.isArray(listRes.body), "Account list response is not an array", listRes.body);

  console.log("[5/5] Verifying created account appears for customer...");
  const found = listRes.body.find((a) => Number(a.id) === createdId);
  assert(Boolean(found), "Created account not found in GET /accounts", {
    created,
    accountListCount: listRes.body.length,
  });

  console.log("PASS: Automated account creation smoke test completed successfully.");
  console.log(JSON.stringify({
    customerId,
    createdAccountId: createdId,
    createdAccountNumber: created.accountNumber,
    status: created.status,
    accountListCount: listRes.body.length,
  }, null, 2));
})().catch((error) => {
  console.error("FAIL: Automated account creation smoke test failed.");
  console.error(error.message || error);
  if (error.context) {
    console.error(JSON.stringify(error.context, null, 2));
  }
  process.exit(1);
});
