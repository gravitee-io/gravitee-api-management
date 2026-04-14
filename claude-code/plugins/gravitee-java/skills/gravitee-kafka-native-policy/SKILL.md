---
name: gravitee-kafka-native-policy
description: Guide and scaffold the creation of a new Gravitee Kafka native policy. Use when a developer needs to create a new policy plugin that operates on the Kafka protocol within the Gravitee gateway, or when modifying an existing Kafka native policy.
---

# Kafka Native Policy Development Guide

## Overview

This skill helps you create a new **Kafka native policy** for the Gravitee APIM gateway. Kafka native policies intercept Kafka protocol traffic at the gateway level and can operate on requests, responses, and individual messages (ProduceRequest / FetchResponse records).

## When to Use

- Developer asks to "create a new Kafka policy", "scaffold a Kafka native policy", or "add a Kafka policy plugin"
- Developer needs to understand the structure and conventions of Kafka native policies
- Developer is modifying an existing Kafka native policy and needs context on the architecture

## Operational Workflow

### Phase 1: Discovery

Before generating any code, ask the developer:

1. **Policy name**: What should the policy be called? (e.g., "kafka-rate-limiting", "kafka-header-injection")
2. **Execution phases**: Which Kafka phases should the policy operate on?
   - `INTERACT` = REQUEST + RESPONSE (protocol-level, e.g., ACL checks, topic mapping)
   - `PUBLISH` = MESSAGE_REQUEST (per-message on ProduceRequest, e.g., encryption, validation)
   - `SUBSCRIBE` = MESSAGE_RESPONSE (per-message on FetchResponse, e.g., filtering, decryption)
   - `PUBLISH,SUBSCRIBE` = both message phases (e.g., quotas, encryption/decryption)
3. **Configuration**: What user-configurable options does the policy need?
4. **GroupId**: Community (`io.gravitee.policy`) or Enterprise (`com.graviteesource.policy`)?
5. **Target repository location**: Where should it be created? (default: `~/workspace/Gravitee/gravitee-policy-kafka-<name>`)

### Phase 2: Scaffold

Generate the complete project structure following the templates in the references. Create all files in order:

1. `pom.xml` - see `references/01-pom-template.md`
2. `src/main/resources/plugin.properties` - see `references/02-plugin-properties.md`
3. `src/main/resources/schemas/schema-form.json` - see `references/03-schema-form.md`
4. `src/assembly/policy-assembly.xml` - see `references/04-assembly.md`
5. Main policy class implementing `KafkaPolicy` - see `references/05-policy-class.md`
6. Configuration class implementing `PolicyConfiguration` - see `references/06-configuration-class.md`
7. Unit tests - see `references/07-testing.md`
8. `DOCUMENTATION.adoc` and `README.adoc` - minimal stubs

### Phase 3: Validate

- Verify the project compiles: `mvn compile`
- Run tests: `mvn test`
- Format code: `mvn prettier:write`
- Package: `mvn package` (produces the `.zip` assembly)

## Key Architecture Concepts

### KafkaPolicy Interface

```java
public interface KafkaPolicy extends BasePolicy {
    // Called once when connection is established (API deployment)
    default Completable onInitialize(KafkaConnectionContext ctx);

    // Called per Kafka request (protocol-level)
    default Completable onRequest(KafkaExecutionContext ctx);

    // Called per Kafka response (protocol-level)
    default Completable onResponse(KafkaExecutionContext ctx);

    // Called per message in ProduceRequest
    default Completable onMessageRequest(KafkaMessageExecutionContext ctx);

    // Called per message in FetchResponse
    default Completable onMessageResponse(KafkaMessageExecutionContext ctx);
}
```

### Execution Phase Mapping

| plugin.properties value | Methods called | Use case |
|------------------------|----------------|----------|
| `native_kafka=INTERACT` | `onRequest` + `onResponse` | Protocol-level (ACL, topic mapping, IP filtering) |
| `native_kafka=PUBLISH` | `onMessageRequest` | Per-message on produce (encryption, validation) |
| `native_kafka=SUBSCRIBE` | `onMessageResponse` | Per-message on fetch (filtering, decryption) |
| `native_kafka=PUBLISH,SUBSCRIBE` | `onMessageRequest` + `onMessageResponse` | Both directions (quota, encryption/decryption) |

### Context Hierarchy

