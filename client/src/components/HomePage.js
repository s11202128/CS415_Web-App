export default function HomePage({
  accounts,
  totalBalance,
  selectedAccountForTx,
  setSelectedAccountForTx,
  transactions,
}) {
  const selectedAccount = accounts.find((a) => String(a.id) === String(selectedAccountForTx));

  return (
    <section className="panel-grid">
      <article className="panel">
        <h2>My Accounts</h2>
        <p className="metric">{accounts.length}</p>
      </article>
      <article className="panel">
        <h2>Portfolio Balance</h2>
        <p className="metric">FJD {totalBalance.toFixed(2)}</p>
      </article>
      <article className="panel">
        <h2>Individual Account Balance</h2>
        <label>
          Choose Account
          <select value={selectedAccountForTx} onChange={(e) => setSelectedAccountForTx(e.target.value)}>
            {accounts.map((a) => (
              <option key={a.id} value={a.id}>
                {a.id} ({a.type})
              </option>
            ))}
          </select>
        </label>
        <p className="metric">FJD {selectedAccount ? Number(selectedAccount.balance || 0).toFixed(2) : "0.00"}</p>
        <p className="hint">Showing current saved balance for selected account.</p>
      </article>

      <article className="panel wide">
        <h2>My Activity Overview</h2>
        <table>
          <thead>
            <tr>
              <th>Time</th>
              <th>Account</th>
              <th>Type</th>
              <th>Amount</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            {transactions.map((t) => (
              <tr key={t.id}>
                <td>{new Date(t.createdAt).toLocaleString()}</td>
                <td>{t.accountId}</td>
                <td>{t.kind}</td>
                <td>FJD {t.amount.toFixed(2)}</td>
                <td>{t.description}</td>
              </tr>
            ))}
            {transactions.length === 0 && (
              <tr>
                <td colSpan="5" className="no-data">No account activity available yet.</td>
              </tr>
            )}
          </tbody>
        </table>
      </article>
    </section>
  );
}
