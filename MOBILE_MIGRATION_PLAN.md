# Mobile Migration Plan (Android Native)

## Goal
Migrate the existing web app UI to Android native while reusing:
- Existing Node/Express backend
- Existing MySQL database
- Existing business rules (OTP, transfers, bills, loan workflows)

No mobile local database is introduced. Mobile accesses data only through secure REST APIs.

## Target Architecture

### 3-Tier Mapping
1. Presentation Layer:
- Android Compose screens and state rendering

2. Business Logic Layer:
- Backend services/controllers in server/src/services and server/src/controllers
- Android ViewModel orchestration for UI use-cases

3. Data Access Layer:
- Backend repositories/models (Sequelize/MySQL)
- Android Retrofit API client (remote API only)

### MVC/MVVM Mapping
- Model:
  - Backend models (server/src/models)
  - Android domain/data models
- View:
  - Android Compose UI
- Controller (MVVM adaptation):
  - Android ViewModel

## Suggested Server Folder Structure

server/src/
  controllers/
    authController.js
  services/
    authService.js
  repositories/
    customerRepository.js
    accountRepository.js
    transactionRepository.js
  routes/
    authRoutes.js
    apiRoutes.js
  models/
  middleware/
  utils/

## Suggested Android Folder Structure

mobile-android/
  app/src/main/java/com/bof/mobile/
    data/
      remote/
        ApiService.kt
        NetworkModule.kt
      repository/
        AuthRepository.kt
        DashboardRepository.kt
    model/
      AuthModels.kt
      DashboardModels.kt
      ApiResult.kt
    viewmodel/
      AuthViewModel.kt
      DashboardViewModel.kt
    ui/
      auth/
        LoginScreen.kt
        RegisterScreen.kt
      dashboard/
        DashboardScreen.kt

## Step-by-Step Plan
1. Backend refactor foundation
- Keep behavior unchanged
- Move auth route logic to controller/service
- Continue with transfer/account/billing controllers/services

2. API contract hardening
- Standardize errors and status codes
- Add pagination/filter support for transaction APIs
- Add aggregate endpoint for account details

3. Android app bootstrap
- Set up Kotlin + Compose + Retrofit + Coroutines
- Implement login/register flows first

4. Feature migration
- Dashboard
- Accounts + transactions
- Transfer + OTP
- Bill payments
- Statements and loans

5. Validation and rollout
- API contract tests
- Mobile integration tests
- staged release

## Commit Strategy
1. refactor(server): extract auth controller and service
2. feat(api): secure and paginate transaction endpoint
3. feat(api): add account details aggregate endpoint
4. feat(api): add recipient search and biller directory endpoints
5. feat(android): scaffold native app architecture (mvvm)
6. feat(android): implement auth ui + api integration
7. feat(android): implement dashboard core flow
8. docs(api): add method-level documentation

## Current Table Coverage Checklist

Legend:
- Complete: table has backend endpoints and Android customer/admin UI workflow.
- Partial: table is wired in backend and/or API but not yet exposed with full dedicated mobile workflow.

1. Customer: Complete
- Backend: customer profile/list/create/update and dashboard access paths are present.
- Mobile customer UI: dashboard/profile flows are implemented.
- Mobile admin UI: customer search, status updates, ID verification, and registration status actions are implemented.

2. Account: Complete
- Backend: list/create/update/freeze and account-linked operations are present.
- Mobile customer UI: accounts and account-related operations are implemented.
- Mobile admin UI: account creation and account status management are implemented.

3. Transaction: Complete
- Backend: transaction listing, transfer history, and reversal/admin transaction operations are present.
- Mobile customer UI: transfers and transaction views are implemented.
- Mobile admin UI: transaction monitoring and reversal controls are implemented.

4. Bill: Complete
- Backend: manual bill payment, scheduling, history, and scheduled execution are present.
- Mobile customer UI: bill payment + scheduling + history workflows are implemented.
- Mobile admin UI: covered through monitoring/reporting context.

5. StatementRequest: Complete
- Backend: statement request create/list/update and statement retrieval paths are present.
- Mobile customer UI: request creation and approved statement retrieval are implemented.
- Mobile admin UI: request approval/rejection tab is implemented.

6. Loan: Complete
- Backend: loan products, applications, status updates, and reporting aggregates are present.
- Mobile customer UI: product browsing and loan application flow are implemented.
- Mobile admin UI: loan status moderation is implemented.

7. Investment: Complete
- Backend: customer and admin list/create + admin status update endpoints are present.
- Mobile customer UI: create/list investment workflow is implemented.
- Mobile admin UI: investments tab with create/filter/status update is implemented.

8. Admin: Complete
- Backend: admin auth and admin-protected API routes are present.
- Mobile admin UI: dedicated dashboard with tabs for operations/monitoring/compliance/reports/statements/investments is implemented.

9. OtpVerification: Complete
- Backend: OTP records are used for transfer verification and password reset workflows.
- Mobile customer UI: transfer verification/password-reset OTP interactions are implemented.
- Mobile admin UI: OTP attempt monitoring is implemented.

10. LoginLog: Complete
- Backend: login log capture/list endpoints are present.
- Mobile admin UI: login log monitoring tab is implemented.

11. NotificationLog: Complete
- Backend: notification log capture/list endpoints are present.
- Mobile customer UI: notification history is implemented.
- Mobile admin UI: notification log monitoring tab is implemented.

12. Registration: Partial
- Backend: registration table writes and registration lifecycle support exist.
- Mobile customer UI: user registration flow is implemented.
- Gap: no dedicated admin registration queue UI based on the Registration table itself; admin actions currently operate via customer registrationStatus fields.

## Styling Note

The repository does not contain a web frontend with CSS sources. UI styling is therefore implemented in Android Compose (theme + composable layout/presentation), which is the platform-equivalent of applying professional CSS for this codebase.
