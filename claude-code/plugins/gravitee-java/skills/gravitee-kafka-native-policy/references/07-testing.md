# Testing Patterns

## Framework & Dependencies

- **JUnit 5** (`junit-jupiter-api`, `junit-jupiter-params`)
- **Mockito** (`mockito-core`, `mockito-junit-jupiter`)
- **AssertJ** (`assertj-core`) for fluent assertions
- **json-unit-assertj** for JSON assertions

## Test Class Structure

```java
package com.graviteesource.policy.kafka.<policy_package>;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.graviteesource.policy.kafka.<policy_package>.configuration.<PolicyName>PolicyConfiguration;
import io.gravitee.gateway.reactive.api.context.kafka.KafkaExecutionContext;
import io.gravitee.gateway.reactive.api.context.kafka.KafkaMessageExecutionContext;
import io.gravitee.gateway.reactive.api.context.kafka.KafkaMessageResponse;
import io.gravitee.gateway.reactive.api.message.kafka.KafkaMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class <PolicyName>PolicyTest {

    @Nested
    class NoMocksTests {

        @Test
        void should_return_policy_id() {
            var policy = new <PolicyName>Policy(new <PolicyName>PolicyConfiguration());
            assertThat(policy.id()).isEqualTo("kafka-<policy-id>");
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class OnRequestTests {

        @Mock
        private KafkaExecutionContext ctx;

        private <PolicyName>Policy policy;

        @BeforeEach
        void setUp() {
            var configuration = new <PolicyName>PolicyConfiguration();
            // Configure as needed
            policy = new <PolicyName>Policy(configuration);
        }

        @Test
        void should_complete_when_request_is_valid() {
            // Given
            // when(ctx.apiKey()).thenReturn(ApiKeys.PRODUCE);

            // When
            var testObserver = policy.onRequest(ctx).test();

            // Then
            testObserver.assertComplete();
            testObserver.assertNoErrors();
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class OnMessageResponseTests {

        @Mock
        private KafkaMessageExecutionContext ctx;

        @Mock
        private KafkaMessageResponse response;

        // ...
    }
}
```

## Conventions

- **Class annotation**: `@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)` - converts `should_do_something` to "should do something" in test output.
- **Method naming**: `should_<expected_behavior>[_when_<condition>]` using underscores.
- **Nested classes**: Group tests by phase (`OnRequestTests`, `OnMessageResponseTests`) or by behavior (`NoMocksTests`, `WithMocksTests`).
- **Pattern**: Given/When/Then sections in each test method.

## Testing Message-Level Policies

For policies using `onMessageRequest` / `onMessageResponse` with `ctx.response().onMessages(...)`:

```java
@Test
void should_filter_messages_matching_condition() {
    // Given
    when(ctx.response()).thenReturn(response);

    ArgumentCaptor<FlowableTransformer<KafkaMessage, KafkaMessage>> transformerCaptor =
        ArgumentCaptor.forClass(FlowableTransformer.class);

    // When
    policy.onMessageResponse(ctx).subscribe();

    // Then
    verify(response).onMessages(transformerCaptor.capture());

    // Simulate message processing through the captured transformer
    Flowable<KafkaMessage> output = Flowable
        .just(message1, message2, message3)
        .compose(transformerCaptor.getValue());

    // Verify output
    output.test()
        .assertValueCount(2)
        .assertValues(message1, message3);
}
```

## Testing Interrupt (Error) Behavior

```java
@Test
void should_interrupt_with_error_when_unauthorized() {
    // Given
    when(ctx.interruptWith(any(Errors.class))).thenReturn(Completable.complete());

    // When
    policy.onRequest(ctx).test().assertComplete();

    // Then
    verify(ctx).interruptWith(any(Errors.class));
}
```

## Logback Test Configuration

Create `src/test/resources/logback-test.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
```
