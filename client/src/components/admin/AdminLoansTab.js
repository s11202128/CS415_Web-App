export default function AdminLoansTab({ loanApplications, onAdminUpdateLoanStatus }) {
  return (
    <article className="panel wide">
      <h3>Loan Application Management</h3>
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Customer</th>
            <th>Amount</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {loanApplications.map((loan) => (
            <tr key={loan.id}>
              <td>{loan.id}</td>
              <td>{loan.customerId}</td>
              <td>FJD {Number(loan.requestedAmount || 0).toFixed(2)}</td>
              <td>{loan.status}</td>
              <td>
                <div className="inline-controls">
                  <button type="button" onClick={() => onAdminUpdateLoanStatus(loan.id, "approved")}>Approve</button>
                  <button type="button" onClick={() => onAdminUpdateLoanStatus(loan.id, "rejected")}>Reject</button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </article>
  );
}
