# Coding Guidelines for gravitee-apim-gateway

## Logging

**Contextualized Logging:** In reactive code, prefer using `ctx.withLogger(log)` whenever possible to ensure proper logging context within the reactive stream.
