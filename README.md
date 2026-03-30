# CS415-A1 - Bank of Fiji Online Banking Prototype

This workspace was split into two standalone repositories (history preserved):

- Mobile Android app: see `C:\Users\LENOVO\Desktop\cs415-mobile` (branch `main`)
- Backend/server: see `C:\Users\LENOVO\Desktop\cs415-web` (branch `main`)

Keep this repo as an archive; do active work in the new repos and push them to GitHub remotes you create (for example `cs415-mobile` and `cs415-web`).

## Stack
- Mobile App (Presentation Layer): Kotlin + Jetpack Compose (`mobile-android`)
- Backend API (Business + Data Access Layers): Node.js + Express + Sequelize (`server`)
- Database: MySQL (remote/shared database accessed only via backend APIs)

## Functional Coverage
- Account holders, accounts, and transactions
- Money transfer with OTP verification for high-value transactions
- Manual and scheduled bill payments
- Statement viewing and CSV download
- SMS-like notifications for money received and bill payment processed
- Investments module
- Savings interest rate configuration (variable, Reserve Bank minimum)
- Annual interest summaries with withholding tax logic and FRCS submission status
- Loan products advertisement and interactive loan application form
- Prioritized user stories, conflicting requirements, and trade-offs exposed via API/mobile UI

## Backend APIs
Base URL: `http://localhost:4000/api`

- `GET /health`
- `GET /requirements`
- `GET /customers`, `POST /customers`
- `GET /accounts`, `POST /accounts`
- `GET /transactions?accountId=ACC-001`
- `POST /transfers/initiate`
- `POST /transfers/verify`
- `POST /bills/manual`
- `POST /bills/scheduled`
- `GET /bills/scheduled`
- `POST /bills/scheduled/:id/run`
- `GET /investments`, `POST /investments`
- `GET /statements/:accountId`
- `GET /statements/:accountId/download`
- `GET /notifications/:customerId`
- `GET /config/interest-rate`, `PUT /config/interest-rate`
- `POST /year-end/interest-summaries`, `GET /year-end/interest-summaries`
- `GET /loan-products`
- `POST /loan-applications`, `GET /loan-applications`

## Run Instructions (Windows)
### 1. Install Node.js
- Download Node.js LTS from: https://nodejs.org/
- During install, ensure "Add to PATH" is enabled.
- Open a **new** terminal and verify:
  - `node -v`
  - `npm -v`

### 2. Install backend dependencies
From workspace root (`c:\Users\LENOVO\Desktop\CS415-A1`):

```powershell
Set-Location .\server
npm install
```

### 3. Start backend
```powershell
Set-Location .\server
npm run dev
```
Backend runs on `http://localhost:4000`.

### 4. Run native Android app
- Open Android Studio.
- Open project folder: `mobile-android`.
- Use emulator/device and run the app module.
- Android emulator should call backend via `http://10.0.2.2:4000/api/`.

## High Priority Flows
- Login and registration on Android auth screens.
- Dashboard summary and recent transactions on Android dashboard screen.
- Transfer and OTP verification via backend transfer endpoints.
- Bills, statements, and loan workflows through backend APIs.

## Notes
- Web React frontend has been removed from this repository by design.
- Architecture target is mobile presentation + backend service/data layers.
- Interest computation is simplified annual prototype logic.
