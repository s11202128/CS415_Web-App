import { useState } from "react";
import AdminOverviewTab from "./admin/AdminOverviewTab";
import AdminCustomersTab from "./admin/AdminCustomersTab";
import AdminAccountsTab from "./admin/AdminAccountsTab";
import AdminLoansTab from "./admin/AdminLoansTab";
import AdminMonitoringTab from "./admin/AdminMonitoringTab";
import AdminReportsTab from "./admin/AdminReportsTab";
import AdminStatementsTab from "./admin/AdminStatementsTab";

const ADMIN_SECTIONS = ["Overview", "Customers", "Accounts", "Statement Requests", "Loans", "Monitoring", "Reports"];

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
  onAdminReverseTransaction,
  adminLoginLogs,
  adminNotificationLogs,
  adminStatementRequests,
  onAdminUpdateStatementRequest,
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
      </section>

      <div className="workspace-layout">
        <aside className="left-tabs">
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
        </aside>

        <section className="tab-content">
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
              accounts={accounts}
              adminAccountForm={adminAccountForm}
              setAdminAccountForm={setAdminAccountForm}
              onCreateAdminAccount={onCreateAdminAccount}
              adminAccountMessage={adminAccountMessage}
              onAdminUpdateAccount={onAdminUpdateAccount}
              onAdminFreezeAccount={onAdminFreezeAccount}
            />
          )}
          {activeSection === "Statement Requests" && (
            <AdminStatementsTab
              statementRequests={adminStatementRequests}
              onAdminUpdateStatementRequest={onAdminUpdateStatementRequest}
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
              onAdminReverseTransaction={onAdminReverseTransaction}
              adminLoginLogs={adminLoginLogs}
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
        </section>
      </div>
    </div>
  );
}

