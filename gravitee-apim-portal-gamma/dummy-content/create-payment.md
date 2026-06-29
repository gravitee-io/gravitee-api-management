# Creating Payments

Use `POST /payments` to initialize a new payment with the specified amount, currency, and payment method.

## Endpoint

```
POST /payments
```

### Headers

| Header | Required | Description |
|--------|----------|-------------|
| `X-API-Key` | Yes | Your API key |
| `Content-Type` | Yes | `application/json` |
| `Idempotency-Key` | Yes | Unique UUID to prevent duplicate charges |

## Request body

```json
{
  "amount": 5000,
  "currency": "USD",
  "payment_method": {
    "type": "card",
    "token": "tok_visa_test"
  },
  "description": "Order #12345",
  "customer_email": "customer@example.com",
  "metadata": {
    "order_id": "12345",
    "customer_tier": "premium"
  },
  "capture": true
}
```

### `payment_method` object

| Field | Required | Description |
|-------|----------|-------------|
| `type` | Yes | One of `card`, `bank_transfer`, or `wallet` |
| `token` | Yes | Tokenized payment method identifier from your client-side integration |

### `capture` flag

- **`true`** (default) — Authorize and capture in a single step. The payment moves directly to `captured` on success.
- **`false`** — Authorize only. Funds are reserved but not transferred. You must call `POST /payments/{id}/capture` later to complete the payment.

## Responses

### 201 Created

Returns the full [Payment object](./overview.md#payment-statuses) with status `pending`, `authorized`, or `captured` depending on the outcome and `capture` flag.

### 400 Bad Request

Invalid request body or missing required fields.

```json
{
  "code": "invalid_request",
  "message": "The amount field is required",
  "details": [
    { "field": "amount", "message": "This field is required" }
  ]
}
```

### 409 Conflict

A different request body was sent with the same `Idempotency-Key`. Retry with a new key or reuse the original request body.

### 429 Too Many Requests

Rate limit exceeded. Check `X-RateLimit-Reset` and retry after the window resets.

## Example — authorize only

```bash
curl -X POST https://sandbox.payments.example.com/v2/payments \
  -H "X-API-Key: sk_test_your_key_here" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 7c9e6679-7425-40de-944b-e07fc1f90ae7" \
  -d '{
    "amount": 12500,
    "currency": "EUR",
    "payment_method": {
      "type": "card",
      "token": "tok_mastercard_test"
    },
    "capture": false
  }'
```

The response will have `status: "authorized"`. Capture the funds when you are ready to fulfill the order — see [Authorize & Capture](./authorize-and-capture.md).
