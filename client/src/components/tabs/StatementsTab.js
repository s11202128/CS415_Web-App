import { useState } from "react";
import { api } from "../../api";

export default function StatementsTab({
  accounts,
  customers,
  statementAccount,
  setStatementAccount,
  statementRows,
  statementRequested,
  fetchStatement,
  notificationCustomer,
  setNotificationCustomer,
  notifications,
}) {
  const [filterType, setFilterType] = useState("all");
  const [sortOrder, setSortOrder] = useState("desc");

  // Filter transactions based on type
  const filteredRows = statementRows.filter((r) => {
    if (filterType === "all") return true;
    return r.kind === filterType;
  });

  // Sort transactions
  const sortedRows = [...filteredRows].sort((a, b) => {
    const dateA = new Date(a.createdAt).getTime();
    const dateB = new Date(b.createdAt).getTime();
    return sortOrder === "desc" ? dateB - dateA : dateA - dateB;
  });

  // Calculate summary statistics
  const totalCredit = filteredRows
    .filter((r) => r.kind === "credit")
    .reduce((sum, r) => sum + r.amount, 0);
  const totalDebit = filteredRows
    .filter((r) => r.kind === "debit")
    .reduce((sum, r) => sum + r.amount, 0);

  return (
    <section className="panel-grid">
      <article className="panel wide">
        <h2>Transaction Statements & History</h2>
        <div className="inline-controls">
          <label>
            Account
            <select value={statementAccount} onChange={(e) => setStatementAccount(e.target.value)}>
              {accounts.map((a) => (
                <option key={a.id} value={a.id}>{a.accountNumber || a.id}</option>
              ))}
            </select>
          </label>
          <label>
            Filter by Type
            <select value={filterType} onChange={(e) => setFilterType(e.target.value)}>
              <option value="all">All Transactions</option>
              <option value="credit">Credit (Deposits)</option>
              <option value="debit">Debit (Withdrawals)</option>
            </select>
          </label>
          <label>
            Sort
            <select value={sortOrder} onChange={(e) => setSortOrder(e.target.value)}>
              <option value="desc">Newest First</option>
              <option value="asc">Oldest First</option>
            </select>
          </label>
          <button onClick={fetchStatement}>View Statement</button>
          <a
            className="button-link"
            href={statementRequested ? api.statementDownloadUrl(statementAccount) : undefined}
            target="_blank"
            rel="noreferrer"
            onClick={(e) => {
              if (!statementRequested) {
                e.preventDefault();
              }
            }}
            aria-disabled={!statementRequested}
          >
            📥 Download CSV
          </a>
        </div>

        {statementRequested && statementRows.length > 0 && (
          <div className="statement-summary">
            <div className="summary-card">
              <strong>Total Credits:</strong> FJD {totalCredit.toFixed(2)}
            </div>
            <div className="summary-card">
              <strong>Total Debits:</strong> FJD {totalDebit.toFixed(2)}
            </div>
            <div className="summary-card">
              <strong>Net Change:</strong> FJD {(totalCredit - totalDebit).toFixed(2)}
            </div>
            <div className="summary-card">
              <strong>Transaction Count:</strong> {sortedRows.length}
            </div>
          </div>
        )}

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
            {!statementRequested ? (
              <tr>
                <td colSpan="4" className="no-data">Select an account and click "View Statement" to load transactions on demand.</td>
              </tr>
            ) : sortedRows.length > 0 ? (
              sortedRows.map((r) => (
                <tr key={r.id} className={`tx-${r.kind}`}>
                  <td>{new Date(r.createdAt).toLocaleString()}</td>
                  <td>
                    <span className={`badge badge-${r.kind}`}>
                      {r.kind === "credit" ? "+" : "-"} {r.kind}
                    </span>
                  </td>
                  <td className={`amount-${r.kind}`}>FJD {r.amount.toFixed(2)}</td>
                  <td>{r.description}</td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="4" className="no-data">No transactions found for the selected account.</td>
              </tr>
            )}
          </tbody>
        </table>
      </article>

      <article className="panel">
        <h2>📱 SMS Notifications</h2>
        <p className="hint">Real-time banking alerts and confirmations</p>
        <label>
          Customer
          <select value={notificationCustomer} onChange={(e) => setNotificationCustomer(e.target.value)}>
            {customers.map((c) => (
              <option key={c.id} value={c.id}>{c.fullName}</option>
            ))}
          </select>
        </label>
        <div className="notification-count">
          {notifications.length} notification(s)
        </div>
        <ul className="feed">
          {notifications.length > 0 ? (
            notifications.map((n) => (
              <li key={n.id} className="notification-item">
                <strong>{new Date(n.createdAt).toLocaleString()}:</strong>
                <p>{n.message}</p>
              </li>
            ))
          ) : (
            <li className="no-data">No notifications yet</li>
          )}
        </ul>
      </article>
    </section>
  );
}
