export default function HomePage({
  customers,
  accounts,
  totalBalance,
  customerMap,
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
        <h2>Selected Account</h2>
        <p className="metric">{selectedAccount ? selectedAccount.id : "N/A"}</p>
        <p className="hint">
          {selectedAccount ? `${selectedAccount.type} • FJD ${Number(selectedAccount.balance || 0).toFixed(2)}` : "Choose an account below"}
        </p>
      </article>

      <article className="panel wide">
        <h2>Transactions</h2>
        <div className="inline-controls">
          <label>
            Account
            <select value={selectedAccountForTx} onChange={(e) => setSelectedAccountForTx(e.target.value)}>
              {accounts.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.id} ({a.type}) - {customerMap[a.customerId]?.fullName || a.customerId}
                </option>
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
                <td>FJD {t.amount.toFixed(2)}</td>
                <td>{t.description}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </article>
    </section>
  );
}
