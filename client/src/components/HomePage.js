import { homepageReviews } from "../data/homepageReviews";

export default function HomePage({
  customers,
  accounts,
  totalBalance,
  customerMap,
  selectedAccountForTx,
  setSelectedAccountForTx,
  transactions,
}) {
  return (
    <section className="panel-grid">
      <article className="panel">
        <h2>Customers</h2>
        <p className="metric">{customers.length}</p>
      </article>
      <article className="panel">
        <h2>Accounts</h2>
        <p className="metric">{accounts.length}</p>
      </article>
      <article className="panel">
        <h2>Total Deposits</h2>
        <p className="metric">FJD {totalBalance.toFixed(2)}</p>
      </article>

      <article className="panel wide">
        <h2>Account Snapshot</h2>
        <table>
          <thead>
            <tr>
              <th>Account</th>
              <th>Customer</th>
              <th>Type</th>
              <th>Balance</th>
              <th>Monthly Fee</th>
            </tr>
          </thead>
          <tbody>
            {accounts.map((a) => (
              <tr key={a.id}>
                <td>{a.id}</td>
                <td>{customerMap[a.customerId]?.fullName || a.customerId}</td>
                <td>{a.type}</td>
                <td>FJD {a.balance.toFixed(2)}</td>
                <td>FJD {a.maintenanceFee.toFixed(2)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </article>

      <article className="panel wide">
        <h2>Transactions</h2>
        <div className="inline-controls">
          <label>
            Account
            <select value={selectedAccountForTx} onChange={(e) => setSelectedAccountForTx(e.target.value)}>
              {accounts.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.id} ({a.type})
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

      <article className="panel wide review-panel">
        <h2>Customer Reviews</h2>
        <div className="review-grid">
          {homepageReviews.map((review) => (
            <blockquote key={review.id} className="review-card">
              <p>{review.quote}</p>
              <footer>{review.author}</footer>
            </blockquote>
          ))}
        </div>
      </article>
    </section>
  );
}
