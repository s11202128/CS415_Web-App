export default function TransfersTab({
  accounts,
  transferForm,
  setTransferForm,
  onInitiateTransfer,
  pendingTransfer,
  setPendingTransfer,
  onVerifyTransfer,
  transferMessage,
}) {
  return (
    <section className="panel-grid">
      <article className="panel">
        <h2>Initiate Transfer</h2>
        <form onSubmit={onInitiateTransfer}>
          <label>
            From Account
            <select
              value={transferForm.fromAccountId}
              onChange={(e) => setTransferForm({ ...transferForm, fromAccountId: e.target.value })}
              required
            >
              <option value="">Select</option>
              {accounts.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.accountNumber || `ID ${a.id}`}
                </option>
              ))}
            </select>
          </label>
          <label>
            To Account
            <input
              value={transferForm.toAccountNumber || ""}
              onChange={(e) => setTransferForm({ ...transferForm, toAccountNumber: e.target.value })}
              placeholder="Enter destination account number"
              required
            />
          </label>
          <label>
            Amount (FJD)
            <input
              type="number"
              min="1"
              step="0.01"
              value={transferForm.amount}
              onChange={(e) => setTransferForm({ ...transferForm, amount: e.target.value })}
              required
            />
          </label>
          <label>
            Description
            <input
              value={transferForm.description}
              onChange={(e) => setTransferForm({ ...transferForm, description: e.target.value })}
            />
          </label>
          <button type="submit">Send Transfer</button>
        </form>
      </article>

      <article className="panel">
        <h2>OTP Verification</h2>
        <div className="otp-notice">
          🔒 Transfers of <strong>FJD 1,000 or more</strong> require OTP verification before processing. The OTP is
          sent via SMS to the account holder's mobile number.
        </div>
        <form onSubmit={onVerifyTransfer}>
          <label>
            Transfer ID
            <input
              value={pendingTransfer.transferId}
              onChange={(e) => setPendingTransfer({ ...pendingTransfer, transferId: e.target.value })}
              required
            />
          </label>
          <label>
            OTP
            <input
              value={pendingTransfer.otp}
              onChange={(e) => setPendingTransfer({ ...pendingTransfer, otp: e.target.value })}
              required
            />
          </label>
          <button type="submit">Verify OTP</button>
        </form>
        <p className="status">{transferMessage}</p>
        <p className="hint">OTP is required only for high-value transactions.</p>
      </article>
    </section>
  );
}
