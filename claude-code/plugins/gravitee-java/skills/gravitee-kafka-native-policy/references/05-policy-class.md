# Policy Class Patterns

The main policy class implements `io.gravitee.gateway.reactive.api.policy.kafka.KafkaPolicy` and is the entry point for the policy logic.

## Package Convention

```
com.graviteesource.policy.kafka.<policy_name>   (enterprise)
io.gravitee.policy.kafka.<policy_name>          (community)
```

Use underscores in package names for multi-word policies (e.g., `topic_mapping`, `messagefiltering`).

## Template: INTERACT Policy (Protocol-Level)

For policies that operate on Kafka requests/responses (ACL, topic mapping, IP filtering):

```java
package com.graviteesource.policy.kafka.<policy_package>;

import com.graviteesource.policy.kafka.<policy_package>.configuration.<PolicyName>PolicyConfiguration;
import io.gravitee.gateway.reactive.api.context.kafka.KafkaConnectionContext;
import io.gravitee.gateway.reactive.api.context.kafka.KafkaExecutionContext;
import io.gravitee.gateway.reactive.api.policy.kafka.KafkaPolicy;
import io.reactivex.rxjava3.core.Completable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class <PolicyName>Policy implements KafkaPolicy {

    private final <PolicyName>PolicyConfiguration configuration;

    public <PolicyName>Policy(<PolicyName>PolicyConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String id() {
        return "kafka-<policy-id>";
    }

    @Override
    public Completable onRequest(KafkaExecutionContext ctx) {
        return Completable.defer(() -> {
            // Access Kafka ApiKey to decide what to do
            // ctx.apiKey() returns the Kafka operation type (PRODUCE, FETCH, METADATA, etc.)

            // Access the raw Kafka request
            // AbstractRequest request = ctx.request().delegate();

            // To reject a request:
            // return ctx.interruptWith(errors);

            return Completable.complete();
        });
    }

    @Override
    public Completable onResponse(KafkaExecutionContext ctx) {
        return Completable.defer(() -> {
            // Access the raw Kafka response
            // AbstractResponse response = ctx.response().delegate();

            return Completable.complete();
        });
    }
}
```

## Template: PUBLISH/SUBSCRIBE Policy (Message-Level)

For policies that operate on individual messages in ProduceRequest/FetchResponse:

```java
package com.graviteesource.policy.kafka.<policy_package>;

import com.graviteesource.policy.kafka.<policy_package>.configuration.<PolicyName>PolicyConfiguration;
import io.gravitee.gateway.reactive.api.context.kafka.KafkaMessageExecutionContext;
import io.gravitee.gateway.reactive.api.message.kafka.KafkaMessage;
import io.gravitee.gateway.reactive.api.policy.kafka.KafkaPolicy;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class <PolicyName>Policy implements KafkaPolicy {

    private final <PolicyName>PolicyConfiguration configuration;

    @Override
    public String id() {
        return "kafka-<policy-id>";
    }

    @Override
    public Completable onMessageRequest(KafkaMessageExecutionContext ctx) {
        // Per-message processing on ProduceRequest
        return Completable.defer(() ->
            ctx.request().onMessages(messages ->
                messages.concatMapMaybe(message -> processMessage(ctx, message))
            )
        );
    }

    @Override
    public Completable onMessageResponse(KafkaMessageExecutionContext ctx) {
        // Per-message processing on FetchResponse
        return Completable.defer(() ->
            ctx.response().onMessages(messages ->
                messages.concatMapMaybe(message -> processMessage(ctx, message))
            )
        );
    }

    private Maybe<KafkaMessage> processMessage(KafkaMessageExecutionContext ctx, KafkaMessage message) {
        // Return Maybe.just(message) to keep the message
        // Return Maybe.empty() to filter out the message
        // Return Maybe.error(throwable) to signal an error
        return Maybe.just(message);
    }
}
```

## Template: Policy with onInitialize (Connection-Level Setup)

For policies that need to set up state when the connection is established (e.g., topic mapping):

```java
@Override
public Completable onInitialize(KafkaConnectionContext ctx) {
    return Completable.fromRunnable(() -> {
        // Access the topic identity registry
        var topicIdentityRegistry = ctx.topicIdentityRegistry();

        // Evaluate EL expressions at connection time
        var evaluatedValue = ctx.getTemplateEngine().evalNow(configuration.getSomeEl(), String.class);

        // Register custom topics
        // topicIdentityRegistry.put(new TopicIdentity(topicId, topicName, TopicIdentity.Source.CLIENT));
    });
}
```

## Template: Policy with Response Action Registration

For policies that need to apply transformations on the response after processing the request:

```java
@Override
public Completable onRequest(KafkaExecutionContext ctx) {
    return Completable.fromRunnable(() -> {
        // Do request-phase work...

        // Register a callback for the response phase
        ctx.addActionOnResponse(responseCtx ->
            Completable.fromRunnable(() -> {
                // This runs during the response phase
                // responseCtx.response().delegate() gives access to the raw response
            })
        );
    });
}
```

## Important Notes

- The constructor receives the configuration instance. The gateway instantiates the policy via reflection using this single-arg constructor.
- The `id()` method must return the same value as the `id` field in `plugin.properties`.
- Always use `Completable.defer(() -> ...)` to ensure lazy evaluation - the Completable is built during chain construction but must not execute side effects until subscription.
- Use `@Slf4j` (Lombok) for logging. Log at `debug` level for operational messages, `trace` for verbose output.
- Access the Kafka ApiKey via `ctx.apiKey()` to branch logic per operation type (PRODUCE, FETCH, METADATA, CREATE_TOPICS, etc.).
