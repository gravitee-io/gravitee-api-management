# List & Retrieve Payments

Query payment history and retrieve individual payment details for reconciliation, customer support, and reporting.

## List payments

```
GET /payments
```

Returns a paginated list of payments. Results are sorted by `created_at` descending (newest first).

### Query parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `limit` | integer | `20` | Results per page (max `100`) |
| `offset` | integer | `0` | Number of results to skip |
| `status` | string | — | Filter by status: `pending`, `authorized`, `captured`, `failed`, `cancelled` |
| `created_after` | datetime | — | ISO 8601 timestamp — include payments created after this time |
| `created_before` | datetime | — | ISO 8601 timestamp — include payments created before this time |

### Example

```bash
curl "https://api.payments.example.com/v2/payments?status=captured&limit=20&offset=0&created_after=2026-06-01T00:00:00Z" \
  -H "X-API-Key: sk_live_your_key_here"
```

### Response

```json
{
  "data": [
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
      "created_at": "2026-06-15T10:30:00Z",
      "updated_at": "2026-06-15T10:30:02Z"
    }
  ],
  "total": 142,
  "limit": 20,
  "offset": 0
}
```

### Pagination

Use `offset` and `limit` to page through results:

```
Page 1: ?limit=20&offset=0
Page 2: ?limit=20&offset=20
Page 3: ?limit=20&offset=40
```

Stop when `offset + data.length >= total`.

## Get payment details

```
GET /payments/{id}
```

Retrieve the full details of a specific payment by ID.

```bash
curl https://api.payments.example.com/v2/payments/pay_2wB9xKqM7nP4 \
  -H "X-API-Key: sk_live_your_key_here"
```

### Response fields

| Field | Description |
|-------|-------------|
| `id` | Unique payment identifier (e.g. `pay_2wB9xKqM7nP4`) |
| `amount` | Original authorized amount in minor units |
| `amount_captured` | Total captured amount (may be less than `amount` for partial captures) |
| `currency` | ISO 4217 currency code |
| `status` | Current payment status |
| `payment_method` | Masked payment method details |
| `description` | Payment description, if provided |
| `customer_email` | Customer email, if provided |
| `metadata` | Custom key-value metadata |
| `failure_reason` | Decline or error reason (only when `status` is `failed`) |
| `created_at` | ISO 8601 creation timestamp |
| `updated_at` | ISO 8601 last update timestamp |

### 404 Not Found

Returned when the payment ID does not exist or belongs to another account.

## Use cases

- **Reconciliation** — Export captured payments for a date range using `created_after` / `created_before`
- **Customer support** — Look up a payment by ID from an order reference stored in `metadata`
- **Dashboards** — Poll `GET /payments?status=failed` to surface declined transactions
- **Webhooks complement** — Use list/retrieve as a fallback when webhook delivery fails

For real-time updates, prefer webhooks over polling.
