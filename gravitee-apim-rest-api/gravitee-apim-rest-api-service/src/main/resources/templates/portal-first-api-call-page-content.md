# Making your first API call

Now that you know how authentication works, let’s make your first request. This guide shows you how to call a simple endpoint and interpret the response.

## Example: “Hello World” endpoint

This endpoint returns a basic JSON message confirming that your request succeeded.

**Example request**

```bash
  curl https://api.example.com/v1/hello \
  -H "Authorization: Bearer YOUR_API_KEY"
```

**Example response**

```json
{
  "message": "Hello, world!",
  "timestamp": "2025-01-01T00:00:00Z"
}
```

## Troubleshooting

If something goes wrong:

- **401 Unauthorized**: Your API key may be missing or invalid
- **403 Forbidden**: Your key doesn’t have access to this resource
- **429 Too Many Requests**: You’ve hit a rate limit

## Next steps

Explore the **Docs** to discover all available resources.