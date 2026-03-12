export default function BillPaymentsTab({
  accounts,
  manualBillForm,
  setManualBillForm,
  onManualBill,
  scheduleBillForm,
  setScheduleBillForm,
  onScheduleBill,
  scheduledBills,
  runScheduledBill,
  billMessage,
}) {
  return (
    <section className="panel-grid">
      <article className="panel">
        <h2>Manual Bill Payment</h2>
        <form onSubmit={onManualBill}>
          <label>
            Account
            <select
              value={manualBillForm.accountId}
              onChange={(e) => setManualBillForm({ ...manualBillForm, accountId: e.target.value })}
              required
            >
              <option value="">Select</option>
              {accounts.map((a) => (
                <option key={a.id} value={a.id}>{a.id}</option>
              ))}
            </select>
          </label>
          <label>
            Payee
            <input
              value={manualBillForm.payee}
              onChange={(e) => setManualBillForm({ ...manualBillForm, payee: e.target.value })}
              required
            />
          </label>
          <label>
            Amount
            <input
              type="number"
              min="1"
              step="0.01"
              value={manualBillForm.amount}
              onChange={(e) => setManualBillForm({ ...manualBillForm, amount: e.target.value })}
              required
            />
          </label>
          <button type="submit">Pay Now</button>
        </form>
      </article>

      <article className="panel">
        <h2>Schedule Auto Payment</h2>
        <form onSubmit={onScheduleBill}>
          <label>
            Account
            <select
              value={scheduleBillForm.accountId}
              onChange={(e) => setScheduleBillForm({ ...scheduleBillForm, accountId: e.target.value })}
              required
            >
              <option value="">Select</option>
              {accounts.map((a) => (
                <option key={a.id} value={a.id}>{a.id}</option>
              ))}
            </select>
          </label>
          <label>
            Payee
            <input
              value={scheduleBillForm.payee}
              onChange={(e) => setScheduleBillForm({ ...scheduleBillForm, payee: e.target.value })}
              required
            />
          </label>
          <label>
            Amount
            <input
              type="number"
              min="1"
              step="0.01"
              value={scheduleBillForm.amount}
              onChange={(e) => setScheduleBillForm({ ...scheduleBillForm, amount: e.target.value })}
              required
            />
          </label>
          <label>
            Scheduled Date
            <input
              type="date"
              value={scheduleBillForm.scheduledDate}
              onChange={(e) => setScheduleBillForm({ ...scheduleBillForm, scheduledDate: e.target.value })}
              required
            />
          </label>
          <button type="submit">Schedule</button>
        </form>
      </article>

      <article className="panel wide">
        <h2>Scheduled Payments</h2>
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Account</th>
              <th>Payee</th>
              <th>Amount</th>
              <th>Date</th>
              <th>Status</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {scheduledBills.map((b) => (
              <tr key={b.id}>
                <td>{b.id}</td>
                <td>{b.accountId}</td>
                <td>{b.payee}</td>
                <td>FJD {b.amount.toFixed(2)}</td>
                <td>{b.scheduledDate}</td>
                <td>{b.status}</td>
                <td>
                  <button disabled={b.status === "processed"} onClick={() => runScheduledBill(b.id)}>
                    Run
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <p className="status">{billMessage}</p>
      </article>
    </section>
  );
}
