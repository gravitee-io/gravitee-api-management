# Authentication

Authentication allows the platform to verify who you are and what you can access.

This guide introduces the authentication method used in the API and how to send secure requests.

## Authentication method

Our APIs use **Bearer tokens (API keys)** for all requests.
Each request must include your API key in the `Authorization` header.

**Example request**

```bash
  curl https://api.example.com/v1/data \
  -H "Authorization: Bearer YOUR_API_KEY"
```

## Best practices

- Keep your API keys secret and never share them
- Rotate your keys regularly
- Use environment variables to store credentials
- Review access logs for unusual activity

## Next steps

Once you understand how to authenticate, try making your first request in the next guide: **Making your first API call**.