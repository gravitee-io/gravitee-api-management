# Existing Kafka Native Policies Reference

Use these as examples when creating a new policy. Ordered from simplest to most complex.

## Quick Reference Table

| Policy | Phases | Complexity | Good Example For |
|--------|--------|------------|------------------|
| kafka-message-filtering | SUBSCRIBE | Simple | Message-level filtering, EL evaluation |
| kafka-quota | PUBLISH,SUBSCRIBE | Medium | Both message directions, network throttling |
| kafka-topic-mapping | INTERACT | Medium | onInitialize, per-ApiKey operations, response actions |
| kafka-ip-filtering | INTERACT | Medium | Connection-level checks, caching |
| kafka-acl | INTERACT | Complex | Strategy pattern per ApiKey, authorization |
| kafka-transform-key | ? | Medium | Message key transformation |
| kafka-virtual-topics | ? | Medium | Topic virtualization |
| kafka-message-encryption-decryption | PUBLISH,SUBSCRIBE | Complex | Crypto processors, factory pattern |

## Repository Locations

All under `~/workspace/Gravitee/`:

```
gravitee-policy-kafka-acl/
gravitee-policy-kafka-ip-filtering/
gravitee-policy-kafka-message-filtering/
gravitee-policy-kafka-message-encryption-decryption/
gravitee-policy-kafka-quota/
gravitee-policy-kafka-topic-mapping/
gravitee-policy-kafka-transform-key/
gravitee-policy-kafka-virtual-topics/
```

## Pattern: Simple Message Filter (kafka-message-filtering)

**Best starting point for SUBSCRIBE-only policies.**

- Single phase: `onMessageResponse`
- Uses EL template engine to evaluate filter conditions per message
- Returns `Maybe.just(message)` to keep, `Maybe.empty()` to filter
- Configuration: just `filter` (String with EL) and `excludeMessagesOnError` (boolean)
- ~76 lines of policy code

## Pattern: Protocol-Level with Strategy per ApiKey (kafka-acl)

**Best starting point for INTERACT policies that need different behavior per Kafka operation.**

- Uses a Map of `ApiKeys -> AuthorizeStrategy` to dispatch logic
- Each ApiKey (PRODUCE, FETCH, CREATE_TOPICS, etc.) has its own strategy
- Interrupts with Kafka protocol errors on unauthorized access
- Configuration: list of Authorization rules with resources and operations

## Pattern: Request + Response Action (kafka-topic-mapping)

**Best example for policies that transform requests AND need to reverse the transform on responses.**

- `onInitialize`: evaluates EL expressions once, registers topic identities
- `onRequest`: applies topic name mapping and registers a response callback via `ctx.addActionOnResponse(...)`
- Response callback reverses the mapping
- Uses `MappingOperation` interface with 50+ implementations per ApiKey

## Pattern: Both Message Directions with Throttling (kafka-quota)

**Best example for PUBLISH,SUBSCRIBE policies that affect network behavior.**

- `onMessageRequest`: measures produce size, records to sensor, throttles on violation
- `onMessageResponse`: measures fetch size, records to sensor, throttles on violation
- Uses `executionContext.networkController().pause(Duration)` for throttling
- Uses `response.delegate().maybeSetThrottleTimeMs(...)` to communicate throttle to client

## Key Imports Cheat Sheet

```java
// Core policy interface
import io.gravitee.gateway.reactive.api.policy.kafka.KafkaPolicy;

// Context types
import io.gravitee.gateway.reactive.api.context.kafka.KafkaConnectionContext;
import io.gravitee.gateway.reactive.api.context.kafka.KafkaExecutionContext;
import io.gravitee.gateway.reactive.api.context.kafka.KafkaMessageExecutionContext;

// Message types
import io.gravitee.gateway.reactive.api.message.kafka.KafkaMessage;

// Configuration
import io.gravitee.policy.api.PolicyConfiguration;

// Kafka protocol
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.requests.AbstractRequest;
import org.apache.kafka.common.requests.AbstractResponse;
import org.apache.kafka.common.requests.ProduceRequest;
import org.apache.kafka.common.requests.FetchResponse;

// RxJava
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Flowable;

// Error handling
import io.gravitee.gateway.reactive.api.context.kafka.Errors;

// Template engine
import io.gravitee.el.TemplateEngine;

// Topic registry
import io.gravitee.gateway.reactive.api.context.kafka.TopicIdentity;
```
