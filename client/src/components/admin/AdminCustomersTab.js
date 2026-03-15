import { useState } from "react";

export default function AdminCustomersTab({ customers, onAdminUpdateCustomer }) {
  const [tinInputs, setTinInputs] = useState({});
  const [searchTerm, setSearchTerm] = useState("");

  const filteredCustomers = customers.filter((customer) => {
    const query = searchTerm.trim().toLowerCase();
    if (!query) {
      return true;
    }
    return [customer.fullName, customer.email, customer.mobile, customer.nationalId]
      .some((value) => String(value || "").toLowerCase().includes(query));
  });

  return (
    <article className="panel wide admin-customers-panel">
      <h3>Customer Management</h3>
      <div className="inline-controls admin-customers-toolbar">
        <label>
          Search Customers
          <input
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search by name, email, phone or ID"
          />
        </label>
      </div>
      <div className="table-wrapper admin-customers-table-wrap">
        <table className="admin-customers-table">
          <thead>
            <tr>
              <th>Customer ID</th>
              <th>Full Name</th>
              <th>Email</th>
              <th>Mobile</th>
              <th>National ID</th>
              <th>Residency</th>
              <th>TIN</th>
              <th>Verification</th>
              <th>Registration</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredCustomers.map((c) => (
              <tr key={c.id}>
                <td className="monospace">{c.id}</td>
                <td>{c.fullName}</td>
                <td>{c.email}</td>
                <td className="monospace">{c.mobile}</td>
                <td className="monospace">{c.nationalId || "-"}</td>
                <td>{c.residencyStatus || "resident"}</td>
                <td className="monospace">{c.tin || "-"}</td>
                <td>{c.identityVerified ? "Verified" : c.emailVerified ? "Email Verified" : "Pending"}</td>
                <td>{c.registrationStatus || "approved"}</td>
                <td>{c.status || "active"}</td>
                <td>
                  <div className="inline-controls admin-customer-actions">
                    <input
                      placeholder="TIN"
                      value={tinInputs[c.id] ?? c.tin ?? ""}
                      onChange={(e) => setTinInputs({ ...tinInputs, [c.id]: e.target.value })}
                    />
                    <button type="button" onClick={() => onAdminUpdateCustomer(c.id, { tin: tinInputs[c.id] ?? c.tin ?? "" })}>
                      Update TIN
                    </button>
                    <button type="button" onClick={() => onAdminUpdateCustomer(c.id, { residencyStatus: "resident" })}>Resident</button>
                    <button type="button" onClick={() => onAdminUpdateCustomer(c.id, { residencyStatus: "non-resident" })}>Non-Resident</button>
                    <button type="button" onClick={() => onAdminUpdateCustomer(c.id, { identityVerified: true })}>Verify ID</button>
                    <button type="button" onClick={() => onAdminUpdateCustomer(c.id, { emailVerified: true })}>Verify Email</button>
                    <button type="button" onClick={() => onAdminUpdateCustomer(c.id, { registrationStatus: "approved" })}>Approve</button>
                    <button type="button" onClick={() => onAdminUpdateCustomer(c.id, { status: "disabled" })}>Disable</button>
                    <button type="button" onClick={() => onAdminUpdateCustomer(c.id, { status: "locked" })}>Lock</button>
                    <button type="button" onClick={() => onAdminUpdateCustomer(c.id, { status: "active", failedLoginAttempts: 0, lockedUntil: null })}>Unlock</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </article>
  );
}
