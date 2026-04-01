// Onboarding/Profile Demo Interactions
document.addEventListener('DOMContentLoaded', () => {
  const fab = document.querySelector('.onboarding-fab');
  if (fab) {
    fab.addEventListener('click', () => {
      fab.textContent = 'Loading...';
      fab.disabled = true;
      setTimeout(() => {
        fab.textContent = 'Get Started';
        fab.disabled = false;
        alert('Welcome to your new mobile experience!');
      }, 1200);
    });
  }
  const settings = document.querySelector('.onboarding-settings');
  if (settings) {
    settings.addEventListener('click', () => {
      alert('Settings coming soon!');
    });
  }
});
// --- State ---
const state = {
  currentScreen: localStorage.getItem('token') ? 'home' : 'login',
  sidebarOpen: false,
  token: localStorage.getItem('token') || '',
  user: JSON.parse(localStorage.getItem('user') || 'null')
};

// --- Screens ---
const screens = {
  login: renderLoginScreen,
  home: renderHomeScreen,
  accounts: renderAccountsScreen,
  bill: renderBillScreen,
  statements: renderStatementsScreen,
  notifications: renderNotificationsScreen,
  loan: renderLoanScreen,
  investment: renderInvestmentScreen,
  interest: renderInterestScreen,
  password: renderPasswordScreen,
  profile: renderProfileScreen
};

// --- Main Render ---
function renderApp() {
  const app = document.getElementById('app');
  if (state.currentScreen === 'login') {
    app.innerHTML = `
      <div class="main-content">${renderLoginScreen()}</div>
    `;
    return;
  }
  app.innerHTML = `
    ${renderTopNav()}
    <div class="main-content">${screens[state.currentScreen]()}</div>
    ${renderBottomNav()}
    ${renderSidebar()}
    <div class="sidebar-overlay${state.sidebarOpen ? ' open' : ''}" onclick="closeSidebar()"></div>
  `;
}

// --- Top Navigation ---
function renderTopNav() {
  return `
    <div class="top-nav">
      <div class="left">
        <button onclick="openSidebar()" aria-label="Open menu">☰</button>
      </div>
      <div class="center">Welcome, ${state.user?.fullName || 'User'} 👋</div>
      <div class="right">
        <div class="profile" onclick="switchScreen('profile')">👤</div>
        <button onclick="signOut()" title="Logout">Logout</button>
      </div>
    </div>
  `;
}

// --- Bottom Navigation ---
function renderBottomNav() {
  return `
    <div class="bottom-nav">
      <button class="nav-btn${state.currentScreen==='home' ? ' active' : ''}" onclick="switchScreen('home')">
        <span>🏠</span><span>Home</span>
      </button>
      <button class="nav-btn${state.currentScreen==='accounts' ? ' active' : ''}" onclick="switchScreen('accounts')">
        <span>💳</span><span>Accounts</span>
      </button>
    </div>
  `;
}

// --- Login Screen ---
function renderLoginScreen() {
  return `
    <div class="card login-card">
      <h2>Sign In</h2>
      <form id="loginForm" class="login-form">
        <label>Email or Mobile
          <input type="text" id="loginEmail" required autocomplete="username" />
        </label>
        <label>Password
          <input type="password" id="loginPassword" required autocomplete="current-password" />
        </label>
        <button type="submit">Login</button>
        <div id="loginError" class="login-error"></div>
      </form>
    </div>
  `;
}

