# Quick Start

Get up and running with the Payment Processing API in under five minutes.

## Prerequisites

- An active developer account with API access enabled
- Your API key (available in the Dashboard under **Settings → API Keys**)
- A tool for making HTTP requests (cURL, Postman, or any HTTP client)

## Step 1 — Create an application

Applications represent your integration and hold your API credentials. Each application can subscribe to multiple APIs with different plans.

1. Navigate to the **Applications** page and click **Create Application**
2. Enter a name and description for your application
3. Select the **Payment Processing API**
4. Choose a subscription plan that fits your needs

## Step 2 — Make your first payment

Send a `POST` request to `/payments`. Include your API key in the `X-API-Key` header, set `Content-Type` to `application/json`, and provide an `Idempotency-Key` header to prevent duplicate charges.

```bash
curl -X POST https://sandbox.payments.example.com/v2/payments \
  -H "X-API-Key: sk_test_your_key_here" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "amount": 5000,
    "currency": "USD",
    "payment_method": {
      "type": "card",
      "token": "tok_visa_test"
    },
    "description": "Order #12345",
    "customer_email": "customer@example.com",
    "capture": true
  }'
```

### Request body fields

| Field | Required | Description |
|-------|----------|-------------|
| `amount` | Yes | Payment amount in minor units (e.g. `5000` = $50.00) |
| `currency` | Yes | ISO 4217 currency code (e.g. `USD`) |
| `payment_method` | Yes | Object with `type` and `token` |
| `description` | No | Optional payment description |
| `customer_email` | No | Customer email for receipt |
| `metadata` | No | Custom key-value pairs |
| `capture` | No | `true` (default) to capture immediately; `false` to authorize only |

## Step 3 — Verify the payment

Use `GET /payments/{id}` to check the payment status at any time:

```bash
curl https://sandbox.payments.example.com/v2/payments/pay_2wB9xKqM7nP4 \
  -H "X-API-Key: sk_test_your_key_here"
```

Example response:

```json
{
  "id": "pay_2wB9xKqM7nP4",
  "amount": 5000,
  "amount_captured": 5000,
  "currency": "USD",
  "status": "captured",
  "payment_method": {
    "type": "card",
    "last_four": "4242",
    "brand": "visa"
  },
  "description": "Order #12345",
  "customer_email": "customer@example.com",
  "created_at": "2026-06-15T10:30:00Z",
  "updated_at": "2026-06-15T10:30:02Z"
}
```

## Next steps

- Learn about [authorize-then-capture flows](./authorize-and-capture.md) for delayed settlement
- Review [payment method types](./payment-methods.md) and tokenization
- Test edge cases with [sandbox test cards](./sandbox-testing.md)
