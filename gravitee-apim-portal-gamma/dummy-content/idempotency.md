# Idempotency

Payment operations are critical financial transactions. Use idempotency keys to prevent duplicate charges when requests are retried due to network issues, timeouts, or client errors.

## How it works

Include an `Idempotency-Key` header with a unique UUID on every `POST /payments` request:

```bash
curl -X POST https://api.payments.example.com/v2/payments \
  -H "X-API-Key: sk_live_your_key_here" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "amount": 5000,
    "currency": "USD",
    "payment_method": {
      "type": "card",
      "token": "tok_visa_test"
    }
  }'
```

The API stores the result of the first request keyed by `Idempotency-Key`. Subsequent requests with the **same key and same body** return the original response without creating a new payment.

## Rules

1. **Generate a new UUID for each distinct payment** — never reuse keys across different transactions
2. **Reuse the same key when retrying** — if a request times out, retry with the identical key and body
3. **Keys expire after 24 hours** — after expiry, the same key can be used for a new payment
4. **Body must match** — sending a different body with an existing key returns `409 Conflict`

## 409 Conflict

```json
{
  "code": "idempotency_conflict",
  "message": "A request with this Idempotency-Key was already processed with a different request body"
}
```

This means you accidentally reused a key with different parameters. Generate a new key and retry.

## Implementation pattern

```javascript
const { v4: uuidv4 } = require('uuid');

async function createPayment(order) {
  const idempotencyKey = order.idempotencyKey ?? uuidv4();

  // Persist the key with the order before sending
  await saveOrder({ ...order, idempotencyKey });

  const response = await fetch('https://api.payments.example.com/v2/payments', {
    method: 'POST',
    headers: {
      'X-API-Key': process.env.PAYMENTS_API_KEY,
      'Content-Type': 'application/json',
      'Idempotency-Key': idempotencyKey,
    },
    body: JSON.stringify({
      amount: order.amount,
      currency: order.currency,
      payment_method: order.paymentMethod,
    }),
  });

  if (!response.ok && response.status >= 500) {
    // Safe to retry with the same idempotencyKey
    throw new RetryableError('Payment API unavailable');
  }

  return response.json();
}
```

## What requires an idempotency key?

| Endpoint | Idempotency-Key required? |
|----------|---------------------------|
| `POST /payments` | **Yes** |
| `POST /payments/{id}/capture` | Recommended |
| `POST /payments/{id}/cancel` | Recommended |
| `GET /payments` | No |
| `GET /payments/{id}` | No |

Always include idempotency keys for any operation that moves money.
