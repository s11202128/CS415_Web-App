export default function AdminMonitoringTab({
  accounts,
  transactions,
  selectedAccountForTx,
  setSelectedAccountForTx,
  adminTransferLimit,
  setAdminTransferLimit,
  onAdminUpdateTransferLimit,
  onAdminReverseTransaction,
  adminLoginLogs,
  adminNotificationLogs,
}) {
  return (
    <section className="panel-grid">
      <article className="panel wide">
        <h3>Transaction Monitoring</h3>
        <div className="inline-controls">
          <label>
            Filter by Account
            <select value={selectedAccountForTx} onChange={(e) => setSelectedAccountForTx(e.target.value)}>
              {accounts.map((a) => (
                <option key={a.id} value={a.id}>{a.id}</option>
              ))}
            </select>
          </label>
        </div>
        <table>
          <thead>
            <tr>
              <th>Time</th>
              <th>Type</th>
              <th>Amount</th>
              <th>Description</th>
              <th>Risk</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {transactions.map((t) => (
              <tr key={t.id}>
                <td>{new Date(t.createdAt).toLocaleString()}</td>
                <td>{t.kind}</td>
                <td>FJD {Number(t.amount).toFixed(2)}</td>
                <td>{t.description}</td>
                <td>{t.suspicious ? "Flagged" : "Normal"}</td>
                <td>
                  <button type="button" disabled={t.status === "reversed"} onClick={() => onAdminReverseTransaction(t.id)}>
                    Reverse
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </article>

      <article className="panel">
        <h3>Transfer Controls</h3>
        <form onSubmit={onAdminUpdateTransferLimit}>
          <label>
            High-value transfer limit (FJD)
            <input
              type="number"
              min="1"
              value={adminTransferLimit}
              onChange={(e) => setAdminTransferLimit(e.target.value)}
            />
          </label>
          <button type="submit">Update Limit</button>
        </form>
        <p className="hint">Transfers at or above this amount require OTP verification.</p>
      </article>

      <article className="panel">
        <h3>Notification Log</h3>
        <ul className="feed">
          {adminNotificationLogs.slice(0, 14).map((log) => (
            <li key={log.id}>
              <strong>{new Date(log.createdAt).toLocaleString()}:</strong> {log.message}
            </li>
          ))}
        </ul>
      </article>

      <article className="panel wide">
        <h3>Login Activity</h3>
        <table>
          <thead>
            <tr>
              <th>Time</th>
              <th>User Type</th>
              <th>Email</th>
              <th>Result</th>
              <th>Reason</th>
            </tr>
          </thead>
          <tbody>
            {adminLoginLogs.map((log) => (
              <tr key={log.id}>
                <td>{new Date(log.createdAt).toLocaleString()}</td>
                <td>{log.userType}</td>
                <td>{log.email}</td>
                <td>{log.success ? "Success" : "Failed"}</td>
                <td>{log.failureReason || "-"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </article>
    </section>
  );
}
