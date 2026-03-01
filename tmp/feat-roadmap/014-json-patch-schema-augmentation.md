# 014 — JSON Patch Schema Augmentation for GAD SDK

## Problem

The GAD SDK generates JSON schemas from Java definition model classes (`AbstractApi`, `Flow`, `Step`, etc.). These classes serve the **gateway runtime** and don't carry all fields needed by **management-layer consumers** like Zee. Examples:

- `AbstractApi` has no `description` field (management concern, not gateway definition)
- `Step.configuration` is `{"type": "object"}` — no policy-specific schema
- Future gaps are inevitable as definition DTOs serve a different audience than the LLM consumer

Modifying source Java classes to add management-layer fields pollutes the gateway definition model and creates coupling between unrelated concerns.

## Solution: JSON Patch at the Consumer Call Site

The SDK's `generate` method accepts an optional list of [RFC 6902 JSON Patch](https://datatracker.ietf.org/doc/html/rfc6902) operations. These patches are applied to the source schema **before** the LLM roundtrip (conversion → prompting → validation).

### SDK-Side Change

Library: `com.github.java-json-tools:json-patch`

New overload on each generated component:

```java
// Existing (unchanged)
static RoundtripResult generate(String prompt, LlmRoundtripEngine engine)

// New overload
static RoundtripResult generate(
    String prompt,
    LlmRoundtripEngine engine,
    List<JsonPatchOperation> schemaPatches
)
```

Engine pipeline becomes: **load schema → apply patches → convert for LLM → prompt → validate against patched schema**.

### APIM Consumer-Side Change

In `LlmEngineServiceImpl.buildRegistry()`, compose patches per-component:

```java
var descriptionPatch = List.of(
    JsonPatch.add("/properties/description", Map.of(
        "type", "string",
        "description", "Human-readable description of the API."
    ))
);

map.put("Api", (prompt, engine) -> Api.generate(prompt, engine, descriptionPatch));
```

One line. No Java class changes. No SDK rebuild. No schema regeneration.

### Why This Beats Source Class Modification

| Approach                   | Tradeoffs                                                                                                 |
| -------------------------- | --------------------------------------------------------------------------------------------------------- |
| Modify Java source classes | Pollutes gateway definition model with management concerns. Every consumer gets fields they may not want. |
| JSON Patch at call site    | Consumer-local. SDK stays a faithful mirror of the source. Each consumer augments for its own needs.      |

## Use Cases

### Immediate: API Description

```json
[{ "op": "add", "path": "/properties/description", "value": { "type": "string", "description": "Human-readable description of the API." } }]
```

### Future: Policy-Specific Step Configuration

When generating a Flow for a specific policy (e.g., rate-limit), patch `Step.configuration` with the actual policy schema at generation time:

```json
[{ "op": "replace", "path": "/$defs/Step/properties/configuration", "value": { "$ref": "#/$defs/RateLimitConfig" } }]
```

The schema becomes **context-dependent**, not static. This solves the `#/rate: expected type: JSONObject, found: Integer` validation error.

### Future: Different Consumers, Different Patches

- Console UI Zee widget: patches for management-layer fields
- CLI tools: patches for automation-specific fields
- Portal: patches for portal-specific metadata

## Implementation Checklist

- [ ] SDK: Add `List<JsonPatchOperation>` parameter to the engine's roundtrip pipeline
- [ ] SDK: Apply patches between schema load and LLM conversion
- [ ] SDK: Validate against patched schema (not original) on response
- [ ] SDK: Backward-compatible — existing `generate(prompt, engine)` delegates to new overload with empty patch list
- [ ] APIM: Update `ComponentInvoker` interface to support patches
- [ ] APIM: Add `description` patch for `Api` component
- [ ] APIM: Verify Zee generates descriptions correctly
- [ ] APIM: (Future) Add policy config patches for `Flow` component

## Dependencies

- `com.github.java-json-tools:json-patch` — used on both SDK and APIM sides
