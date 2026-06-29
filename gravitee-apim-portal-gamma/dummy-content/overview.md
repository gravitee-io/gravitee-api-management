# Payment Processing API

Create, authorize, capture, and manage payments across multiple payment methods and currencies.

**Version:** 2.1.0  
**Base URL (Production):** `https://api.payments.example.com/v2`  
**Base URL (Sandbox):** `https://sandbox.payments.example.com/v2`

## What you can do

The Payment Processing API provides a complete payment lifecycle:

| Operation | Endpoint | Description |
|-----------|----------|-------------|
| Create payment | `POST /payments` | Initialize a new payment with amount, currency, and payment method |
| List payments | `GET /payments` | Retrieve a paginated list of payments with optional filters |
| Get payment | `GET /payments/{id}` | Retrieve full details for a specific payment |
| Capture payment | `POST /payments/{id}/capture` | Capture an authorized payment (full or partial) |
| Cancel payment | `POST /payments/{id}/cancel` | Cancel a pending or authorized payment |

## Payment statuses

Every payment moves through one of the following states:

- **pending** — Payment is being processed by the payment network
- **authorized** — Funds are reserved and ready for capture
- **captured** — Funds have been captured successfully
- **failed** — Payment was declined or encountered an error
- **cancelled** — Payment was cancelled before capture

## Supported payment methods

- **Card** — Visa, Mastercard, Amex, Discover, and more
- **Bank transfer** — ACH, SEPA, and wire transfers
- **Digital wallet** — Apple Pay, Google Pay, PayPal

## Multi-currency support

Process payments in 135+ currencies using ISO 4217 codes (e.g. `USD`, `EUR`, `GBP`). Amounts are always expressed in **minor units** (e.g. `5000` = $50.00).

## Authentication

All requests require an API key passed in the `X-API-Key` header. Generate keys in your Dashboard under **Settings → API Keys**.

## Related guides

- [Quick Start](./quick-start.md)
- [Creating Payments](./create-payment.md)
- [Authorize & Capture](./authorize-and-capture.md)
- [Payment Methods](./payment-methods.md)
- [Multi-Currency](./multi-currency.md)
- [Idempotency](./idempotency.md)
- [Sandbox Testing](./sandbox-testing.md)
