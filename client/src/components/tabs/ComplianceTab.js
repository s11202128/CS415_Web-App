export default function ComplianceTab({
  interestRate,
  setInterestRate,
  onUpdateRate,
  summaryYear,
  setSummaryYear,
  onGenerateSummaries,
  summaries,
  complianceMessage,
}) {
  return (
    <section className="panel-grid">
      <article className="panel">
        <h2>Savings Interest Rate Config</h2>
        <form onSubmit={onUpdateRate}>
          <label>
            Reserve Bank Minimum Savings Interest Rate (decimal)
            <input
              type="number"
              step="0.0001"
              value={interestRate}
              onChange={(e) => setInterestRate(e.target.value)}
              required
            />
          </label>
          <button type="submit">Update Rate</button>
        </form>
      </article>

      <article className="panel">
        <h2>Generate Year-End Interest Summaries</h2>
        <label>
          Year
          <input type="number" value={summaryYear} onChange={(e) => setSummaryYear(e.target.value)} />
        </label>
        <button onClick={onGenerateSummaries}>Generate + Submit to FRCS</button>
        <p className="status">{complianceMessage}</p>
      </article>

      <article className="panel wide">
        <h2>Interest Summaries</h2>
        <table>
          <thead>
            <tr>
              <th>Account</th>
              <th>Customer</th>
              <th>Year</th>
              <th>Gross</th>
              <th>Withholding Tax</th>
              <th>Net</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {summaries.map((s) => (
              <tr key={s.id}>
                <td>{s.accountId}</td>
                <td>{s.customerName}</td>
                <td>{s.year}</td>
                <td>FJD {s.grossInterest.toFixed(2)}</td>
                <td>FJD {s.withholdingTax.toFixed(2)}</td>
                <td>FJD {s.netInterest.toFixed(2)}</td>
                <td>{s.status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </article>
    </section>
  );
}
