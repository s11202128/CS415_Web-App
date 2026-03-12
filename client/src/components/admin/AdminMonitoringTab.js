export default function AdminMonitoringTab({
  accounts,
  transactions,
  selectedAccountForTx,
  setSelectedAccountForTx,
  adminTransferLimit,
  setAdminTransferLimit,
  onAdminUpdateTransferLimit,
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
            </tr>
          </thead>
          <tbody>
            {transactions.map((t) => (
              <tr key={t.id}>
                <td>{new Date(t.createdAt).toLocaleString()}</td>
                <td>{t.kind}</td>
                <td>FJD {Number(t.amount).toFixed(2)}</td>
                <td>{t.description}</td>
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
    </section>
  );
}
