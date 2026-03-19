import { useState } from "react";
import { api } from "../../api";

const ACCOUNT_SECTIONS = ["Summary", "My Accounts", "Open Account", "Account Types", "Tips"];

export default function AccountsTab({
  accounts,
  currentUser,
  accountMessage,
  setAccountMessage,
}) {
  const [activeSection, setActiveSection] = useState("Summary");
  const [newAccountForm, setNewAccountForm] = useState({
    type: "Savings",
    openingBalance: "0",
    accountNumber: "",
  });

  const activeCustomerId = currentUser?.customerId || currentUser?.userId || currentUser?.id || "";

  // Filter accounts for current user
  const userAccounts = currentUser
    ? accounts.filter((a) => String(a.customerId) === String(activeCustomerId))
    : [];

  const handleCreateAccount = async (e) => {
    e.preventDefault();
    setAccountMessage("");
    if (!activeCustomerId) {
      setAccountMessage("❌ Unable to determine your customer ID. Please log out and sign in again.");
      return;
    }
    try {
      await api.createAccountRequest({
        customerId: Number(activeCustomerId),
        type: newAccountForm.type,
        openingBalance: Number(newAccountForm.openingBalance || 0),
        accountNumber: newAccountForm.accountNumber || undefined,
      });
      setAccountMessage("✅ Account request submitted. It will be activated after admin approval.");
      setNewAccountForm({ type: "Savings", openingBalance: "0", accountNumber: "" });
      // Refresh would be done by parent component
      setTimeout(() => setAccountMessage(""), 3000);
    } catch (err) {
      setAccountMessage(`❌ ${err.message}`);
    }
  };

  const getAccountTypeDescription = (type) => {
    switch (type) {
      case "Simple Access":
      case "Cheque":
        return {
          desc: "Everyday transaction account for payments and transfers",
          fee: "FJD 2.50/month",
          interest: "None",
          bestFor: "Daily transactions and cheque access",
        };
      case "Savings":
        return {
          desc: "Interest-bearing savings account",
          fee: "None",
          interest: "3.25% p.a.",
          bestFor: "Building savings with interest",
        };
      default:
        return { desc: "Unknown type", fee: "N/A", interest: "N/A", bestFor: "N/A" };
    }
  };

  const accountStats = userAccounts.reduce(
    (acc, a) => ({
      total: acc.total + (a.status === "active" ? a.balance : 0),
      count: acc.count + 1,
    }),
    { total: 0, count: 0 }
  );

  return (
    <section className="panel-grid">
      <article className="panel wide">
        {/* Horizontal section switcher */}
        <nav className="acct-tab-bar">
          {ACCOUNT_SECTIONS.map((s) => (
            <button
              key={s}
              type="button"
              className={`acct-tab-btn${activeSection === s ? " active" : ""}`}
              onClick={() => setActiveSection(s)}
            >
              {s}
            </button>
          ))}
        </nav>

        <div className="acct-tab-body">

          {activeSection === "Summary" && (
            <>
              <div className="account-summary">
                <div className="summary-item">
                  <span className="label">Total Accounts:</span>
                  <span className="value">{accountStats.count}</span>
                </div>
                <div className="summary-item">
                  <span className="label">Combined Balance:</span>
                  <span className="value highlight">FJD {accountStats.total.toFixed(2)}</span>
                </div>
                <div className="summary-item">
                  <span className="label">Account Owner:</span>
                  <span className="value">{currentUser?.fullName || "N/A"}</span>
                </div>
              </div>
              <p className="hint">New account requests must be approved by admin before becoming active.</p>
            </>
          )}

          {activeSection === "My Accounts" && (
            userAccounts.length === 0 ? (
              <p className="no-data">You have no accounts yet. Open a new account to get started.</p>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>Account #</th>
                    <th>Account Holder</th>
                    <th>Type</th>
                    <th>Balance</th>
                    <th>Fee/Interest</th>
                    <th>Status</th>
                    <th>Created</th>
                  </tr>
                </thead>
                <tbody>
                  {userAccounts.map((a) => {
                    const typeInfo = getAccountTypeDescription(a.type);
                    return (
                      <tr key={a.id} className={`account-row account-${a.status}`}>
                        <td className="account-number">{a.accountNumber}</td>
                        <td>{a.accountHolder || currentUser?.fullName || "N/A"}</td>
                        <td>
                          <div>
                            <strong>{a.type === "Simple Access" ? "Cheque" : a.type}</strong>
                            <p className="hint">{typeInfo.desc}</p>
                          </div>
                        </td>
                        <td className="balance">
                          {a.status === "active" ? (
                            <strong>FJD {Number(a.balance).toFixed(2)}</strong>
                          ) : (
                            <span className="balance-pending" title="Awaiting admin approval">—</span>
                          )}
                        </td>
                        <td className="fee-interest">
                          <div>
                            <p>💰 {typeInfo.interest}</p>
                            <p>💳 {typeInfo.fee}</p>
                          </div>
                        </td>
                        <td>
                          <span className={`status-badge status-${a.status}`}>
                            {a.status}
                          </span>
                        </td>
                        <td className="date">
                          {new Date(a.createdAt).toLocaleDateString()}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )
          )}

          {activeSection === "Open Account" && (
            <>
              <form className="admin-form" onSubmit={handleCreateAccount}>
                <label>
                  Customer ID
                  <input value={activeCustomerId} readOnly />
                </label>
                <label>
                  Account Type
                  <select
                    value={newAccountForm.type}
                    onChange={(e) => setNewAccountForm({ ...newAccountForm, type: e.target.value })}
                    required
                  >
                    <option value="Simple Access">Cheque</option>
                    <option value="Savings">Savings</option>
                  </select>
                </label>
                <label>
                  Opening Balance
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={newAccountForm.openingBalance}
                    onChange={(e) => setNewAccountForm({ ...newAccountForm, openingBalance: e.target.value })}
                  />
                </label>
                <label>
                  Account Number (optional — 12 digits)
                  <input
                    value={newAccountForm.accountNumber}
                    onChange={(e) => setNewAccountForm({ ...newAccountForm, accountNumber: e.target.value })}
                    placeholder="Leave blank to auto-generate"
                  />
                </label>
                <button type="submit">Submit Request</button>
              </form>
              {accountMessage && (
                <p className={`status ${accountMessage.includes("✅") ? "success" : "error"}`}>
                  {accountMessage}
                </p>
              )}
            </>
          )}

          {activeSection === "Account Types" && (
            <div className="account-types-grid">
              {["Cheque", "Savings"].map((type) => {
                const info = getAccountTypeDescription(type);
                return (
                  <div key={type} className="account-type-card">
                    <h3>{type}</h3>
                    <p className="description">{info.desc}</p>
                    <dl>
                      <dt>Monthly Fee:</dt>
                      <dd>{info.fee}</dd>
                      <dt>Annual Interest:</dt>
                      <dd>{info.interest}</dd>
                      <dt>Best For:</dt>
                      <dd>{info.bestFor}</dd>
                    </dl>
                  </div>
                );
              })}
            </div>
          )}

          {activeSection === "Tips" && (
            <ul className="tips-list">
              <li><strong>Cheque Account:</strong> Pay FJD 2.50 per month for instant access to funds.</li>
              <li><strong>Savings Account:</strong> Earn 3.25% annual interest on your balance.</li>
              <li><strong>Multiple Accounts:</strong> You can open multiple accounts for organization.</li>
              <li><strong>Transfers:</strong> Move money between your own accounts instantly.</li>
              <li><strong>Account Number:</strong> Always required for receiving transfers from others.</li>
            </ul>
          )}

        </div>
      </article>
    </section>
  );
}
