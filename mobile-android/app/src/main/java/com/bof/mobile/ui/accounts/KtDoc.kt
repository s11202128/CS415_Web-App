
/**
 * # Documentation for Accounts UI Module
 *
 * This file summarizes the main composables, functions, and ViewModel used in the accounts UI package.
 *
 * ## Main Components
 *
 * ### AccountsScreen
 * Main screen for managing user accounts. Displays the user's bank accounts, allows navigation between account overview and account creation tabs, and provides options to refresh account data. Uses a ViewModel to manage state and actions.
 * - **Params:**
 *   - viewModel: The ViewModel for account state and actions
 *   - canGoBack: Whether the back button is enabled
 *   - onBack: Callback for back navigation
 *   - onNavigateToCreateAccount: Callback to navigate to account creation
 *
 * ### OverviewTab
 * Displays an overview of the user's accounts, including account details and recent transactions. Allows paging through accounts.
 * - **Params:**
 *   - uiState: The current UI state
 *   - accounts: List of user accounts
 *   - onLoadPreviousPage: Callback to load previous page
 *   - onLoadNextPage: Callback to load next page
 *
 * ### CreateTab
 * Displays the account creation tab and a list of accounts. Allows users to open the account creation form and select accounts for details.
 * - **Params:**
 *   - uiState: The current UI state
 *   - onOpenCreateAccount: Callback to open the account creation form
 *   - onSelectAccount: Callback when an account is selected
 *   - onLoadPreviousPage: Callback to load previous page
 *   - onLoadNextPage: Callback to load next page
 *
 * ### AccountInlineDetails
 * Shows inline details for a selected account, including recent transactions and paging controls.
 * - **Params:**
 *   - uiState: The current UI state
 *   - onLoadPreviousPage: Callback to load previous page
 *   - onLoadNextPage: Callback to load next page
 *
 * ### AccountsMessageBanner
 * Displays a message banner for account-related messages, such as errors or empty states.
 * - **Params:**
 *   - text: The message text
 *   - isError: Whether the message is an error
 *   - onDismiss: Optional callback for dismissing the banner
 *   - actionLabel: Optional label for the action button
 *
 * ### Utility Functions
 * - **maskAccountNumber:** Masks an account number, showing only the last 4 digits.
 * - **formatAccountUpdatedTime:** Formats the account updated time from a raw string.
 * - **formatBalanceUpdatedTime:** Formats the balance updated time from an epoch value.
 * - **formatMoney:** Formats a double value as a money string.
 *
 * ## ViewModel: AccountsViewModel
 * Manages the state and business logic for the accounts UI. Handles loading accounts, selecting accounts, filtering, paging, and error handling.
 * - **AccountsUiState:** Data class representing the UI state for accounts, including loading flags, account lists, selected account, transactions, paging, filters, and error messages.
 * - **Key Methods:**
 *   - loadAccounts: Loads the list of accounts from the repository
 *   - selectAccount: Selects an account and loads its details and transactions
 *   - setTypeFilter: Sets a filter for transaction types
 *   - loadAccountDetails: Loads details for a selected account
 *   - loadTransactions: Loads transactions for a selected account
 *   - loadNextPage/loadPreviousPage: Handles paging for transactions
 *   - clearError: Clears error messages
 *
 * ---
 * This documentation is auto-generated from the codebase for quick reference.
 */
