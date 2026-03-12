import { useEffect, useMemo, useState } from "react";
import { api, setToken, clearToken } from "./api";
import { tabs } from "./constants/tabs";
import AuthPage from "./components/AuthPage";
import HomePage from "./components/HomePage";
import SiteFooter from "./components/SiteFooter";

export default function App() {
  const [activeTab, setActiveTab] = useState("Overview");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // ── Auth state ───────────────────────────────────────────────────────────
  const [authToken, setAuthToken] = useState(null);
  const [currentUser, setCurrentUser] = useState(null);
  const [authView, setAuthView] = useState("login"); // "login" | "register"
  const [authForm, setAuthForm] = useState({ fullName: "", mobile: "", email: "", password: "", confirmPassword: "" });
  const [authMessage, setAuthMessage] = useState("");
  // ────────────────────────────────────────────────────────────────────────
  const [customers, setCustomers] = useState([]);
  const [accounts, setAccounts] = useState([]);
  const [requirements, setRequirements] = useState(null);
  const [scheduledBills, setScheduledBills] = useState([]);
  const [loanProducts, setLoanProducts] = useState([]);
  const [loanApplications, setLoanApplications] = useState([]);
  const [investments, setInvestments] = useState([]);
  const [summaries, setSummaries] = useState([]);
  const [interestRate, setInterestRate] = useState(0);
  const [selectedAccountForTx, setSelectedAccountForTx] = useState("");
  const [transactions, setTransactions] = useState([]);
  const [statementAccount, setStatementAccount] = useState("");
  const [statementRows, setStatementRows] = useState([]);
  const [notificationCustomer, setNotificationCustomer] = useState("");
  const [notifications, setNotifications] = useState([]);

  const [transferForm, setTransferForm] = useState({ fromAccountId: "", toAccountId: "", amount: "", description: "" });
  const [transferMessage, setTransferMessage] = useState("");
  const [pendingTransfer, setPendingTransfer] = useState({ transferId: "", otp: "" });

  const [manualBillForm, setManualBillForm] = useState({ accountId: "", payee: "", amount: "" });
  const [scheduleBillForm, setScheduleBillForm] = useState({ accountId: "", payee: "", amount: "", scheduledDate: "" });
  const [billMessage, setBillMessage] = useState("");

  const [investmentForm, setInvestmentForm] = useState({ customerId: "", name: "", amount: "", annualRate: "" });
  const [investmentMessage, setInvestmentMessage] = useState("");

  const [loanForm, setLoanForm] = useState({
    customerId: "",
    loanProductId: "",
    requestedAmount: "",
    termMonths: "",
    purpose: "",
    monthlyIncome: "",
    employmentStatus: "",
  });
  const [loanMessage, setLoanMessage] = useState("");

  const [summaryYear, setSummaryYear] = useState(new Date().getFullYear());
  const [complianceMessage, setComplianceMessage] = useState("");

  const customerMap = useMemo(() => {
    const map = {};
    customers.forEach((c) => {
      map[c.id] = c;
    });
    return map;
  }, [customers]);

  useEffect(() => {
    if (authToken) loadInitialData();
  }, [authToken]);

  async function loadInitialData() {
    setLoading(true);
    setError("");
    try {
      const [reqs, customerRows, accountRows, scheduled, products, apps, invs, rate, sumRows] = await Promise.all([
        api.getRequirements(),
        api.getCustomers(),
        api.getAccounts(),
        api.getScheduledBills(),
        api.getLoanProducts(),
        api.getLoanApplications(),
        api.getInvestments(),
        api.getInterestRate(),
        api.getSummaries(),
      ]);

      setRequirements(reqs);
      setCustomers(customerRows);
      setAccounts(accountRows);
      setScheduledBills(scheduled);
      setLoanProducts(products);
      setLoanApplications(apps);
      setInvestments(invs);
      setInterestRate(rate.reserveBankMinSavingsInterestRate);
      setSummaries(sumRows);

      if (accountRows.length > 0) {
        setSelectedAccountForTx(accountRows[0].id);
        setStatementAccount(accountRows[0].id);
      }
      if (customerRows.length > 0) {
        setNotificationCustomer(customerRows[0].id);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (!selectedAccountForTx) return;
    api.getTransactions(selectedAccountForTx).then(setTransactions).catch((err) => setError(err.message));
  }, [selectedAccountForTx]);

  useEffect(() => {
    if (!notificationCustomer) return;
    api.getNotifications(notificationCustomer).then(setNotifications).catch((err) => setError(err.message));
  }, [notificationCustomer]);

  // ── Auth handlers ────────────────────────────────────────────────────────
  async function onLogin(e) {
    e.preventDefault();
    setAuthMessage("");
    try {
      const result = await api.login({ email: authForm.email, password: authForm.password });
      setToken(result.token);
      setAuthToken(result.token);
      setCurrentUser({ fullName: result.fullName, userId: result.userId, customerId: result.customerId });
    } catch (err) {
      setAuthMessage(err.message);
    }
  }

  async function onRegister(e) {
    e.preventDefault();
    setAuthMessage("");
    if (authForm.password !== authForm.confirmPassword) {
      setAuthMessage("Passwords do not match");
      return;
    }
    try {
      await api.register({
        fullName: authForm.fullName,
        mobile: authForm.mobile,
        email: authForm.email,
        password: authForm.password,
      });
      setAuthMessage("Registration successful! You can now log in.");
      setAuthView("login");
      setAuthForm({ ...authForm, password: "", confirmPassword: "" });
    } catch (err) {
      setAuthMessage(err.message);
    }
  }

  function onLogout() {
    clearToken();
    setAuthToken(null);
    setCurrentUser(null);
    setAuthView("login");
    setAuthForm({ fullName: "", mobile: "", email: "", password: "", confirmPassword: "" });
    setAuthMessage("");
  }

  function onAuthViewChange(view) {
    setAuthView(view);
    setAuthMessage("");
  }
  // ────────────────────────────────────────────────────────────────────────

  async function onInitiateTransfer(e) {
    e.preventDefault();
    setTransferMessage("");
    try {
      const payload = {
        ...transferForm,
        amount: Number(transferForm.amount),
      };
      const result = await api.initiateTransfer(payload);
      if (result.requiresOtp) {
        setPendingTransfer({ transferId: result.transferId, otp: result.otp });
        setTransferMessage(`High-value transfer pending OTP. Demo OTP: ${result.otp}`);
      } else {
        setPendingTransfer({ transferId: "", otp: "" });
        setTransferMessage("Transfer completed successfully.");
        await loadInitialData();
      }
    } catch (err) {
      setTransferMessage(err.message);
    }
  }

  async function onVerifyTransfer(e) {
    e.preventDefault();
    try {
      await api.verifyTransfer({ transferId: pendingTransfer.transferId, otp: pendingTransfer.otp });
      setTransferMessage("OTP verified and transfer completed.");
      setPendingTransfer({ transferId: "", otp: "" });
      await loadInitialData();
    } catch (err) {
      setTransferMessage(err.message);
    }
  }

  async function onManualBill(e) {
    e.preventDefault();
    setBillMessage("");
    try {
      await api.payBillManual({ ...manualBillForm, amount: Number(manualBillForm.amount) });
      setBillMessage("Manual bill payment processed.");
      await loadInitialData();
    } catch (err) {
      setBillMessage(err.message);
    }
  }

  async function onScheduleBill(e) {
    e.preventDefault();
    setBillMessage("");
    try {
      await api.scheduleBill({ ...scheduleBillForm, amount: Number(scheduleBillForm.amount) });
      setBillMessage("Scheduled bill payment created.");
      await loadInitialData();
    } catch (err) {
      setBillMessage(err.message);
    }
  }

  async function runScheduledBill(id) {
    setBillMessage("");
    try {
      await api.runScheduledBill(id);
      setBillMessage(`Scheduled payment ${id} processed.`);
      await loadInitialData();
    } catch (err) {
      setBillMessage(err.message);
    }
  }

  async function fetchStatement() {
    try {
      const rows = await api.getStatement(statementAccount);
      setStatementRows(rows);
    } catch (err) {
      setError(err.message);
    }
  }

  async function onAddInvestment(e) {
    e.preventDefault();
    setInvestmentMessage("");
    try {
      await api.addInvestment({
        ...investmentForm,
        amount: Number(investmentForm.amount),
        annualRate: Number(investmentForm.annualRate),
      });
      setInvestmentMessage("Investment created.");
      await loadInitialData();
    } catch (err) {
      setInvestmentMessage(err.message);
    }
  }

  async function onSubmitLoan(e) {
    e.preventDefault();
    setLoanMessage("");
    try {
      await api.createLoanApplication({
        ...loanForm,
        requestedAmount: Number(loanForm.requestedAmount),
        termMonths: Number(loanForm.termMonths),
        monthlyIncome: Number(loanForm.monthlyIncome || 0),
      });
      setLoanMessage("Loan application submitted.");
      await loadInitialData();
    } catch (err) {
      setLoanMessage(err.message);
    }
  }

  async function onUpdateRate(e) {
    e.preventDefault();
    setComplianceMessage("");
    try {
      await api.updateInterestRate(interestRate);
      setComplianceMessage("Savings interest rate updated.");
      await loadInitialData();
    } catch (err) {
      setComplianceMessage(err.message);
    }
  }

  async function onGenerateSummaries() {
    setComplianceMessage("");
    try {
      const rows = await api.generateSummaries(Number(summaryYear));
      setSummaries(rows);
      setComplianceMessage(`Generated ${rows.length} annual interest summaries and submitted to FRCS.`);
    } catch (err) {
      setComplianceMessage(err.message);
    }
  }

  const totalBalance = accounts.reduce((sum, a) => sum + a.balance, 0);
  const currentYear = new Date().getFullYear();

  // ── Auth gate ────────────────────────────────────────────────────────────
  if (!authToken) {
    return (
      <AuthPage
        authView={authView}
        setAuthView={onAuthViewChange}
        authForm={authForm}
        setAuthForm={setAuthForm}
        authMessage={authMessage}
        onLogin={onLogin}
        onRegister={onRegister}
        currentYear={currentYear}
      />
    );
  }
  // ────────────────────────────────────────────────────────────────────────

  return (
    <div className="app-shell">
      <header className="hero">
        <div className="hero-row">
          <div>
            <h1>Bank of Fiji Online Banking Prototype</h1>
            <p>Client-server implementation using React frontend + Node.js REST backend.</p>
          </div>
          {currentUser && (
            <div className="hero-user">
              <span>Welcome, <strong>{currentUser.fullName}</strong></span>
              <button className="logout-btn" onClick={onLogout}>Logout</button>
            </div>
          )}
        </div>
      </header>

      <nav className="tabs">
        {tabs.map((tab) => (
          <button
            key={tab}
            className={tab === activeTab ? "tab active" : "tab"}
            onClick={() => setActiveTab(tab)}
          >
            {tab}
          </button>
        ))}
      </nav>

      {loading && <p className="status">Loading data...</p>}
      {error && <p className="status error">{error}</p>}

      {!loading && activeTab === "Overview" && (
        <HomePage
          customers={customers}
          accounts={accounts}
          totalBalance={totalBalance}
          customerMap={customerMap}
          selectedAccountForTx={selectedAccountForTx}
          setSelectedAccountForTx={setSelectedAccountForTx}
          transactions={transactions}
        />
      )}

      {!loading && activeTab === "Transfers" && (
        <section className="panel-grid">
          <article className="panel">
            <h2>Initiate Transfer</h2>
            <form onSubmit={onInitiateTransfer}>
              <label>
                From Account
                <select
                  value={transferForm.fromAccountId}
                  onChange={(e) => setTransferForm({ ...transferForm, fromAccountId: e.target.value })}
                  required
                >
                  <option value="">Select</option>
                  {accounts.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.id} - FJD {a.balance.toFixed(2)}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                To Account
                <select
                  value={transferForm.toAccountId}
                  onChange={(e) => setTransferForm({ ...transferForm, toAccountId: e.target.value })}
                  required
                >
                  <option value="">Select</option>
                  {accounts.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.id} - {a.type}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Amount (FJD)
                <input
                  type="number"
                  min="1"
                  step="0.01"
                  value={transferForm.amount}
                  onChange={(e) => setTransferForm({ ...transferForm, amount: e.target.value })}
                  required
                />
              </label>
              <label>
                Description
                <input
                  value={transferForm.description}
                  onChange={(e) => setTransferForm({ ...transferForm, description: e.target.value })}
                />
              </label>
              <button type="submit">Send Transfer</button>
            </form>
          </article>

          <article className="panel">
            <h2>OTP Verification</h2>
            <div className="otp-notice">
              🔒 Transfers of <strong>FJD 1,000 or more</strong> require OTP verification before processing. The OTP is sent via SMS to the account holder's mobile number.
            </div>
            <form onSubmit={onVerifyTransfer}>
              <label>
                Transfer ID
                <input
                  value={pendingTransfer.transferId}
                  onChange={(e) => setPendingTransfer({ ...pendingTransfer, transferId: e.target.value })}
                  required
                />
              </label>
              <label>
                OTP
                <input
                  value={pendingTransfer.otp}
                  onChange={(e) => setPendingTransfer({ ...pendingTransfer, otp: e.target.value })}
                  required
                />
              </label>
              <button type="submit">Verify OTP</button>
            </form>
            <p className="status">{transferMessage}</p>
            <p className="hint">OTP is required only for high-value transactions.</p>
          </article>
        </section>
      )}

      {!loading && activeTab === "Bill Payments" && (
        <section className="panel-grid">
          <article className="panel">
            <h2>Manual Bill Payment</h2>
            <form onSubmit={onManualBill}>
              <label>
                Account
                <select
                  value={manualBillForm.accountId}
                  onChange={(e) => setManualBillForm({ ...manualBillForm, accountId: e.target.value })}
                  required
                >
                  <option value="">Select</option>
                  {accounts.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.id}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Payee
                <input
                  value={manualBillForm.payee}
                  onChange={(e) => setManualBillForm({ ...manualBillForm, payee: e.target.value })}
                  required
                />
              </label>
              <label>
                Amount
                <input
                  type="number"
                  min="1"
                  step="0.01"
                  value={manualBillForm.amount}
                  onChange={(e) => setManualBillForm({ ...manualBillForm, amount: e.target.value })}
                  required
                />
              </label>
              <button type="submit">Pay Now</button>
            </form>
          </article>

          <article className="panel">
            <h2>Schedule Auto Payment</h2>
            <form onSubmit={onScheduleBill}>
              <label>
                Account
                <select
                  value={scheduleBillForm.accountId}
                  onChange={(e) => setScheduleBillForm({ ...scheduleBillForm, accountId: e.target.value })}
                  required
                >
                  <option value="">Select</option>
                  {accounts.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.id}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Payee
                <input
                  value={scheduleBillForm.payee}
                  onChange={(e) => setScheduleBillForm({ ...scheduleBillForm, payee: e.target.value })}
                  required
                />
              </label>
              <label>
                Amount
                <input
                  type="number"
                  min="1"
                  step="0.01"
                  value={scheduleBillForm.amount}
                  onChange={(e) => setScheduleBillForm({ ...scheduleBillForm, amount: e.target.value })}
                  required
                />
              </label>
              <label>
                Scheduled Date
                <input
                  type="date"
                  value={scheduleBillForm.scheduledDate}
                  onChange={(e) => setScheduleBillForm({ ...scheduleBillForm, scheduledDate: e.target.value })}
                  required
                />
              </label>
              <button type="submit">Schedule</button>
            </form>
          </article>

          <article className="panel wide">
            <h2>Scheduled Payments</h2>
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Account</th>
                  <th>Payee</th>
                  <th>Amount</th>
                  <th>Date</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {scheduledBills.map((b) => (
                  <tr key={b.id}>
                    <td>{b.id}</td>
                    <td>{b.accountId}</td>
                    <td>{b.payee}</td>
                    <td>FJD {b.amount.toFixed(2)}</td>
                    <td>{b.scheduledDate}</td>
                    <td>{b.status}</td>
                    <td>
                      <button disabled={b.status === "processed"} onClick={() => runScheduledBill(b.id)}>
                        Run
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <p className="status">{billMessage}</p>
          </article>
        </section>
      )}

      {!loading && activeTab === "Statements" && (
        <section className="panel-grid">
          <article className="panel wide">
            <h2>On-Demand Statements</h2>
            <div className="inline-controls">
              <label>
                Account
                <select value={statementAccount} onChange={(e) => setStatementAccount(e.target.value)}>
                  {accounts.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.id}
                    </option>
                  ))}
                </select>
              </label>
              <button onClick={fetchStatement}>View Statement</button>
              <a className="button-link" href={api.statementDownloadUrl(statementAccount)} target="_blank" rel="noreferrer">
                Download CSV
              </a>
            </div>
            <table>
              <thead>
                <tr>
                  <th>Time</th>
                  <th>Type</th>
                  <th>Amount</th>
                  <th>Description</th>
                </tr>
              </thead>
              <tbody>
                {statementRows.map((r) => (
                  <tr key={r.id}>
                    <td>{new Date(r.createdAt).toLocaleString()}</td>
                    <td>{r.kind}</td>
                    <td>FJD {r.amount.toFixed(2)}</td>
                    <td>{r.description}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </article>

          <article className="panel">
            <h2>SMS Notifications</h2>
            <label>
              Customer
              <select value={notificationCustomer} onChange={(e) => setNotificationCustomer(e.target.value)}>
                {customers.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.fullName}
                  </option>
                ))}
              </select>
            </label>
            <ul className="feed">
              {notifications.map((n) => (
                <li key={n.id}>
                  <strong>{new Date(n.createdAt).toLocaleString()}:</strong> {n.message}
                </li>
              ))}
            </ul>
          </article>
        </section>
      )}

      {!loading && activeTab === "Investments" && (
        <section className="panel-grid">
          <article className="panel">
            <h2>Create Investment</h2>
            <form onSubmit={onAddInvestment}>
              <label>
                Customer
                <select
                  value={investmentForm.customerId}
                  onChange={(e) => setInvestmentForm({ ...investmentForm, customerId: e.target.value })}
                  required
                >
                  <option value="">Select</option>
                  {customers.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.fullName}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Product Name
                <input
                  value={investmentForm.name}
                  onChange={(e) => setInvestmentForm({ ...investmentForm, name: e.target.value })}
                  required
                />
              </label>
              <label>
                Amount
                <input
                  type="number"
                  min="1"
                  step="0.01"
                  value={investmentForm.amount}
                  onChange={(e) => setInvestmentForm({ ...investmentForm, amount: e.target.value })}
                  required
                />
              </label>
              <label>
                Annual Rate (decimal)
                <input
                  type="number"
                  step="0.0001"
                  value={investmentForm.annualRate}
                  onChange={(e) => setInvestmentForm({ ...investmentForm, annualRate: e.target.value })}
                  required
                />
              </label>
              <button type="submit">Create</button>
            </form>
            <p className="status">{investmentMessage}</p>
          </article>

          <article className="panel wide">
            <h2>Investments</h2>
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Customer</th>
                  <th>Name</th>
                  <th>Amount</th>
                  <th>Rate</th>
                </tr>
              </thead>
              <tbody>
                {investments.map((inv) => (
                  <tr key={inv.id}>
                    <td>{inv.id}</td>
                    <td>{customerMap[inv.customerId]?.fullName || inv.customerId}</td>
                    <td>{inv.name}</td>
                    <td>FJD {inv.amount.toFixed(2)}</td>
                    <td>{(inv.annualRate * 100).toFixed(2)}%</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </article>
        </section>
      )}

      {!loading && activeTab === "Loans" && (
        <section className="panel-grid">
          <article className="panel wide">
            <h2>Loan Products (Website Advertisement)</h2>
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Annual Rate</th>
                  <th>Max Amount</th>
                  <th>Term</th>
                </tr>
              </thead>
              <tbody>
                {loanProducts.map((lp) => (
                  <tr key={lp.id}>
                    <td>{lp.name}</td>
                    <td>{(lp.annualRate * 100).toFixed(2)}%</td>
                    <td>FJD {lp.maxAmount.toFixed(2)}</td>
                    <td>
                      {lp.minTermMonths}-{lp.maxTermMonths} months
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </article>

          <article className="panel">
            <h2>Apply For Loan</h2>
            <form onSubmit={onSubmitLoan}>
              <label>
                Customer
                <select value={loanForm.customerId} onChange={(e) => setLoanForm({ ...loanForm, customerId: e.target.value })} required>
                  <option value="">Select</option>
                  {customers.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.fullName}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Loan Product
                <select
                  value={loanForm.loanProductId}
                  onChange={(e) => setLoanForm({ ...loanForm, loanProductId: e.target.value })}
                  required
                >
                  <option value="">Select</option>
                  {loanProducts.map((lp) => (
                    <option key={lp.id} value={lp.id}>
                      {lp.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Requested Amount
                <input
                  type="number"
                  min="1"
                  value={loanForm.requestedAmount}
                  onChange={(e) => setLoanForm({ ...loanForm, requestedAmount: e.target.value })}
                  required
                />
              </label>
              <label>
                Term (months)
                <input
                  type="number"
                  min="1"
                  value={loanForm.termMonths}
                  onChange={(e) => setLoanForm({ ...loanForm, termMonths: e.target.value })}
                  required
                />
              </label>
              <label>
                Purpose
                <input value={loanForm.purpose} onChange={(e) => setLoanForm({ ...loanForm, purpose: e.target.value })} required />
              </label>
              <label>
                Monthly Income
                <input
                  type="number"
                  min="0"
                  value={loanForm.monthlyIncome}
                  onChange={(e) => setLoanForm({ ...loanForm, monthlyIncome: e.target.value })}
                />
              </label>
              <label>
                Employment Status
                <input
                  value={loanForm.employmentStatus}
                  onChange={(e) => setLoanForm({ ...loanForm, employmentStatus: e.target.value })}
                />
              </label>
              <button type="submit">Submit Application</button>
            </form>
            <p className="status">{loanMessage}</p>
          </article>

          <article className="panel wide">
            <h2>Submitted Loan Applications</h2>
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Customer</th>
                  <th>Product</th>
                  <th>Amount</th>
                  <th>Term</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {loanApplications.map((a) => (
                  <tr key={a.id}>
                    <td>{a.id}</td>
                    <td>{customerMap[a.customerId]?.fullName || a.customerId}</td>
                    <td>{loanProducts.find((p) => p.id === a.loanProductId)?.name || a.loanProductId}</td>
                    <td>FJD {a.requestedAmount.toFixed(2)}</td>
                    <td>{a.termMonths}</td>
                    <td>{a.status}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </article>
        </section>
      )}

      {!loading && activeTab === "Compliance" && (
        <section className="panel-grid">
          <article className="panel">
            <h2>Savings Interest Rate Config</h2>
            <form onSubmit={onUpdateRate}>
              <label>
                Reserve Bank Minimum Savings Interest Rate (decimal)
                <input
                  type="number"
                  step="0.0001"
                  value={interestRate}
                  onChange={(e) => setInterestRate(e.target.value)}
                  required
                />
              </label>
              <button type="submit">Update Rate</button>
            </form>
          </article>

          <article className="panel">
            <h2>Generate Year-End Interest Summaries</h2>
            <label>
              Year
              <input type="number" value={summaryYear} onChange={(e) => setSummaryYear(e.target.value)} />
            </label>
            <button onClick={onGenerateSummaries}>Generate + Submit to FRCS</button>
            <p className="status">{complianceMessage}</p>
          </article>

          <article className="panel wide">
            <h2>Interest Summaries</h2>
            <table>
              <thead>
                <tr>
                  <th>Account</th>
                  <th>Customer</th>
                  <th>Year</th>
                  <th>Gross</th>
                  <th>Withholding Tax</th>
                  <th>Net</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {summaries.map((s) => (
                  <tr key={s.id}>
                    <td>{s.accountId}</td>
                    <td>{s.customerName}</td>
                    <td>{s.year}</td>
                    <td>FJD {s.grossInterest.toFixed(2)}</td>
                    <td>FJD {s.withholdingTax.toFixed(2)}</td>
                    <td>FJD {s.netInterest.toFixed(2)}</td>
                    <td>{s.status}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </article>
        </section>
      )}

      {!loading && activeTab === "Requirements" && requirements && (
        <section className="panel-grid">
          <article className="panel wide">
            <h2>Prioritized User Stories</h2>
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Priority</th>
                  <th>User Story</th>
                </tr>
              </thead>
              <tbody>
                {requirements.userStories.map((s) => (
                  <tr key={s.id}>
                    <td>{s.id}</td>
                    <td>{s.priority}</td>
                    <td>{s.story}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </article>

          <article className="panel wide">
            <h2>Conflicting Requirements and Trade-Offs</h2>
            <table>
              <thead>
                <tr>
                  <th>Conflict</th>
                  <th>Trade-Off</th>
                </tr>
              </thead>
              <tbody>
                {requirements.conflictsAndTradeOffs.map((item, idx) => (
                  <tr key={idx}>
                    <td>{item.conflict}</td>
                    <td>{item.tradeOff}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </article>

          <article className="panel">
            <h2>High Priority Prototypes</h2>
            <ul className="feed">
              {requirements.highPriorityPrototypes.map((p) => (
                <li key={p}>{p}</li>
              ))}
            </ul>
          </article>
        </section>
      )}

      <SiteFooter currentYear={currentYear} />
    </div>
  );
}