// --- Login Logic ---
document.addEventListener('submit', async (e) => {
  if (e.target && e.target.id === 'loginForm') {
    e.preventDefault();
    const email = document.getElementById('loginEmail').value.trim();
    const password = document.getElementById('loginPassword').value;
    const errorDiv = document.getElementById('loginError');
    errorDiv.textContent = '';
    try {
      const res = await fetch('http://localhost:4000/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || 'Login failed');
      state.token = data.token;
      state.user = data;
      localStorage.setItem('token', data.token);
      localStorage.setItem('user', JSON.stringify(data));
      state.currentScreen = 'home';
      renderApp();
    } catch (err) {
      errorDiv.textContent = err.message;
    }
  }
});

// --- Sidebar ---
function renderSidebar() {
  return `
    <div class="sidebar${state.sidebarOpen ? ' open' : ''}">
      <button class="close-btn" onclick="closeSidebar()">×</button>
      <button class="menu-item" onclick="switchScreen('transfers')">Transfers</button>
      <button class="menu-item" onclick="switchScreen('bill')">Bill Payment</button>
      <button class="menu-item" onclick="switchScreen('statements')">Statements</button>
      <button class="menu-item" onclick="switchScreen('notifications')">Notifications</button>
      <button class="menu-item" onclick="switchScreen('loan')">Loan</button>
      <button class="menu-item" onclick="switchScreen('investment')">Investment</button>
      <button class="menu-item" onclick="switchScreen('interest')">Interest Summaries</button>
      <button class="menu-item" onclick="switchScreen('password')">Password Reset</button>
    </div>
  `;
}

function openSidebar() {
  state.sidebarOpen = true;
  renderApp();
}
function closeSidebar() {
  state.sidebarOpen = false;
  renderApp();
}

// --- Screen Switch ---
function switchScreen(screen) {
  state.currentScreen = screen;
  state.sidebarOpen = false;
  renderApp();
}
window.switchScreen = switchScreen;
window.openSidebar = openSidebar;
window.closeSidebar = closeSidebar;
window.signOut = function() { alert('Sign out clicked!'); };

// --- Home Screen ---
function renderHomeScreen() {
  if (!state.accounts) {
    fetchAccounts();
    return `<div class="card">Loading accounts...</div>`;
  }
  const mainAccount = state.accounts[0];
  return `
    <div class="balance-card">
      <div class="balance">${mainAccount ? formatCurrency(mainAccount.balance) : '--'}</div>
      <div class="card-number">${mainAccount ? '•••• ' + String(mainAccount.accountNumber).slice(-4) : ''}</div>
    </div>
    <div class="quick-actions">
      <button class="quick-action-btn" onclick="switchScreen('transfers')">Transfer</button>
      <button class="quick-action-btn" onclick="switchScreen('bill')">Pay Bills</button>
      <button class="quick-action-btn" onclick="switchScreen('statements')">Statements</button>
      <button class="quick-action-btn" onclick="switchScreen('profile')">Profile</button>
    </div>
    <div class="stats-row" style="display: flex; gap: 16px; margin-bottom: 18px;">
      <div class="card" style="flex:1; text-align:center; padding:12px 0;">
        <div style="font-size:1.1rem; font-weight:600; color:#6366f1;">Total Balance</div>
        <div style="font-size:1.3rem; font-weight:700;">${mainAccount ? formatCurrency(mainAccount.balance) : '--'}</div>
      </div>
      <div class="card" style="flex:1; text-align:center; padding:12px 0;">
        <div style="font-size:1.1rem; font-weight:600; color:#6366f1;">Active</div>
        <div style="font-size:1.3rem; font-weight:700;">✔️</div>
      </div>
    </div>
    <div class="card" style="margin-bottom:0;">
      <h2 style="margin:0 0 10px 0; font-size:1.1rem; color:#6366f1;">Recent Transactions</h2>
      <div style="font-size:0.98rem; color:#374151;">See your latest activity here.</div>
    </div>
  `;
}

// --- Placeholder Screens ---
function renderAccountsScreen() {
  if (!state.accounts) {
    fetchAccounts();
    return `<div class="card">Loading accounts...</div>`;
  }
  return `
    <div class="card">
      <h2>My Accounts</h2>
      <div class="account-list">
        ${state.accounts.map(acc => `
          <div class="account-item">
            <div class="account-type">${acc.type || 'Account'}</div>
            <div class="account-balance">${formatCurrency(acc.balance)}</div>
            <div class="account-number">•••• ${String(acc.accountNumber).slice(-4)}</div>
          </div>
        `).join('')}
      </div>
    </div>
  `;
}

// --- Fetch Accounts ---
async function fetchAccounts() {
  if (!state.token || !state.user?.customerId) return;
  try {
    const res = await fetch(`http://localhost:4000/api/accounts`, {
      headers: { 'Authorization': 'Bearer ' + state.token }
    });
    if (!res.ok) throw new Error('Failed to load accounts');
    const accounts = await res.json();
    state.accounts = accounts;
    renderApp();
  } catch (err) {
    state.accounts = [];
    renderApp();
  }
}

function formatCurrency(amount) {
  return amount == null ? '--' : `$${Number(amount).toLocaleString(undefined, {minimumFractionDigits:2, maximumFractionDigits:2})}`;
}
function renderTransfersScreen() {
  return `
    <div class="card">
      <h2>Transfer Money</h2>
      <form class="transfer-form" onsubmit="event.preventDefault(); alert('Transfer submitted!');">
        <label>From Account
          <select><option>Savings (•••• 1234)</option><option>Checking (•••• 5678)</option></select>
        </label>
        <label>To Account/Recipient
          <input type="text" placeholder="Recipient name or account" required />
        </label>
        <label>Amount
          <input type="number" placeholder="$0.00" required />
        </label>
        <button type="submit">Send</button>
      </form>
    </div>
  `;
}
function renderBillScreen() {
  if (!state.billers) {
    fetchBillers();
    return `<div class="card">Loading billers...</div>`;
  }
  return `
    <div class="card">
      <h2>Bill Payment</h2>
      <form id="billForm" class="bill-form">
        <label>Biller
          <select id="billBiller" required>
            ${state.billers.map(b => `<option value="${b.id}">${b.name}</option>`).join('')}
          </select>
        </label>
        <label>Account/Reference
          <input type="text" id="billReference" placeholder="Reference number" required />
        </label>
        <label>Amount
          <input type="number" id="billAmount" placeholder="$0.00" required />
        </label>
        <button type="submit">Pay Bill</button>
        <div id="billError" class="login-error"></div>
      </form>
    </div>
  `;
}
function renderStatementsScreen() {
  if (!state.accounts) {
    fetchAccounts();
    return `<div class="card">Loading accounts...</div>`;
  }
  if (!state.transactions) {
    fetchTransactions();
    return `<div class="card">Loading transactions...</div>`;
  }
  return `
    <div class="card">
      <h2>Recent Transactions</h2>
      <table class="transactions-table">
        <thead><tr><th>Date</th><th>Description</th><th>Amount</th></tr></thead>
        <tbody>
          ${state.transactions.map(tx => `
            <tr>
              <td>${tx.createdAt ? tx.createdAt.slice(0,10) : ''}</td>
              <td>${tx.description || tx.kind || ''}</td>
              <td>${formatCurrency(tx.amount)}</td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    </div>
  `;
}
function renderNotificationsScreen() {
  if (!state.notifications) {
    fetchNotifications();
    return `<div class="card">Loading notifications...</div>`;
  }
  return `
    <div class="card">
      <h2>Notifications</h2>
      <ul class="notifications-list">
        ${state.notifications.map(n => `<li>${n.message || n.type || ''}</li>`).join('')}
      </ul>
    </div>
  `;
}
function renderLoanScreen() {
  if (!state.loans) {
    fetchLoans();
    return `<div class="card">Loading loans...</div>`;
  }
  if (!state.loans.length) {
    return `<div class="card"><h2>Loan Summary</h2><div class="loan-info">No loans found.</div></div>`;
  }
  return `
    <div class="card">
      <h2>Loan Summary</h2>
      <div class="loan-info">
        ${state.loans.map(loan => `
          <div>Type: <b>${loan.type || loan.loanType || 'Loan'}</b></div>
          <div>Outstanding: <b>${formatCurrency(loan.outstandingAmount || loan.amount)}</b></div>
          <div>Next Payment Due: <b>${loan.nextPaymentDue || '-'}</b></div>
          <div>Monthly Payment: <b>${formatCurrency(loan.monthlyPayment)}</b></div>
        `).join('<hr/>')}
      </div>
    </div>
  `;
}
function renderInvestmentScreen() {
  if (!state.investments) {
    fetchInvestments();
    return `<div class="card">Loading investments...</div>`;
  }
  if (!state.investments.length) {
    return `<div class="card"><h2>Investments</h2><div class="investment-list">No investments found.</div></div>`;
  }
  return `
    <div class="card">
      <h2>Investments</h2>
      <div class="investment-list">
        ${state.investments.map(inv => `
          <div>${inv.type || inv.investmentType || 'Investment'}: <b>${formatCurrency(inv.amount)}</b> (${inv.rate ? inv.rate + '% p.a.' : ''})</div>
        `).join('')}
      </div>
    </div>
  `;
}
function renderInterestScreen() {
  if (!state.interestSummaries) {
    fetchInterestSummaries();
    return `<div class="card">Loading interest summaries...</div>`;
  }
  if (!state.interestSummaries.length) {
    return `<div class="card"><h2>Interest Summary</h2><div class="interest-summary">No interest summaries found.</div></div>`;
  }
  return `
    <div class="card">
      <h2>Interest Summary</h2>
      <div class="interest-summary">
        ${state.interestSummaries.map(i => `
          <div>${i.accountType || i.type || 'Account'} Interest (${i.year || ''}): <b>${formatCurrency(i.amount)}</b></div>
        `).join('')}
      </div>
    </div>
  `;

// --- Fetch Interest Summaries ---
async function fetchInterestSummaries() {
  if (!state.token) return;
  try {
    const res = await fetch(`http://localhost:4000/api/year-end/interest-summaries`, {
      headers: { 'Authorization': 'Bearer ' + state.token }
    });
    if (!res.ok) throw new Error('Failed to load interest summaries');
    state.interestSummaries = await res.json();
    renderApp();
  } catch (err) {
    state.interestSummaries = [];
    renderApp();
  }
}
  state.loans = undefined;
  state.investments = undefined;
  state.interestSummaries = undefined;
}
function renderPasswordScreen() {
  return `
    <div class="card">
      <h2>Password Reset</h2>
      <form class="password-form" onsubmit="event.preventDefault(); alert('Password reset link sent!');">
        <label>Email
          <input type="email" placeholder="Enter your email" required />
        </label>
        <button type="submit">Send Reset Link</button>
      </form>
    </div>
  `;
}
function renderProfileScreen() {
  if (!state.profile) {
    fetchProfile();
    return `<div class="card">Loading profile...</div>`;
  }
  const p = state.profile;
  return `
    <div class="card">
      <h2>Profile</h2>
      <div class="profile-info">
        <div><b>Name:</b> ${p.fullName || ''}</div>
        <div><b>Email:</b> ${p.email || ''}</div>
        <div><b>Phone:</b> ${p.mobile || ''}</div>
        <div><b>Student ID:</b> ${p.nationalId || ''}</div>
      </div>
    </div>
  `;
}

// --- Initial Render ---
renderApp();
