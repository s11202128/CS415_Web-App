export default function AdminAccountsTab({
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
            Customer Name
            <input
              value={adminAccountForm.customerName}
              onChange={(e) => setAdminAccountForm({ ...adminAccountForm, customerName: e.target.value })}
              placeholder="Type full name (existing or new customer)"
              required
            />
          </label>
          <label>
            Account Type
            <select
              value={adminAccountForm.type}
              onChange={(e) => setAdminAccountForm({ ...adminAccountForm, type: e.target.value })}
              required
            >
              <option value="Simple Access">Cheque</option>
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
    </section>
  );
}
