export default function AdminAccountsTab({
  customers,
  accounts,
  adminAccountForm,
  setAdminAccountForm,
  onCreateAdminAccount,
  adminAccountMessage,
  onAdminUpdateAccount,
  onAdminFreezeAccount,
}) {
  return (
    <section className="panel-grid">
      <article className="panel wide">
        <h3>Open New Account</h3>
        <form className="admin-form" onSubmit={onCreateAdminAccount}>
          <label>
            Customer
            <select
              value={adminAccountForm.customerId}
              onChange={(e) => setAdminAccountForm({ ...adminAccountForm, customerId: e.target.value })}
              required
            >
              <option value="">Select customer</option>
              {customers.map((c) => (
                <option key={c.id} value={c.id}>{c.fullName} ({c.id})</option>
              ))}
            </select>
          </label>
          <label>
            Account Type
            <select
              value={adminAccountForm.type}
              onChange={(e) => setAdminAccountForm({ ...adminAccountForm, type: e.target.value })}
              required
            >
              <option value="Simple Access">Simple Access</option>
              <option value="Savings">Savings</option>
            </select>
          </label>
          <label>
            Opening Balance
            <input
              type="number"
              min="0"
              step="0.01"
              value={adminAccountForm.openingBalance}
              onChange={(e) => setAdminAccountForm({ ...adminAccountForm, openingBalance: e.target.value })}
            />
          </label>
          <label>
            Account Number (optional — 12 digits)
            <input
              value={adminAccountForm.accountNumber}
              onChange={(e) => setAdminAccountForm({ ...adminAccountForm, accountNumber: e.target.value })}
              placeholder="Leave blank to auto-generate"
            />
          </label>
          <button type="submit">Create Account</button>
        </form>
        <p className="status">{adminAccountMessage}</p>
      </article>

      <article className="panel wide">
        <h3>Account Controls</h3>
        <table>
          <thead>
            <tr>
              <th>Account ID</th>
              <th>Customer ID</th>
              <th>Account Number</th>
              <th>Type</th>
              <th>Balance</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {accounts.map((a) => (
              <tr key={a.id}>
                <td>{a.id}</td>
                <td>{a.customerId}</td>
                <td>{a.accountNumber || "-"}</td>
                <td>{a.type}</td>
                <td>FJD {Number(a.balance).toFixed(2)}</td>
                <td>{a.status || "active"}</td>
                <td>
                  <div className="inline-controls">
                    <button type="button" onClick={() => onAdminUpdateAccount(a.id, { type: "Simple Access" })}>Set Simple</button>
                    <button type="button" onClick={() => onAdminUpdateAccount(a.id, { type: "Savings" })}>Set Savings</button>
                    <button type="button" onClick={() => onAdminUpdateAccount(a.id, { status: "suspended" })}>Suspend</button>
                    <button type="button" onClick={() => onAdminUpdateAccount(a.id, { status: "closed" })}>Close</button>
                    <button type="button" onClick={() => onAdminFreezeAccount(a.id)}>Freeze</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </article>
    </section>
  );
}
