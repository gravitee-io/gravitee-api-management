# Multi-Currency Support

Process payments in 135+ currencies with real-time exchange rates applied at capture time.

## Specifying currency

Pass a three-letter ISO 4217 code in the `currency` field:

```json
{
  "amount": 4500,
  "currency": "EUR",
  "payment_method": {
    "type": "card",
    "token": "tok_visa_test"
  }
}
```

Common codes: `USD`, `EUR`, `GBP`, `JPY`, `CAD`, `AUD`, `CHF`, `SEK`, `NOK`, `DKK`.

## Amounts in minor units

All amounts are integers in the currency's smallest unit:

| Currency | Minor unit | Example |
|----------|------------|---------|
| USD, EUR, GBP | Cents / pence | `5000` = €50.00 |
| JPY | Yen (no decimals) | `5000` = ¥5,000 |
| BHD, KWD | Fils (3 decimals) | `5000` = 5.000 BHD |

Always validate amounts on the client before sending. The API rejects fractional minor units for zero-decimal currencies like JPY.

## Exchange rates

When a customer's payment method is denominated in a different currency than the charge:

1. The authorization uses the exchange rate at authorization time
2. The capture locks in the rate at capture time
3. Settlement to your merchant account is in your configured settlement currency

Exchange rates are updated in real time. For multi-currency reporting, store both the charge currency and your settlement currency amounts in `metadata`.

## Regional payment methods

Local payment methods are available in supported regions and are automatically presented based on the charge currency and customer location:

| Region | Currency | Local methods |
|--------|----------|---------------|
| Europe | EUR | SEPA, iDEAL, Bancontact |
| United Kingdom | GBP | Bacs Direct Debit |
| United States | USD | ACH, Apple Pay, Google Pay |
| Japan | JPY | Convenience store payments |

## Example — multi-currency order

```bash
# Charge a UK customer in GBP
curl -X POST https://api.payments.example.com/v2/payments \
  -H "X-API-Key: sk_live_your_key_here" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: f47ac10b-58cc-4372-a567-0e02b2c3d479" \
  -d '{
    "amount": 8999,
    "currency": "GBP",
    "payment_method": {
      "type": "card",
      "token": "tok_visa_uk"
    },
    "description": "Annual subscription",
    "metadata": {
      "settlement_currency": "USD",
      "product_sku": "SUB-ANNUAL-GB"
    }
  }'
```

## Listing payments by currency

Filter payments when listing:

```bash
curl "https://api.payments.example.com/v2/payments?status=captured&limit=50" \
  -H "X-API-Key: sk_live_your_key_here"
```

Use `metadata` fields to tag payments with your internal currency or pricing tier for reconciliation.
