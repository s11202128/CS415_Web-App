# Mobile API Additions

## GET /api/transactions
Returns transactions for an account with optional filters and pagination.

### Query Parameters
- accountId (number, optional): Account id
- accountNumber (string, optional): 12-digit account number
- type (string, optional): debit or credit
- fromDate (ISO string, optional)
- toDate (ISO string, optional)
- minAmount (number, optional)
- maxAmount (number, optional)
- paginated (boolean, optional)
- page (number, optional)
- pageSize (number, optional)

### Response
- Non-paginated: array of transaction rows
- Paginated: object with items, page, pageSize, total, totalPages

## GET /api/accounts/:id/details
Returns account details and recent transactions in one request.

### Query Parameters
- limit (number, optional): max rows, default 20

### Response
- account: account object
- customer: owner summary
- transactions: recent transaction array

## GET /api/recipients/search
Search destination accounts for transfer.

### Query Parameters
- q (string, required): minimum length 3

### Response
- Array of recipient objects: accountId, accountNumber, accountHolder, customerId

## GET /api/billers
Returns supported billers for bill payment UI.

### Response
- Array of biller objects: code, name

## POST /api/admin/create-account
Creates an account for an existing customer (admin only).

### Request Body
- customerId (number, optional): Existing customer id
- customerName (string, optional): Existing customer full name (case-insensitive match)
- type (string, required): `Simple Access` | `Savings` | `Current`
- openingBalance (number, optional)
- accountNumber (string, optional): 12-digit custom account number

### Validation
- Either `customerId` or `customerName` must resolve to an existing customer.
- Unknown `customerName` now returns `400` (`Customer not found for provided customerName`).

## GET /api/investments
Returns investment records.

### Query Parameters
- customerId (number, optional): Admin-only filter. Non-admin requests always return the authenticated customer's investments.

### Response
- Array of investments: id, customerId, customerName, investmentType, amount, expectedReturn, maturityDate, status, createdAt, updatedAt

## POST /api/investments
Creates a new investment record.

### Request Body
- customerId (number, optional for non-admin): defaults to authenticated customer
- investmentType (string, required)
- amount (number, required)
- expectedReturn (number, optional)
- maturityDate (ISO string/date, optional)
- status (string, optional, default `active`)

## GET /api/admin/investments
Lists investments for admin workflows.

### Query Parameters
- customerId (number, optional)
- status (string, optional)

## POST /api/admin/investments
Creates investment for a customer (admin only).

### Request Body
- customerId (number, optional)
- customerName (string, optional, must resolve to existing customer when used)
- investmentType (string, required)
- amount (number, required)
- expectedReturn (number, optional)
- maturityDate (ISO string/date, optional)
- status (string, optional)

## PATCH /api/admin/investments/:id
Updates investment fields (admin only).

### Updatable Fields
- investmentType
- amount
- expectedReturn
- maturityDate
- status
