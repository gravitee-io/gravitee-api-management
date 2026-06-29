# Sandbox Testing

Test your integration without processing real transactions using the sandbox environment and test payment methods.

## Sandbox base URL

```
https://sandbox.payments.example.com/v2
```

Sandbox API keys are available in your Dashboard under **Settings → API Keys**. Sandbox and production keys are separate — never mix them.

## Test card numbers

Use these card numbers with any future expiry date and any three-digit CVC:

| Card number | Outcome |
|-------------|---------|
| `4242 4242 4242 4242` | Successful payment |
| `4000 0000 0000 0002` | Card declined |
| `4000 0000 0000 9995` | Insufficient funds |
| `4000 0000 0000 0069` | Expired card |
| `4000 0025 0000 3155` | Requires 3D Secure authentication |

Tokenize test cards through the sandbox SDK to obtain `tok_*` tokens for API requests.

## Testing payment flows

### Successful one-step payment

```bash
curl -X POST https://sandbox.payments.example.com/v2/payments \
  -H "X-API-Key: sk_test_your_key_here" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{
    "amount": 2000,
    "currency": "USD",
    "payment_method": { "type": "card", "token": "tok_visa_test" },
    "capture": true
  }'
```

Expected result: `status: "captured"`.

### Authorize-then-capture

```bash
# 1. Authorize
curl -X POST https://sandbox.payments.example.com/v2/payments \
  -H "X-API-Key: sk_test_your_key_here" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{
    "amount": 5000,
    "currency": "USD",
    "payment_method": { "type": "card", "token": "tok_visa_test" },
    "capture": false
  }'

# 2. Capture (use the id from step 1)
curl -X POST https://sandbox.payments.example.com/v2/payments/pay_XXXXX/capture \
  -H "X-API-Key: sk_test_your_key_here" \
  -H "Content-Type: application/json" \
  -d '{}'
```

### Declined payment

Use token `tok_card_declined` or card `4000 0000 0000 0002`. Expected result: `status: "failed"` with a `failure_reason`.

## Sandbox vs. production

| | Sandbox | Production |
|---|---------|------------|
| Base URL | `sandbox.payments.example.com` | `api.payments.example.com` |
| API keys | `sk_test_*` | `sk_live_*` |
| Real money | No | Yes |
| Card networks | Simulated | Live |
| Webhooks | Delivered to registered endpoints | Delivered to registered endpoints |
| Rate limits | Relaxed | Per plan |

Behavior, endpoints, and response schemas are identical between environments.

## Going to production

1. Complete integration testing in sandbox
2. Switch to production API keys (`sk_live_*`) in your deployment configuration
3. Update the base URL to `https://api.payments.example.com/v2`
4. Register production webhook endpoints
5. Enable IP allowlisting if required by your security policy

No code changes are needed beyond configuration — request and response formats are the same.
