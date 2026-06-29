# Cancel Payments

Cancel a pending or authorized payment to release reserved funds without capturing them.

## Endpoint

```
POST /payments/{id}/cancel
```

No request body is required.

## When cancellation is allowed

| Current status | Can cancel? | Notes |
|----------------|-------------|-------|
| `pending` | Yes | Payment has not yet been authorized |
| `authorized` | Yes | Reserved funds are released |
| `captured` | No | Use the Refunds API instead |
| `failed` | No | Payment already terminated |
| `cancelled` | No | Already cancelled |

## Example

```bash
curl -X POST https://api.payments.example.com/v2/payments/pay_2wB9xKqM7nP4/cancel \
  -H "X-API-Key: sk_live_your_key_here"
```

### Success response (200)

Returns the updated Payment object with `status: "cancelled"`:

```json
{
  "id": "pay_2wB9xKqM7nP4",
  "amount": 10000,
  "amount_captured": 0,
  "currency": "USD",
  "status": "cancelled",
  "payment_method": {
    "type": "card",
    "last_four": "4242",
    "brand": "visa"
  },
  "created_at": "2026-06-15T10:30:00Z",
  "updated_at": "2026-06-15T11:45:00Z"
}
```

### Error responses

| Status | Cause |
|--------|-------|
| `400` | Payment is in a state that cannot be cancelled (e.g. already captured) |
| `404` | Payment ID not found |

## Common scenarios

### Order cancelled before fulfillment

A customer cancels an order while the payment is still `authorized`. Cancel the payment to release the hold on their card.

### Authorization timeout

Authorizations expire after 7 days by default. Cancel proactively if you know you will not capture, or the authorization will expire automatically.

### Inventory unavailable

If you cannot fulfill an order, cancel the authorization rather than capturing and refunding — this is faster for the customer and avoids refund fees.

## Cancel vs. refund

| Action | Payment status required | Effect |
|--------|------------------------|--------|
| Cancel | `pending` or `authorized` | Releases hold; no money moved |
| Refund | `captured` | Returns captured funds to customer |
