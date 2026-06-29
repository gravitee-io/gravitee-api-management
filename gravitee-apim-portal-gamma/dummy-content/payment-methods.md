# Payment Methods

The Payment Processing API supports three payment method types. Each type uses a tokenized identifier — never send raw card numbers or bank account details to the API.

## Card payments

**Type:** `card`

Accept major card networks including Visa, Mastercard, American Express, and Discover.

```json
{
  "payment_method": {
    "type": "card",
    "token": "tok_visa_test"
  }
}
```

### Features

- 3D Secure (SCA) for European transactions
- Recurring payments and saved cards
- Real-time authorization and capture
- Card brand and last-four digits returned in the Payment object

### Tokenization

Collect card details on the client using our JavaScript SDK or mobile SDK. The SDK returns a single-use or reusable token that you pass to `POST /payments`. Card data never touches your servers, simplifying PCI compliance.

## Bank transfers

**Type:** `bank_transfer`

Lower fees and higher limits — ideal for B2B payments and large transactions.

```json
{
  "payment_method": {
    "type": "bank_transfer",
    "token": "tok_ach_test"
  }
}
```

### Supported rails

| Rail | Region | Settlement |
|------|--------|------------|
| ACH | United States | 3–5 business days |
| SEPA | European Union | 1–2 business days |
| Wire | Global | Same day to 2 business days |

Bank transfer payments typically start in `pending` status and move to `captured` once the transfer clears.

## Digital wallets

**Type:** `wallet`

One-tap checkout for higher conversion rates.

```json
{
  "payment_method": {
    "type": "wallet",
    "token": "tok_applepay_test"
  }
}
```

### Supported wallets

- Apple Pay
- Google Pay
- PayPal

Wallet tokens are obtained through the respective wallet SDK on the client device. They represent a pre-authorized payment method scoped to your merchant account.

## Choosing a payment method

| Method | Best for | Typical settlement |
|--------|----------|-------------------|
| Card | E-commerce, SaaS, subscriptions | Instant |
| Bank transfer | Invoices, B2B, high-value | 1–5 business days |
| Wallet | Mobile checkout, recurring | Instant |

## Response fields

The Payment object includes a masked representation of the method used:

```json
{
  "payment_method": {
    "type": "card",
    "last_four": "4242",
    "brand": "visa"
  }
}
```

Bank transfers and wallets return `type` only; sensitive account details are never exposed in API responses.
