import { useEffect, useMemo, useState } from "react";
import { api, clearToken } from "./api";
import { tabs } from "./constants/tabs";
import AuthPage from "./components/AuthPage";
import HomePage from "./components/HomePage";
import AdminPage from "./components/AdminPage";
import SiteFooter from "./components/SiteFooter";
import AccountsTab from "./components/tabs/AccountsTab";
import TransfersTab from "./components/tabs/TransfersTab";
import BillPaymentsTab from "./components/tabs/BillPaymentsTab";
import StatementsTab from "./components/tabs/StatementsTab";
import InvestmentsTab from "./components/tabs/InvestmentsTab";
import LoansTab from "./components/tabs/LoansTab";
import ProfileTab from "./components/tabs/ProfileTab";
import ComplianceTab from "./components/tabs/ComplianceTab";
import AdminLockScreen from "./components/tabs/AdminLockScreen";

export default function App() {
  const [activeTab, setActiveTab] = useState("Overview");
  const [showAdmin, setShowAdmin] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const [authToken, setAuthToken] = useState(null);
  const [currentUser, setCurrentUser] = useState(null);
  const [customers, setCustomers] = useState([]);
  const [accounts, setAccounts] = useState([]);
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
  const [statementRequested, setStatementRequested] = useState(false);
  const [notificationCustomer, setNotificationCustomer] = useState("");
  const [notifications, setNotifications] = useState([]);
  const [profileForm, setProfileForm] = useState({
    fullName: "",
    email: "",
    mobile: "",
    currentPassword: "",
    newPassword: "",
  });
  const [profileMessage, setProfileMessage] = useState("");

  const [accountMessage, setAccountMessage] = useState("");
  const [transferForm, setTransferForm] = useState({ fromAccountId: "", toAccountNumber: "", amount: "", description: "" });
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
  const [adminMessage, setAdminMessage] = useState("");
  const [adminAccessGranted, setAdminAccessGranted] = useState(false);
  const [adminAuthForm, setAdminAuthForm] = useState({ email: "", password: "" });
  const [adminAuthMessage, setAdminAuthMessage] = useState("");
  const [adminTransactions, setAdminTransactions] = useState([]);
  const [adminLoginLogs, setAdminLoginLogs] = useState([]);
  const [adminNotificationLogs, setAdminNotificationLogs] = useState([]);
  const [adminTransferLimit, setAdminTransferLimit] = useState(1000);
  const [adminReport, setAdminReport] = useState(null);
  const [adminLastUpdated, setAdminLastUpdated] = useState("");
  const [adminAccountForm, setAdminAccountForm] = useState({
    customerName: "",
    type: "Simple Access",
    openingBalance: "0",
    accountNumber: "",
  });
  const [adminAccountMessage, setAdminAccountMessage] = useState("");

  const customerMap = useMemo(() => {
    const map = {};
    customers.forEach((c) => {
      map[c.id] = c;
    });
    return map;
  }, [customers]);

  const isAdminUser = Boolean(currentUser?.isAdmin);
  const effectiveShowAdmin = showAdmin || isAdminUser;

  useEffect(() => {
    if (authToken) loadInitialData();
  }, [authToken, currentUser?.customerId, currentUser?.isAdmin]);

  useEffect(() => {
    if (!currentUser?.customerId || customers.length === 0) {
      return;
    }
    const profile = customers.find((customer) => String(customer.id) === String(currentUser.customerId));
    if (!profile) {
      return;
    }
    setProfileForm((prev) => ({
      ...prev,
      fullName: profile.fullName || "",
      email: profile.email || currentUser.email || "",
      mobile: profile.mobile || currentUser.mobile || "",
      currentPassword: "",
      newPassword: "",
    }));
  }, [customers, currentUser]);

  async function loadInitialData() {
    setLoading(true);
    setError("");
    try {
      const [customerRows, accountRows, scheduled, products, apps, invs, rate, sumRows] = await Promise.all([
        api.getCustomers(),
        api.getAccounts(),
        api.getScheduledBills(),
        api.getLoanProducts(),
        api.getLoanApplications(),
        api.getInvestments(),
        api.getInterestRate(),
        api.getSummaries(),
      ]);
      const isAdminUser = Boolean(currentUser?.isAdmin);
      const activeCustomerId = currentUser?.customerId;

      const visibleCustomers = isAdminUser
        ? customerRows
        : customerRows.filter((c) => String(c.id) === String(activeCustomerId));

      const visibleCustomerIds = new Set(visibleCustomers.map((c) => String(c.id)));

      const visibleAccounts = isAdminUser
        ? accountRows
        : accountRows.filter((a) => visibleCustomerIds.has(String(a.customerId)));

      const visibleAccountIds = new Set(visibleAccounts.map((a) => String(a.id)));

      const visibleScheduledBills = isAdminUser
        ? scheduled
        : scheduled.filter((b) => {
          const byAccount = b.accountId && visibleAccountIds.has(String(b.accountId));
          const byCustomer = b.customerId && visibleCustomerIds.has(String(b.customerId));
          return byAccount || byCustomer;
        });

      const visibleLoanApplications = isAdminUser
        ? apps
        : apps.filter((l) => visibleCustomerIds.has(String(l.customerId)));

      const visibleInvestments = isAdminUser
        ? invs
        : invs.filter((x) => visibleCustomerIds.has(String(x.customerId)));

      const visibleSummaries = isAdminUser
        ? sumRows
        : sumRows.filter((s) => visibleCustomerIds.has(String(s.customerId)));

      setCustomers(visibleCustomers);
      setAccounts(visibleAccounts);
      setScheduledBills(visibleScheduledBills);
      setLoanProducts(products);
      setLoanApplications(visibleLoanApplications);
      setInvestments(visibleInvestments);
      setInterestRate(rate.reserveBankMinSavingsInterestRate);
      setSummaries(visibleSummaries);

      if (visibleAccounts.length > 0) {
        setSelectedAccountForTx((prev) =>
          visibleAccounts.some((a) => String(a.id) === String(prev)) ? prev : visibleAccounts[0].id
        );
        setStatementAccount((prev) =>
          visibleAccounts.some((a) => String(a.id) === String(prev)) ? prev : visibleAccounts[0].id
        );
      } else {
        setSelectedAccountForTx("");
        setStatementAccount("");
      }
      setStatementRows([]);
      setStatementRequested(false);
      if (visibleCustomers.length > 0) {
        setNotificationCustomer((prev) =>
          visibleCustomers.some((c) => String(c.id) === String(prev)) ? prev : visibleCustomers[0].id
        );
        setInvestmentForm((prev) => ({
          ...prev,
          customerId: visibleCustomers.some((c) => String(c.id) === String(prev.customerId))
            ? prev.customerId
            : String(visibleCustomers[0].id),
        }));
        setLoanForm((prev) => ({
          ...prev,
          customerId: visibleCustomers.some((c) => String(c.id) === String(prev.customerId))
            ? prev.customerId
            : String(visibleCustomers[0].id),
        }));
      } else {
        setNotificationCustomer("");
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (!accounts.length) {
      setTransactions([]);
      return;
    }

    let cancelled = false;
    const loadOverviewTransactions = async () => {
      try {
        if (currentUser?.isAdmin) {
          if (!selectedAccountForTx) {
            setTransactions([]);
            return;
          }
          const rows = await api.getTransactions(selectedAccountForTx);
          if (!cancelled) {
            setTransactions(rows);
          }
          return;
        }

        const rowsByAccount = await Promise.all(accounts.map((account) => api.getTransactions(account.id)));
        const merged = rowsByAccount
          .flat()
          .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        if (!cancelled) {
          setTransactions(merged);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err.message);
        }
      }
    };

    loadOverviewTransactions();
    return () => {
      cancelled = true;
    };
  }, [accounts, currentUser?.isAdmin, selectedAccountForTx]);

  useEffect(() => {
    if (!notificationCustomer) return;
    api.getNotifications(notificationCustomer).then(setNotifications).catch((err) => setError(err.message));
  }, [notificationCustomer]);

  useEffect(() => {
    const hasAdminAccess = currentUser?.isAdmin || adminAccessGranted;
    if (!hasAdminAccess || !showAdmin) {
      return;
    }
    let cancelled = false;
    const refreshAdminData = async () => {
      const selected = accounts.find((a) => a.id === selectedAccountForTx);
      const accountNumber = selected?.accountNumber || "";
      try {
        const [txRows, loginLogRows, notificationLogRows, limit, report] = await Promise.all([
          api.getAdminTransactions(accountNumber),
          api.getAdminLoginLogs(100),
          api.getNotificationLogsAdmin(100),
          api.getTransferLimitAdmin(),
          api.getAdminDashboardReport(),
        ]);
        if (cancelled) {
          return;
        }
        setAdminTransactions(txRows);
        setAdminLoginLogs(loginLogRows);
        setAdminNotificationLogs(notificationLogRows);
        setAdminTransferLimit(Number(limit.highValueTransferLimit || 1000));
        setAdminReport(report);
        setAdminLastUpdated(new Date().toISOString());
      } catch (err) {
        if (!cancelled) {
          setAdminMessage(err.message);
        }
      }
    };

    refreshAdminData();
    const timer = setInterval(refreshAdminData, 10000);
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, [showAdmin, currentUser?.isAdmin, adminAccessGranted, selectedAccountForTx, accounts]);

  const totalBalance = accounts.reduce((sum, a) => sum + a.balance, 0);
  const currentYear = new Date().getFullYear();

  // ── Auth gate ────────────────────────────────────────────────────────────
  if (!authToken) {
    return <AuthPage onLoginSuccess={handleLoginSuccess} currentYear={currentYear} />;
  }
  // ────────────────────────────────────────────────────────────────────────

  // ── Auth handlers ────────────────────────────────────────────────────────
  function handleLoginSuccess(token, user) {
    setAuthToken(token);
    setCurrentUser(user);
    setShowAdmin(Boolean(user?.isAdmin));
  }

  function onLogout() {
    clearToken();
    setAuthToken(null);
    setCurrentUser(null);
    setStatementRows([]);
    setStatementRequested(false);
    setAdminAccessGranted(false);
    setAdminAuthForm({ email: "", password: "" });
    setAdminAuthMessage("");
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
      setStatementRequested(true);
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

  async function onUpdateProfile(e) {
    e.preventDefault();
    setProfileMessage("");
    try {
      const updatedProfile = await api.updateProfile({
        customerId: currentUser.customerId,
        ...profileForm,
      });
      setCurrentUser((prev) => ({
        ...prev,
        fullName: updatedProfile.fullName,
        email: updatedProfile.email,
        mobile: updatedProfile.mobile,
      }));
      setProfileForm((prev) => ({ ...prev, currentPassword: "", newPassword: "" }));
      setProfileMessage("Profile updated successfully.");
      await loadInitialData();
    } catch (err) {
      setProfileMessage(err.message);
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

  async function onAdminUpdateCustomer(customerId, updates) {
    setAdminMessage("");
    try {
      await api.updateCustomerAdmin(customerId, updates);
      setAdminMessage("Customer updated.");
      await loadInitialData();
    } catch (err) {
      setAdminMessage(err.message);
    }
  }

  async function onAdminUpdateAccount(accountId, updates) {
    setAdminMessage("");
    try {
      await api.updateAccountAdmin(accountId, updates);
      setAdminMessage("Account updated.");
      await loadInitialData();
    } catch (err) {
      setAdminMessage(err.message);
    }
  }

  async function onAdminFreezeAccount(accountId) {
    setAdminMessage("");
    try {
      await api.freezeAccountAdmin(accountId);
      setAdminMessage("Account frozen.");
      await loadInitialData();
    } catch (err) {
      setAdminMessage(err.message);
    }
  }

  async function onAdminUpdateLoanStatus(loanId, status) {
    setAdminMessage("");
    try {
      await api.updateLoanApplicationAdmin(loanId, { status });
      setAdminMessage(`Loan ${status}.`);
      await loadInitialData();
    } catch (err) {
      setAdminMessage(err.message);
    }
  }

  async function onAdminUpdateTransferLimit(e) {
    e.preventDefault();
    setAdminMessage("");
    try {
      const result = await api.updateTransferLimitAdmin(adminTransferLimit);
      setAdminTransferLimit(Number(result.highValueTransferLimit));
      setAdminMessage("High-value transfer limit updated.");
    } catch (err) {
      setAdminMessage(err.message);
    }
  }

  async function onAdminReverseTransaction(transactionId) {
    setAdminMessage("");
    try {
      await api.reverseTransactionAdmin(transactionId);
      setAdminMessage("Transaction reversed successfully.");
      await loadInitialData();
    } catch (err) {
      setAdminMessage(err.message);
    }
  }

  async function onCreateAdminAccount(e) {
    e.preventDefault();
    setAdminAccountMessage("");
    try {
      await api.createAccount({
        customerName: adminAccountForm.customerName,
        type: adminAccountForm.type,
        openingBalance: Number(adminAccountForm.openingBalance || 0),
        accountNumber: adminAccountForm.accountNumber || undefined,
      });
      setAdminAccountMessage("Account created successfully.");
      setAdminAccountForm({ customerName: "", type: "Simple Access", openingBalance: "0", accountNumber: "" });
      await loadInitialData();
    } catch (err) {
      setAdminAccountMessage(err.message);
    }
  }

  async function onVerifyAdminAccess(e) {
    e.preventDefault();
    setAdminAuthMessage("");
    try {
      await api.verifyAdminCredentials(adminAuthForm);
      setAdminAccessGranted(true);
      setAdminAuthMessage("Admin access granted.");
    } catch (err) {
      setAdminAuthMessage(err.message);
    }
  }

  // ── Admin page view ───────────────────────────────────────────────────────
  if (effectiveShowAdmin) {
    return (
      <div className="app-shell">
        <header className="hero">
          <div className="hero-row">
            <div>
              <h1>Bank of Fiji — Admin</h1>
              <p>Admin dashboard — live updates every 10 seconds.</p>
            </div>
            {currentUser && (
              <div className="hero-user">
                <span>Welcome, <strong>{currentUser.fullName}</strong></span>
                  {!isAdminUser && <button className="home-btn" onClick={() => setShowAdmin(false)}>Home</button>}
                <button className="logout-btn" onClick={onLogout}>Logout</button>
              </div>
            )}
          </div>
        </header>

        {!(currentUser?.isAdmin || adminAccessGranted) && (
          <AdminLockScreen
            adminAuthForm={adminAuthForm}
            setAdminAuthForm={setAdminAuthForm}
            onVerifyAdminAccess={onVerifyAdminAccess}
            adminAuthMessage={adminAuthMessage}
          />
        )}
        {(currentUser?.isAdmin || adminAccessGranted) && (
          <AdminPage
            customers={customers}
            accounts={accounts}
            transactions={adminTransactions}
            scheduledBills={scheduledBills}
            loanApplications={loanApplications}
            summaries={summaries}
            selectedAccountForTx={selectedAccountForTx}
            setSelectedAccountForTx={setSelectedAccountForTx}
            adminAccountForm={adminAccountForm}
            setAdminAccountForm={setAdminAccountForm}
            onCreateAdminAccount={onCreateAdminAccount}
            adminAccountMessage={adminAccountMessage}
            adminMessage={adminMessage}
            onAdminUpdateCustomer={onAdminUpdateCustomer}
            onAdminUpdateAccount={onAdminUpdateAccount}
            onAdminFreezeAccount={onAdminFreezeAccount}
            onAdminUpdateLoanStatus={onAdminUpdateLoanStatus}
            adminTransferLimit={adminTransferLimit}
            setAdminTransferLimit={setAdminTransferLimit}
            onAdminUpdateTransferLimit={onAdminUpdateTransferLimit}
            onAdminReverseTransaction={onAdminReverseTransaction}
            adminLoginLogs={adminLoginLogs}
            adminNotificationLogs={adminNotificationLogs}
            adminReport={adminReport}
            adminLastUpdated={adminLastUpdated}
          />
        )}

        <SiteFooter currentYear={currentYear} />
      </div>
    );
  }
  // ─────────────────────────────────────────────────────────────────────────

  return (
    <div className="app-shell">
      <header className="hero">
        <div className="hero-row">
          <div>
            <h1>Bank of Fiji Online Banking</h1>
            <p>Home Dashboard</p>
          </div>
          {currentUser && (
            <div className="hero-user">
              <span>Welcome, <strong>{currentUser.fullName}</strong></span>
              {!isAdminUser && <button className="admin-btn" onClick={() => { setShowAdmin(true); setAdminAuthMessage(""); }}>Admin</button>}
              <button className="logout-btn" onClick={onLogout}>Logout</button>
            </div>
          )}
        </div>
      </header>

      <div className="workspace-layout">
        <aside className="left-tabs">
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
        </aside>

        <section className="tab-content">
          {loading && <p className="status">Loading data...</p>}
          {error && <p className="status error">{error}</p>}

          {!loading && activeTab === "Overview" && (
            <HomePage
              accounts={accounts}
              totalBalance={totalBalance}
              selectedAccountForTx={selectedAccountForTx}
              setSelectedAccountForTx={setSelectedAccountForTx}
              transactions={transactions}
            />
          )}

          {!loading && activeTab === "Accounts" && (
            <AccountsTab
              accounts={accounts}
              customers={customers}
              customerMap={customerMap}
              currentUser={currentUser}
              accountMessage={accountMessage}
              setAccountMessage={setAccountMessage}
              onCreateAccount={onCreateAdminAccount}
            />
          )}

          {!loading && activeTab === "Transfers" && (
            <TransfersTab
              accounts={accounts}
              transferForm={transferForm}
              setTransferForm={setTransferForm}
              onInitiateTransfer={onInitiateTransfer}
              pendingTransfer={pendingTransfer}
              setPendingTransfer={setPendingTransfer}
              onVerifyTransfer={onVerifyTransfer}
              transferMessage={transferMessage}
            />
          )}

          {!loading && activeTab === "Bill Payments" && (
            <BillPaymentsTab
              accounts={accounts}
              manualBillForm={manualBillForm}
              setManualBillForm={setManualBillForm}
              onManualBill={onManualBill}
              scheduleBillForm={scheduleBillForm}
              setScheduleBillForm={setScheduleBillForm}
              onScheduleBill={onScheduleBill}
              scheduledBills={scheduledBills}
              runScheduledBill={runScheduledBill}
              billMessage={billMessage}
            />
          )}

          {!loading && activeTab === "Statements" && (
            <StatementsTab
              accounts={accounts}
              customers={customers}
              statementAccount={statementAccount}
              setStatementAccount={setStatementAccount}
              statementRows={statementRows}
              statementRequested={statementRequested}
              fetchStatement={fetchStatement}
              notificationCustomer={notificationCustomer}
              setNotificationCustomer={setNotificationCustomer}
              notifications={notifications}
            />
          )}

          {!loading && activeTab === "Investments" && (
            <InvestmentsTab
              customers={customers}
              customerMap={customerMap}
              investments={investments}
              investmentForm={investmentForm}
              setInvestmentForm={setInvestmentForm}
              onAddInvestment={onAddInvestment}
              investmentMessage={investmentMessage}
            />
          )}

          {!loading && activeTab === "Loans" && (
            <LoansTab
              customers={customers}
              customerMap={customerMap}
              loanProducts={loanProducts}
              loanApplications={loanApplications}
              loanForm={loanForm}
              setLoanForm={setLoanForm}
              onSubmitLoan={onSubmitLoan}
              loanMessage={loanMessage}
            />
          )}

          {!loading && activeTab === "Profile" && (
            <ProfileTab
              profileForm={profileForm}
              setProfileForm={setProfileForm}
              onUpdateProfile={onUpdateProfile}
              profileMessage={profileMessage}
            />
          )}

          {!loading && activeTab === "Compliance" && (
            <ComplianceTab
              interestRate={interestRate}
              setInterestRate={setInterestRate}
              onUpdateRate={onUpdateRate}
              summaryYear={summaryYear}
              setSummaryYear={setSummaryYear}
              onGenerateSummaries={onGenerateSummaries}
              summaries={summaries}
              complianceMessage={complianceMessage}
            />
          )}
        </section>
      </div>

      <SiteFooter currentYear={currentYear} />
    </div>
  );
}