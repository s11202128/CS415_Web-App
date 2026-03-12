export default function LoansTab({
  customers,
  customerMap,
  loanProducts,
  loanApplications,
  loanForm,
  setLoanForm,
  onSubmitLoan,
  loanMessage,
}) {
  return (
    <section className="panel-grid">
      <article className="panel wide">
        <h2>Loan Products (Website Advertisement)</h2>
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Annual Rate</th>
              <th>Max Amount</th>
              <th>Term</th>
            </tr>
          </thead>
          <tbody>
            {loanProducts.map((lp) => (
              <tr key={lp.id}>
                <td>{lp.name}</td>
                <td>{(lp.annualRate * 100).toFixed(2)}%</td>
                <td>FJD {lp.maxAmount.toFixed(2)}</td>
                <td>{lp.minTermMonths}-{lp.maxTermMonths} months</td>
              </tr>
            ))}
          </tbody>
        </table>
      </article>

      <article className="panel">
        <h2>Apply For Loan</h2>
        <form onSubmit={onSubmitLoan}>
          <label>
            Customer
            <select value={loanForm.customerId} onChange={(e) => setLoanForm({ ...loanForm, customerId: e.target.value })} required>
              <option value="">Select</option>
              {customers.map((c) => (
                <option key={c.id} value={c.id}>{c.fullName}</option>
              ))}
            </select>
          </label>
          <label>
            Loan Product
            <select
              value={loanForm.loanProductId}
              onChange={(e) => setLoanForm({ ...loanForm, loanProductId: e.target.value })}
              required
            >
              <option value="">Select</option>
              {loanProducts.map((lp) => (
                <option key={lp.id} value={lp.id}>{lp.name}</option>
              ))}
            </select>
          </label>
          <label>
            Requested Amount
            <input
              type="number"
              min="1"
              value={loanForm.requestedAmount}
              onChange={(e) => setLoanForm({ ...loanForm, requestedAmount: e.target.value })}
              required
            />
          </label>
          <label>
            Term (months)
            <input
              type="number"
              min="1"
              value={loanForm.termMonths}
              onChange={(e) => setLoanForm({ ...loanForm, termMonths: e.target.value })}
              required
            />
          </label>
          <label>
            Purpose
            <input value={loanForm.purpose} onChange={(e) => setLoanForm({ ...loanForm, purpose: e.target.value })} required />
          </label>
          <label>
            Monthly Income
            <input
              type="number"
              min="0"
              value={loanForm.monthlyIncome}
              onChange={(e) => setLoanForm({ ...loanForm, monthlyIncome: e.target.value })}
            />
          </label>
          <label>
            Employment Status
            <input
              value={loanForm.employmentStatus}
              onChange={(e) => setLoanForm({ ...loanForm, employmentStatus: e.target.value })}
            />
          </label>
          <button type="submit">Submit Application</button>
        </form>
        <p className="status">{loanMessage}</p>
      </article>

      <article className="panel wide">
        <h2>Submitted Loan Applications</h2>
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Customer</th>
              <th>Product</th>
              <th>Amount</th>
              <th>Term</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {loanApplications.map((a) => (
              <tr key={a.id}>
                <td>{a.id}</td>
                <td>{customerMap[a.customerId]?.fullName || a.customerId}</td>
                <td>{loanProducts.find((p) => p.id === a.loanProductId)?.name || a.loanProductId}</td>
                <td>FJD {a.requestedAmount.toFixed(2)}</td>
                <td>{a.termMonths}</td>
                <td>{a.status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </article>
    </section>
  );
}
