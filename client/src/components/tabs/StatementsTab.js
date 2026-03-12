import { api } from "../../api";

export default function StatementsTab({
  accounts,
  customers,
  statementAccount,
  setStatementAccount,
  statementRows,
  fetchStatement,
  notificationCustomer,
  setNotificationCustomer,
  notifications,
}) {
  return (
    <section className="panel-grid">
      <article className="panel wide">
        <h2>On-Demand Statements</h2>
        <div className="inline-controls">
          <label>
            Account
            <select value={statementAccount} onChange={(e) => setStatementAccount(e.target.value)}>
              {accounts.map((a) => (
                <option key={a.id} value={a.id}>{a.id}</option>
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
              <option key={c.id} value={c.id}>{c.fullName}</option>
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
  );
}
