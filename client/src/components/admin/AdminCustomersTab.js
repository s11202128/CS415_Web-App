import { useState } from "react";

export default function AdminCustomersTab({ customers, onAdminUpdateCustomer }) {
  const [tinInputs, setTinInputs] = useState({});

  return (
    <article className="panel wide">
      <h3>Customer Management</h3>
      <table>
        <thead>
          <tr>
            <th>Customer ID</th>
            <th>Full Name</th>
            <th>Mobile</th>
            <th>Residency</th>
            <th>TIN</th>
            <th>Verification</th>
            <th>Registration</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {customers.map((c) => (
            <tr key={c.id}>
              <td>{c.id}</td>
              <td>{c.fullName}</td>
              <td>{c.mobile}</td>
              <td>{c.residencyStatus || "resident"}</td>
              <td>{c.tin || "-"}</td>
              <td>{c.identityVerified ? "Verified" : "Pending"}</td>
              <td>{c.registrationStatus || "approved"}</td>
              <td>
                <div className="inline-controls">
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
                  <button type="button" onClick={() => onAdminUpdateCustomer(c.id, { registrationStatus: "approved" })}>Approve</button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </article>
  );
}
