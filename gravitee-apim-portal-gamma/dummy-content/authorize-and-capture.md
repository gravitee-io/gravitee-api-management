# Authorize & Capture

The two-step payment flow lets you reserve funds at checkout and capture them later — ideal for marketplaces, pre-orders, and fulfillment workflows.

## Payment lifecycle

```
Create → Authorize → Capture → Settle
```

1. **Create** — Initialize a payment with amount, currency, and payment method
2. **Authorize** — The processor verifies the method and reserves funds on the customer's account
3. **Capture** — Transfer the authorized funds (supports partial captures)
4. **Settle** — Funds arrive in your merchant account, typically within 1–2 business days

## Step 1 — Authorize

Create a payment with `"capture": false`:

```bash
curl -X POST https://sandbox.payments.example.com/v2/payments \
  -H "X-API-Key: sk_test_your_key_here" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: a1b2c3d4-e5f6-7890-abcd-ef1234567890" \
  -d '{
    "amount": 10000,
    "currency": "USD",
    "payment_method": {
      "type": "card",
      "token": "tok_visa_test"
    },
    "capture": false
  }'
```

On success the payment status is `authorized`. The full authorized amount is held but not yet transferred.

## Step 2 — Capture

### Full capture

Omit the `amount` field to capture the entire authorized amount:

```bash
curl -X POST https://sandbox.payments.example.com/v2/payments/pay_2wB9xKqM7nP4/capture \
  -H "X-API-Key: sk_test_your_key_here" \
  -H "Content-Type: application/json" \
  -d '{}'
```

### Partial capture

Specify an `amount` less than the authorized total:

```bash
curl -X POST https://sandbox.payments.example.com/v2/payments/pay_2wB9xKqM7nP4/capture \
  -H "X-API-Key: sk_test_your_key_here" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 6000
  }'
```

If you authorized $100.00 (`10000` minor units) and capture $60.00 (`6000`), the remaining $40.00 is released back to the customer.

## Capture responses

| Status | Description |
|--------|-------------|
| `200` | Payment captured successfully |
| `400` | Payment cannot be captured (wrong status, already captured, etc.) |
| `404` | Payment not found |

After a successful capture, `amount_captured` on the Payment object reflects the total captured so far.

## Cancelling instead of capturing

If you no longer need the authorized funds, cancel the payment:

```bash
curl -X POST https://sandbox.payments.example.com/v2/payments/pay_2wB9xKqM7nP4/cancel \
  -H "X-API-Key: sk_test_your_key_here"
```

Only `pending` and `authorized` payments can be cancelled. Captured payments must be refunded via the Refunds API instead.

## When to use each flow

| Flow | Use case |
|------|----------|
| Single-step (`capture: true`) | Digital goods, instant delivery, subscriptions |
| Two-step (`capture: false`) | Physical goods, hotel bookings, custom manufacturing |
| Partial capture | Split shipments, variable final amounts, tipping |