- **`KafkaConnectionContext`** (onInitialize) - connection-level, access to `topicIdentityRegistry()`, `getTemplateEngine()`
- **`KafkaExecutionContext`** (onRequest/onResponse) - per-request, access to `apiKey()`, `request()`, `response()`, `principal()`, `correlationId()`, `interruptWith()`, `addActionOnResponse()`
- **`KafkaMessageExecutionContext`** (onMessageRequest/onMessageResponse) - per-message, access to `request().onMessages()`, `response().onMessages()`, `executionContext()`, `getTemplateEngine(message)`

### Error Management

Policies reject requests via `ctx.interruptWith(...)`. Two overloads exist:

- **`ctx.interruptWith(Errors errors)`** - simple Kafka error code (e.g., `Errors.TOPIC_AUTHORIZATION_FAILED`). The gateway builds the protocol response automatically.
- **`ctx.interruptWith(AbstractResponse response)`** - full protocol response with per-topic/partition errors, throttle times, etc.

Use `Errors.forException(throwable)` to map generic Java exceptions to the closest Kafka error code.

Common error handling patterns (see `references/09-error-management.md` for full details):

1. **Try/Catch with custom exception** (kafka-acl pattern) - throw a domain exception carrying `Errors` or `AbstractResponse`, catch in the policy method
2. **`onErrorResumeNext`** (kafka-topic-mapping pattern) - let errors propagate through the reactive chain, map them at the end
3. **Direct interruption** - call `interruptWith()` inline for simple validation checks
4. **Throttling without interruption** (kafka-quota pattern) - pause network via `ctx.networkController().pause(Duration)` and set throttle time on response

Commonly used Kafka error codes in policies:
- `Errors.TOPIC_AUTHORIZATION_FAILED` (29) - unauthorized topic access
- `Errors.CLUSTER_AUTHORIZATION_FAILED` (31) - unauthorized cluster operation
- `Errors.POLICY_VIOLATION` (44) - generic policy rule violation
- `Errors.INVALID_REQUEST` (42) - malformed request
- `Errors.THROTTLING_QUOTA_EXCEEDED` (89) - quota exceeded
- `Errors.UNKNOWN_SERVER_ERROR` (-1) - fallback for unexpected errors

### Protocol Versioning

Kafka request/response schemas change between protocol versions. The same ApiKey (e.g., Produce, Fetch) can have different field layouts, encodings, and semantics depending on the negotiated version. The gateway intersects client and broker supported versions, so **policy code must handle whichever version was negotiated**.

Key implications for policy authors:
- **Fields appear/disappear by version**: e.g., `FindCoordinatorRequest.data().key()` in v0-v3 becomes `coordinatorKeys()` in v4+
- **Collections change type**: e.g., `DeleteTopicsRequest.data().topicNames()` (v0-v5) vs `topics()` (v6+)
- **Authorization semantics shift**: e.g., `AddPartitionsToTxnRequest` v4+ uses CLUSTER scope, v3- uses per-transactional-id
- **Response fields are version-gated**: e.g., `MetadataResponse.authorizedOperations` only in v8+

Always use `request.version()` to branch logic when accessing version-dependent fields. Use `request.getErrorResponse(throttleMs, exception)` to build version-appropriate error responses automatically. See `references/10-protocol-versioning.md` for patterns and the full API keys table.

### Reactive Programming

All policy methods return RxJava 3 `Completable`. Key patterns:
- `Completable.defer(() -> ...)` - lazy evaluation (preferred)
- `Completable.fromRunnable(() -> ...)` - synchronous work
- `ctx.response().onMessages(messages -> messages.concatMapMaybe(...))` - message transformation
- `ctx.addActionOnResponse(context -> Completable.fromRunnable(...))` - deferred response actions

## Reference Files

Open these files for detailed templates and examples:

- Project template (pom.xml): `references/01-pom-template.md`
- Plugin manifest: `references/02-plugin-properties.md`
- Configuration schema: `references/03-schema-form.md`
- Assembly descriptor: `references/04-assembly.md`
- Policy class patterns: `references/05-policy-class.md`
- Configuration class: `references/06-configuration-class.md`
- Testing patterns: `references/07-testing.md`
- Existing policies reference: `references/08-existing-policies.md`
- Error management & Kafka error codes: `references/09-error-management.md`
- Protocol versioning & API keys: `references/10-protocol-versioning.md`
