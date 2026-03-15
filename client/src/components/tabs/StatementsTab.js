import { useState } from "react";

export default function StatementsTab({
  accounts,
  customers,
  currentUser,
  statementAccount,
  setStatementAccount,
  statementRows,
  statementRequested,
  statementRequests,
  statementMessage,
  setStatementMessage,
  fetchStatement,
  onSubmitStatementRequest,
  onDownloadStatement,
  notificationCustomer,
  setNotificationCustomer,
  notifications,
}) {
  const [filterType, setFilterType] = useState("all");
  const [sortOrder, setSortOrder] = useState("desc");
  const [requestForm, setRequestForm] = useState({ fromDate: "", toDate: "" });

  const selectedAccount = accounts.find((account) => String(account.id) === String(statementAccount));

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

  const approvedRequests = statementRequests.filter((request) => request.status === "approved");

  const handleSubmitStatementRequest = async (e) => {
    e.preventDefault();
    if (!selectedAccount) {
      setStatementMessage("Please select an account before submitting a statement request.");
      return;
    }
    await onSubmitStatementRequest({
      accountId: selectedAccount.id,
      accountHolder: selectedAccount.accountHolder || currentUser?.fullName || "",
      accountNumber: selectedAccount.accountNumber || "",
      fromDate: requestForm.fromDate,
      toDate: requestForm.toDate,
    });
    setRequestForm({ fromDate: "", toDate: "" });
  };

  return (
    <section className="panel-grid">
      <article className="panel wide">
        <h2>Approved Statement Preview</h2>
        <div className="inline-controls">
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
          <p className="hint">Approved requests: {approvedRequests.length}</p>
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

        {!statementRequested ? (
          <p className="no-data">Submit a request and wait for admin approval before previewing statement data.</p>
        ) : sortedRows.length === 0 ? (
          <p className="no-data">No transactions found for the selected account.</p>
        ) : null}
      </article>

      <article className="panel wide">
        <h2>Statement Request Form</h2>
        <form className="admin-form" onSubmit={handleSubmitStatementRequest}>
          <label>
            Account Holder
            <input value={selectedAccount?.accountHolder || currentUser?.fullName || ""} readOnly />
          </label>
          <label>
            Account Number
            <select value={statementAccount} onChange={(e) => setStatementAccount(e.target.value)}>
              {accounts.map((a) => (
                <option key={a.id} value={a.id}>{a.accountNumber || `ID ${a.id}`}</option>
              ))}
            </select>
          </label>
          <label>
            From Date
            <input
              type="date"
              value={requestForm.fromDate}
              onChange={(e) => setRequestForm({ ...requestForm, fromDate: e.target.value })}
              required
            />
          </label>
          <label>
            To Date
            <input
              type="date"
              value={requestForm.toDate}
              onChange={(e) => setRequestForm({ ...requestForm, toDate: e.target.value })}
              required
            />
          </label>
          <button type="submit">Submit Statement Request</button>
        </form>
        {statementMessage && <p className="status">{statementMessage}</p>}
      </article>

      <article className="panel wide">
        <h2>My Statement Requests</h2>
        <table>
          <thead>
            <tr>
              <th>Requested</th>
              <th>Account Holder</th>
              <th>Account Number</th>
              <th>From</th>
              <th>To</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {statementRequests.map((request) => {
              const isApproved = request.status === "approved";
              return (
                <tr key={request.id}>
                  <td>{new Date(request.createdAt).toLocaleString()}</td>
                  <td>{request.accountHolder}</td>
                  <td>{request.accountNumber}</td>
                  <td>{request.fromDate}</td>
                  <td>{request.toDate}</td>
                  <td>{request.status}</td>
                  <td>
                    <div className="inline-controls">
                      <button type="button" disabled={!isApproved} onClick={() => fetchStatement(request.id)}>
                        View
                      </button>
                      <button type="button" disabled={!isApproved} onClick={() => onDownloadStatement(request.id)}>
                        Download CSV
                      </button>
                    </div>
                  </td>
                </tr>
              );
            })}
            {statementRequests.length === 0 && (
              <tr>
                <td colSpan="7" className="no-data">No statement requests submitted yet.</td>
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
