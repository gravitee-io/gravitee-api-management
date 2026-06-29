# Error Handling

The Payment Processing API uses standard HTTP status codes and returns structured error responses. Build resilient integrations by handling each category appropriately.

## Error response format

```json
{
  "code": "invalid_request",
  "message": "The amount field is required",
  "details": [
    { "field": "amount", "message": "This field is required" }
  ]
}
```

| Field | Description |
|-------|-------------|
| `code` | Machine-readable error identifier |
| `message` | Human-readable description |
| `details` | Optional array of field-level errors |

## Error categories

### Client errors (4xx)

Invalid requests, authentication failures, or resources not found. **Do not retry** — fix the request first.

| Status | Meaning |
|--------|---------|
| `400` | Malformed request or business rule violation |
| `401` | Missing or invalid API key |
| `404` | Payment not found |
| `409` | Idempotency conflict |
| `422` | Valid syntax but semantically incorrect (e.g. refund exceeds captured amount) |
| `429` | Rate limit exceeded |

### Server errors (5xx)

Temporary failures. **Retry with exponential backoff.**

| Status | Meaning |
|--------|---------|
| `500` | Internal server error |
| `502` | Bad gateway |
| `503` | Service temporarily unavailable |

### Payment errors

Declined transactions and fraud detection. Handle based on the specific decline reason in `failure_reason`.

## Common error codes

| Code | Description | Action |
|------|-------------|--------|
| `invalid_request` | Missing or malformed fields | Fix the request body |
| `authentication_failed` | Invalid or expired API key | Check your `X-API-Key` header |
| `insufficient_funds` | Payment method lacks funds | Ask customer for a different method |
| `card_declined` | Issuer declined the transaction | Display decline message to customer |
| `rate_limit_exceeded` | Too many requests | Wait for `X-RateLimit-Reset` |
| `idempotency_conflict` | Reused key with different body | Generate a new idempotency key |
| `payment_method_expired` | Card or method has expired | Request updated payment details |
| `fraud_detected` | Transaction flagged by fraud system | Do not retry; contact support |

## Retry strategy

```
4xx (except 429)  →  Do not retry
429               →  Retry after Retry-After header duration
5xx               →  Retry: 1s, 2s, 4s, 8s (max 5 attempts)
Network timeout   →  Retry with same Idempotency-Key
```

Always use [idempotency keys](./idempotency.md) when retrying payment creation requests.

## Rate limit headers

Every response includes rate limit information:

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 87
X-RateLimit-Reset: 1718445600
```

When `X-RateLimit-Remaining` reaches `0`, wait until `X-RateLimit-Reset` (Unix timestamp) before sending more requests.

## Example — handling a declined card

```javascript
const response = await createPayment(order);

if (response.status === 'failed') {
  switch (response.failure_reason) {
    case 'card_declined':
      showError('Your card was declined. Please try a different payment method.');
      break;
    case 'insufficient_funds':
      showError('Insufficient funds. Please use a different card.');
      break;
    case 'fraud_detected':
      showError('This transaction could not be processed. Please contact support.');
      break;
    default:
      showError('Payment failed. Please try again.');
  }
}
```
