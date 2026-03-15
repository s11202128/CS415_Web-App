export default function AdminStatementsTab({ statementRequests, onAdminUpdateStatementRequest }) {
  return (
    <section className="panel-grid">
      <article className="panel wide">
        <h3>Statement Request Approval Queue</h3>
        <p className="hint">Approve requests before customers can view or download statements.</p>
        <table>
          <thead>
            <tr>
              <th>Requested</th>
              <th>Customer</th>
              <th>Account Holder</th>
              <th>Account Number</th>
              <th>From</th>
              <th>To</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {statementRequests.map((request) => (
              <tr key={request.id}>
                <td>{new Date(request.createdAt).toLocaleString()}</td>
                <td>{request.fullName}</td>
                <td>{request.accountHolder}</td>
                <td>{request.accountNumber}</td>
                <td>{request.fromDate}</td>
                <td>{request.toDate}</td>
                <td>{request.status}</td>
                <td>
                  <div className="inline-controls">
                    <button type="button" onClick={() => onAdminUpdateStatementRequest(request.id, "approved")}>Approve</button>
                    <button type="button" onClick={() => onAdminUpdateStatementRequest(request.id, "rejected")}>Reject</button>
                    <button type="button" onClick={() => onAdminUpdateStatementRequest(request.id, "pending")}>Reset</button>
                  </div>
                </td>
              </tr>
            ))}
            {statementRequests.length === 0 && (
              <tr>
                <td colSpan="8" className="no-data">No statement requests yet.</td>
              </tr>
            )}
          </tbody>
        </table>
      </article>
    </section>
  );
}
