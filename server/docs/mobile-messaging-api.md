# Mobile Messaging Notification System

## Backend API Structure

- `POST /api/transaction/initiate`
  - Initiates a transfer transaction. If amount is above configured threshold, OTP verification is required.
- `POST /api/otp/send`
  - Sends OTP for a high-value transfer request payload.
- `POST /api/otp/verify`
  - Verifies OTP and completes pending transfer transaction.
- `POST /api/notifications/send`
  - Sends an SMS notification to a customer and stores log.
- `GET /api/notifications/history`
  - Returns authenticated user's SMS notification logs. Admin can query by `customerId`.
- `GET /api/admin/notifications/logs`
  - Admin endpoint for global SMS notification logs.
- `GET /api/admin/otp-attempts`
  - Admin endpoint to monitor OTP verification attempts.
- `PUT /api/admin/transfer-limit`
  - Admin endpoint to configure high-value transfer threshold.

## Authentication

- Messaging and OTP endpoints require `Authorization: Bearer <jwt_token>`.
- Admin endpoints require authenticated admin token.

## Database Schema

### `otp_verifications`
- `id` BIGINT PK
- `referenceCode` VARCHAR UNIQUE NOT NULL
- `customerId` BIGINT NOT NULL
- `otp` VARCHAR NOT NULL (SHA-256 hash)
- `transactionType` VARCHAR NOT NULL
- `amount` DECIMAL(12,2)
- `metadata` TEXT
- `expiresAt` DATETIME NOT NULL
- `verified` BOOLEAN DEFAULT false
- `attempts` INT DEFAULT 0
- `maxAttempts` INT DEFAULT 3
- `lastAttemptAt` DATETIME NULL
- `createdAt` DATETIME
- `updatedAt` DATETIME

### `notification_logs`
- `id` BIGINT PK
- `userId` BIGINT NOT NULL
- `phoneNumber` VARCHAR NOT NULL
- `message` TEXT NOT NULL
- `notificationType` VARCHAR NOT NULL
- `deliveryStatus` VARCHAR NOT NULL
- `providerMessageId` VARCHAR NULL
- `createdAt` DATETIME
- `updatedAt` DATETIME

## Example SMS Sending Logic

- SMS provider is configured with:
  - `SMS_PROVIDER=twilio`
  - `TWILIO_ACCOUNT_SID`
  - `TWILIO_AUTH_TOKEN`
  - `TWILIO_FROM_NUMBER`
- If provider config is missing, delivery is marked failed and still logged.
- SMS messages are sent for:
  - OTP verification for high-value transactions
  - Money received events
  - Credit card payments
  - Bill payments

## Example OTP Verification Flow

1. Client calls `POST /api/transaction/initiate` with transfer payload.
2. Server checks threshold.
3. If amount >= threshold:
   - generate 6-digit OTP
   - hash OTP and store in `otp_verifications`
   - set expiry to 5 minutes
   - send OTP over SMS
4. Client submits OTP via `POST /api/otp/verify`.
5. Server validates:
   - record exists and not expired
   - attempt count below max attempts
   - OTP hash matches
6. If valid, transfer is completed and record marked verified.

## Sample API Requests and Responses

### 1) Initiate Transaction

Request:

```http
POST /api/transaction/initiate
Authorization: Bearer <token>
Content-Type: application/json

{
  "fromAccountId": 101,
  "toAccountId": 102,
  "amount": 8000,
  "description": "High value transfer"
}
```

Response (OTP required):

```json
{
  "highValueThreshold": 5000,
  "status": "pending_verification",
  "requiresOtp": true,
  "transferId": "TRF-AB12CD34",
  "expiresInSeconds": 300,
  "attemptsRemaining": 3
}
```

### 2) Verify OTP

Request:

```http
POST /api/otp/verify
Authorization: Bearer <token>
Content-Type: application/json

{
  "transferId": "TRF-AB12CD34",
  "otp": "123456"
}
```

Response:

```json
{
  "status": "completed",
  "transferId": "TRF-AB12CD34",
  "result": {
    "debitTx": {
      "id": 901
    },
    "creditTx": {
      "id": 902
    }
  }
}
```

### 3) Send Notification

Request:

```http
POST /api/notifications/send
Authorization: Bearer <token>
Content-Type: application/json

{
  "customerId": 1,
  "message": "Your credit card payment has been processed.",
  "notificationType": "CREDIT_CARD_PAYMENT"
}
```

Response:

```json
{
  "id": 3001,
  "userId": 1,
  "phoneNumber": "+679812345",
  "message": "Your credit card payment has been processed.",
  "notificationType": "CREDIT_CARD_PAYMENT",
  "deliveryStatus": "queued",
  "timestamp": "2026-03-15T10:00:00.000Z"
}
```

### 4) Notification History

Request:

```http
GET /api/notifications/history?limit=50
Authorization: Bearer <token>
```

Response:

```json
[
  {
    "id": 3001,
    "userId": 1,
    "phoneNumber": "+679812345",
    "message": "You received FJD 1500.00 into account 102.",
    "notificationType": "MONEY_RECEIVED",
    "deliveryStatus": "queued",
    "providerMessageId": "SMxxxxxxxx",
    "timestamp": "2026-03-15T09:55:00.000Z"
  }
]
```
