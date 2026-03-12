const API_BASE = import.meta.env.VITE_API_BASE || "/api";

let _authToken = null;
export function setToken(token) { _authToken = token; }
export function clearToken() { _authToken = null; }

async function request(path, options = {}) {
  if (path === "/accounts" && String(options.method || "GET").toUpperCase() === "POST" && options.body) {
    try {
      const payload = JSON.parse(options.body);
      const accountNumber = String(payload?.accountNumber || "").trim();
      if (accountNumber && !/^\d{12}$/.test(accountNumber)) {
        throw new Error("Reenter 12 digit number");
      }
    } catch (err) {
      if (err instanceof Error && err.message === "Reenter 12 digit number") {
        throw err;
      }
    }
  }

  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {}),
  };
  if (_authToken) {
    headers["Authorization"] = `Bearer ${_authToken}`;
  }
  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers,
    });
  } catch (err) {
    throw new Error("Cannot reach backend server. Start server on port 4000 and try again.");
  }

  if (!response.ok) {
    let message = "Request failed";
    try {
      const payload = await response.json();
      message = payload.error || message;
    } catch (err) {
      message = `${response.status} ${response.statusText}`;
    }
    throw new Error(message);
  }

  const ct = response.headers.get("content-type") || "";
  if (ct.includes("application/json")) {
    return response.json();
  }
  return response.text();
}

export const api = {
  register: (body) => request("/auth/register", { method: "POST", body: JSON.stringify(body) }),
  login: (body) => request("/auth/login", { method: "POST", body: JSON.stringify(body) }),
  getRequirements: () => request("/requirements"),
  getCustomers: () => request("/customers"),
  getAccounts: () => request("/accounts"),
  getTransactions: (accountId) => request(`/transactions?accountId=${encodeURIComponent(accountId)}`),
  initiateTransfer: (body) => request("/transfers/initiate", { method: "POST", body: JSON.stringify(body) }),
  verifyTransfer: (body) => request("/transfers/verify", { method: "POST", body: JSON.stringify(body) }),
  payBillManual: (body) => request("/bills/manual", { method: "POST", body: JSON.stringify(body) }),
  scheduleBill: (body) => request("/bills/scheduled", { method: "POST", body: JSON.stringify(body) }),
  getScheduledBills: () => request("/bills/scheduled"),
  runScheduledBill: (id) => request(`/bills/scheduled/${id}/run`, { method: "POST" }),
  getStatement: (accountId) => request(`/statements/${accountId}`),
  getNotifications: (customerId) => request(`/notifications/${customerId}`),
  getInvestments: () => request("/investments"),
  addInvestment: (body) => request("/investments", { method: "POST", body: JSON.stringify(body) }),
  getInterestRate: () => request("/config/interest-rate"),
  updateInterestRate: (rate) =>
    request("/config/interest-rate", {
      method: "PUT",
      body: JSON.stringify({ reserveBankMinSavingsInterestRate: Number(rate) }),
    }),
  generateSummaries: (year) => request("/year-end/interest-summaries", { method: "POST", body: JSON.stringify({ year }) }),
  getSummaries: () => request("/year-end/interest-summaries"),
  getLoanProducts: () => request("/loan-products"),
  createLoanApplication: (body) => request("/loan-applications", { method: "POST", body: JSON.stringify(body) }),
  getLoanApplications: () => request("/loan-applications"),
  statementDownloadUrl: (accountId) => `${API_BASE}/statements/${accountId}/download`,
};
