import { useState } from "react";
import AdminOverviewTab from "./admin/AdminOverviewTab";
import AdminCustomersTab from "./admin/AdminCustomersTab";
import AdminAccountsTab from "./admin/AdminAccountsTab";
import AdminLoansTab from "./admin/AdminLoansTab";
import AdminMonitoringTab from "./admin/AdminMonitoringTab";
import AdminReportsTab from "./admin/AdminReportsTab";

const ADMIN_SECTIONS = ["Overview", "Customers", "Accounts", "Loans", "Monitoring", "Reports"];

export default function AdminPage({
  customers,
  accounts,
  transactions,
  scheduledBills,
  loanApplications,
  summaries,
  selectedAccountForTx,
  setSelectedAccountForTx,
  adminAccountForm,
  setAdminAccountForm,
  onCreateAdminAccount,
  adminAccountMessage,
  adminMessage,
  onAdminUpdateCustomer,
  onAdminUpdateAccount,
  onAdminFreezeAccount,
  onAdminUpdateLoanStatus,
  adminTransferLimit,
  setAdminTransferLimit,
  onAdminUpdateTransferLimit,
  adminNotificationLogs,
  adminReport,
  adminLastUpdated,
}) {
  const [activeSection, setActiveSection] = useState("Overview");

  return (
    <div className="admin-grid">
      <section className="panel-grid">
        <article className="panel wide">
          <h2>Admin Dashboard</h2>
          <p className="hint">Professional admin console — live updates every 10 seconds.</p>
        </article>
        <article className="panel wide">
          <div className="admin-tabs" role="tablist" aria-label="Admin sections">
            {ADMIN_SECTIONS.map((section) => (
              <button
                key={section}
                type="button"
                className={activeSection === section ? "admin-tab active" : "admin-tab"}
                onClick={() => setActiveSection(section)}
              >
                {section}
              </button>
            ))}
          </div>
        </article>
      </section>

      {activeSection === "Overview" && (
        <AdminOverviewTab
          customers={customers}
          accounts={accounts}
          adminReport={adminReport}
          adminLastUpdated={adminLastUpdated}
          adminMessage={adminMessage}
        />
      )}
      {activeSection === "Customers" && (
        <AdminCustomersTab customers={customers} onAdminUpdateCustomer={onAdminUpdateCustomer} />
      )}
      {activeSection === "Accounts" && (
        <AdminAccountsTab
          customers={customers}
          accounts={accounts}
          adminAccountForm={adminAccountForm}
          setAdminAccountForm={setAdminAccountForm}
          onCreateAdminAccount={onCreateAdminAccount}
          adminAccountMessage={adminAccountMessage}
          onAdminUpdateAccount={onAdminUpdateAccount}
          onAdminFreezeAccount={onAdminFreezeAccount}
        />
      )}
      {activeSection === "Loans" && (
        <AdminLoansTab loanApplications={loanApplications} onAdminUpdateLoanStatus={onAdminUpdateLoanStatus} />
      )}
      {activeSection === "Monitoring" && (
        <AdminMonitoringTab
          accounts={accounts}
          transactions={transactions}
          selectedAccountForTx={selectedAccountForTx}
          setSelectedAccountForTx={setSelectedAccountForTx}
          adminTransferLimit={adminTransferLimit}
          setAdminTransferLimit={setAdminTransferLimit}
          onAdminUpdateTransferLimit={onAdminUpdateTransferLimit}
          adminNotificationLogs={adminNotificationLogs}
        />
      )}
      {activeSection === "Reports" && (
        <AdminReportsTab
          adminReport={adminReport}
          scheduledBills={scheduledBills}
          summaries={summaries}
        />
      )}
    </div>
  );
}

