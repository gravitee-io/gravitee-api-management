# 016 - Separate V2 and V4 Endpoints

## Goal

Decouple V2 and V4 generation by introducing a dedicated endpoint for V2 components, ensuring clean separation of concerns.

## Description

V2 and V4 schemas are completely unrelated domains. Rather than using a string prefix format (e.g., `"v2/Plan"`) in a single `/ai/generate` endpoint, we will split the endpoints.

- **V4 Endpoint**: `POST /management/v2/environments/{envId}/ai/generate` (or explicitly nested under v4).
- **V2 Endpoint**: `POST /management/v2/environments/{envId}/v2/ai/generate` (or similar logical path).

The frontend should also be adapted to have specific V2 and V4 wrappers (e.g., `ZeeWidgetV2` and `ZeeWidgetV4`) that share a common core component but point to their respective endpoints and models.

## Checklist

- [ ] Vend the updated `v2-0.1.0-ALPHA.jar` with enum/factory wrapper to `distribution/lib/ext/`.
- [ ] Create a new JAX-RS endpoint specifically for V2 generation.
- [ ] Refactor `LlmEngineServiceImpl` to expose separate `generateV4` and `generateV2` methods, leveraging the respective SDK Enums.
- [ ] Update the Console UI frontend to use separate widget contexts (`ZeeWidgetV2`, `ZeeWidgetV4`).
- [ ] Test at least 1-2 V2 models from the UI to demonstrate correct generation via the V2 endpoint.
