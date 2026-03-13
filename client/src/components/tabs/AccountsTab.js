import { useState } from "react";
import { api } from "../../api";

export default function AccountsTab({
  accounts,
  customers,
  customerMap,
  currentUser,
  accountMessage,
  setAccountMessage,
  onCreateAccount,
}) {
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [newAccountForm, setNewAccountForm] = useState({
    type: "Savings",
    openingBalance: "0",
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
      await api.createAccount({
        customerId: Number(activeCustomerId),
        type: newAccountForm.type,
        openingBalance: Number(newAccountForm.openingBalance || 0),
      });
      setAccountMessage("✅ Account created successfully!");
      setNewAccountForm({ type: "Savings", openingBalance: "0" });
      setShowCreateForm(false);
      // Refresh would be done by parent component
      setTimeout(() => setAccountMessage(""), 3000);
    } catch (err) {
      setAccountMessage(`❌ ${err.message}`);
    }
  };

  const getAccountTypeDescription = (type) => {
    switch (type) {
      case "Simple Access":
        return {
          desc: "Basic daily banking account",
          fee: "FJD 2.50/month",
          interest: "None",
          bestFor: "General spending & savings",
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
      total: acc.total + a.balance,
      count: acc.count + 1,
    }),
    { total: 0, count: 0 }
  );

  return (
    <section className="panel-grid">
      <article className="panel">
        <h2>💳 Account Summary</h2>
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
        <button
          className="primary-btn"
          onClick={() => setShowCreateForm(!showCreateForm)}
          style={{ marginTop: "16px", width: "100%" }}
        >
          {showCreateForm ? "Cancel" : "+ Open New Account"}
        </button>
      </article>

      {showCreateForm && (
        <article className="panel">
          <h2>📋 Open New Account</h2>
          <form onSubmit={handleCreateAccount}>
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
                <option value="Simple Access">Simple Access (FJD 2.50/month fee)</option>
                <option value="Savings">Savings (3.25% p.a. interest)</option>
              </select>
            </label>
            <label>
              Opening Balance (FJD)
              <input
                type="number"
                min="0"
                step="0.01"
                value={newAccountForm.openingBalance}
                onChange={(e) => setNewAccountForm({ ...newAccountForm, openingBalance: e.target.value })}
                required
              />
            </label>
            <button type="submit" className="success-btn">Create Account</button>
            {accountMessage && (
              <p className={`status ${accountMessage.includes("✅") ? "success" : "error"}`}>
                {accountMessage}
              </p>
            )}
          </form>
        </article>
      )}

      <article className="panel wide">
        <h2>📊 Your Accounts</h2>
        {userAccounts.length === 0 ? (
          <p className="no-data">You have no accounts yet. Open a new account to get started.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Account #</th>
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
                    <td>
                      <div>
                        <strong>{a.type}</strong>
                        <p className="hint">{typeInfo.desc}</p>
                      </div>
                    </td>
                    <td className="balance">
                      <strong>FJD {Number(a.balance).toFixed(2)}</strong>
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
        )}
      </article>

      <article className="panel wide">
        <h2>📚 Account Types Explained</h2>
        <div className="account-types-grid">
          {["Simple Access", "Savings"].map((type) => {
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
      </article>

      <article className="panel">
        <h2>❓ Account Management Tips</h2>
        <ul className="tips-list">
          <li><strong>Simple Access:</strong> Pay FJD 2.50 per month for instant access to funds.</li>
          <li><strong>Savings Account:</strong> Earn 3.25% annual interest on your balance.</li>
          <li><strong>Multiple Accounts:</strong> You can open multiple accounts for organization.</li>
          <li><strong>Transfers:</strong> Move money between your own accounts instantly.</li>
          <li><strong>Account Number:</strong> Always required for receiving transfers from others.</li>
        </ul>
      </article>
    </section>
  );
}
