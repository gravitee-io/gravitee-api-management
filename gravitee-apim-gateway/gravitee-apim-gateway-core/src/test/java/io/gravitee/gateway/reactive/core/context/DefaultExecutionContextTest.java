/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.reactive.core.context;

import static io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext.TEMPLATE_ATTRIBUTE_CONTEXT;
import static io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext.TEMPLATE_ATTRIBUTE_REQUEST;
import static io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext.TEMPLATE_ATTRIBUTE_RESPONSE;
import static java.lang.Double.parseDouble;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.gravitee.definition.model.Api;
import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ExecutionWarn;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionException;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionFailureException;
import io.gravitee.reporter.api.v4.metric.Metrics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DefaultExecutionContextTest {

    protected static final String ATTRIBUTE_KEY = "key";
    protected static final String ATTRIBUTE_VALUE = "value";

    protected MutableExecutionContext cut;

    @Mock
    protected MutableRequest request;

    @Mock
    protected MutableResponse response;

    @Mock
    protected Api api;

    @Mock
    protected Metrics metrics;

    @BeforeEach
    void init() {
        cut = new DefaultExecutionContext(request, response);
        cut.metrics(metrics);
    }

    @Test
    void should_put_and_get_attributes() {
        for (int i = 0; i < 10; i++) {
            cut.putAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        for (int i = 0; i < 10; i++) {
            assertThat(cut.getAttribute(ATTRIBUTE_KEY + i).toString()).isEqualTo(ATTRIBUTE_VALUE + i);
        }
    }

    @Test
    void should_get_all_attributes() {
        for (int i = 0; i < 10; i++) {
            cut.putAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        final Map<String, Object> attributes = cut.getAttributes();

        for (int i = 0; i < 10; i++) {
            assertThat(attributes).containsEntry(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }
    }

    @Test
    void should_get_all_prefixed_attributes() {
        for (int i = 0; i < 10; i++) {
            // Put attribute with prefix.
            cut.putAttribute(ContextAttributes.ATTR_PREFIX + ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        final var attributes = cut.getAttributes();

        for (int i = 0; i < 10; i++) {
            // Get attribute without prefix.
            assertThat(attributes).containsEntry(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }
    }

    @Test
    void should_remove_attributes() {
        for (int i = 0; i < 10; i++) {
            cut.putAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        for (int i = 0; i < 10; i++) {
            cut.removeAttribute(ATTRIBUTE_KEY + i);
            assertThat((Object) cut.getAttribute(ATTRIBUTE_KEY + i)).isNull();
        }
    }

    @Test
    void should_get_cast_attributes() {
        cut.putAttribute(ATTRIBUTE_KEY, 1.0f);
        assertThat((float) cut.getAttribute(ATTRIBUTE_KEY)).isEqualTo(1.0f);

        cut.putAttribute(ATTRIBUTE_KEY, ATTRIBUTE_VALUE);
        assertThat((String) cut.getAttribute(ATTRIBUTE_KEY)).isEqualTo(ATTRIBUTE_VALUE);

        final Object object = new Object();
        cut.putAttribute(ATTRIBUTE_KEY, object);
        assertThat((Object) cut.getAttribute(ATTRIBUTE_KEY)).isEqualTo(object);
    }

    @Test
    void should_return_class_cast_exception_when_invalid_cast_attribute() {
        cut.putAttribute(ATTRIBUTE_KEY, ATTRIBUTE_VALUE);

        assertThatThrownBy(() -> {
                final Float value = cut.getAttribute(ATTRIBUTE_KEY);
            })
            .isInstanceOf(ClassCastException.class);
    }

    @Test
    void should_return_null_when_get_unknown_attribute() {
        assertThat((Object) cut.getAttribute(ATTRIBUTE_KEY)).isNull();
    }

    @Test
    void should_put_and_get_internal_attributes() {
        for (int i = 0; i < 10; i++) {
            cut.putInternalAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        for (int i = 0; i < 10; i++) {
            assertThat(cut.getInternalAttribute(ATTRIBUTE_KEY + i).toString()).isEqualTo(ATTRIBUTE_VALUE + i);
        }
    }

    @Test
    void should_get_all_internal_attributes() {
        for (int i = 0; i < 10; i++) {
            cut.putInternalAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        final Map<String, Object> internalAttributes = cut.getInternalAttributes();

        for (int i = 0; i < 10; i++) {
            assertThat(internalAttributes).containsEntry(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }
    }

    @Test
    void should_remove_internal_attributes() {
        for (int i = 0; i < 10; i++) {
            cut.putInternalAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        for (int i = 0; i < 10; i++) {
            cut.removeInternalAttribute(ATTRIBUTE_KEY + i);
            assertThat((Object) cut.getAttribute(ATTRIBUTE_KEY + i)).isNull();
        }
    }

    @Test
    void should_get_cast_internal_attributes() {
        cut.putInternalAttribute(ATTRIBUTE_KEY, 1.0f);
        assertThat((float) cut.getInternalAttribute(ATTRIBUTE_KEY)).isEqualTo(1.0f);

        cut.putInternalAttribute(ATTRIBUTE_KEY, ATTRIBUTE_VALUE);
        assertThat(cut.getInternalAttribute(ATTRIBUTE_KEY).toString()).isEqualTo(ATTRIBUTE_VALUE);

        final Object object = new Object();
        cut.putInternalAttribute(ATTRIBUTE_KEY, object);
        assertThat((Object) cut.getInternalAttribute(ATTRIBUTE_KEY)).isEqualTo(object);
    }

    @Test
    void should_return_class_cast_exception_when_invalid_cast_internal_attribute() {
        cut.putInternalAttribute(ATTRIBUTE_KEY, ATTRIBUTE_VALUE);

        assertThatThrownBy(() -> {
                final Float value = cut.getInternalAttribute(ATTRIBUTE_KEY);
            })
            .isInstanceOf(ClassCastException.class);
    }

    @Test
    void should_return_null_when_get_unknown_internal_attribute() {
        assertThat((Object) cut.getAttribute(ATTRIBUTE_KEY)).isNull();
    }

    @Test
    void should_populate_template_context_with_variables() {
        final TemplateEngine templateEngine = cut.getTemplateEngine();
        final TemplateContext templateContext = templateEngine.getTemplateContext();

        Stream
            .of(TEMPLATE_ATTRIBUTE_REQUEST, TEMPLATE_ATTRIBUTE_RESPONSE, TEMPLATE_ATTRIBUTE_RESPONSE)
            .forEach(attribute -> assertThat(templateContext.lookupVariable(attribute)).isNotNull());
    }

    @Test
    void should_initialize_template_engine_only_once() {
        final TemplateEngine templateEngine = cut.getTemplateEngine();

        for (int i = 0; i < 10; i++) {
            assertThat(cut.getTemplateEngine()).isEqualTo(templateEngine);
        }
    }

    @Test
    void should_interrupt_with_interruption_exception() {
        cut.interrupt().test().assertError(InterruptionException.class);
    }

    @Test
    void should_interrupt_with_interruption_failure_exception() {
        cut
            .interruptWith(new ExecutionFailure(404))
            .test()
            .assertError(throwable -> {
                assertThat(throwable).isInstanceOf(InterruptionFailureException.class);
                InterruptionFailureException failureException = (InterruptionFailureException) throwable;
                assertThat(failureException.getExecutionFailure().statusCode()).isEqualTo(404);
                ExecutionFailure executionFailure = cut.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE);
                assertThat(executionFailure.statusCode()).isEqualTo(404);
                return true;
            });
    }

    @Test
    void should_interrupt_body_with_interruption_exception() {
        cut.interruptBody().test().assertError(InterruptionException.class);
    }

    @Test
    void should_interrupt_body_with_interruption_failure_exception() {
        cut
            .interruptBodyWith(new ExecutionFailure(404))
            .test()
            .assertError(throwable -> {
                assertThat(throwable).isInstanceOf(InterruptionFailureException.class);
                InterruptionFailureException failureException = (InterruptionFailureException) throwable;
                assertThat(failureException.getExecutionFailure().statusCode()).isEqualTo(404);
                ExecutionFailure executionFailure = cut.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE);
                assertThat(executionFailure.statusCode()).isEqualTo(404);
                return true;
            });
    }

    @Test
    void should_interrupt_messages_with_interruption_exception() {
        cut.interruptMessages().test().assertError(InterruptionException.class);
    }

    @Test
    void should_interrupt_messages_with_interruption_failure_exception() {
        cut
            .interruptMessagesWith(new ExecutionFailure(404))
            .test()
            .assertError(throwable -> {
                assertThat(throwable).isInstanceOf(InterruptionFailureException.class);
                InterruptionFailureException failureException = (InterruptionFailureException) throwable;
                assertThat(failureException.getExecutionFailure().statusCode()).isEqualTo(404);
                return true;
            });
    }

    @Test
    void should_interrupt_message_with_interruption_exception() {
        cut.interruptMessage().test().assertError(InterruptionException.class);
    }

    @Test
    void should_interrupt_message_with_interruption_failure_exception() {
        cut
            .interruptMessageWith(new ExecutionFailure(404))
            .test()
            .assertError(throwable -> {
                assertThat(throwable).isInstanceOf(InterruptionFailureException.class);
                InterruptionFailureException failureException = (InterruptionFailureException) throwable;
                assertThat(failureException.getExecutionFailure().statusCode()).isEqualTo(404);
                return true;
            });
    }

    @Test
    void should_create_template_engine_once() {
        final TemplateEngine templateEngine = cut.getTemplateEngine();

        for (int i = 0; i < 10; i++) {
            assertThat(cut.getTemplateEngine()).isSameAs(templateEngine);
        }
    }

    @Test
    void should_create_template_engine_per_message() {
        final TemplateEngine templateEngine = cut.getTemplateEngine(mock(Message.class));
        final TemplateContext templateContext = templateEngine.getTemplateContext();

        for (int i = 0; i < 10; i++) {
            final TemplateEngine otherTemplateEngine = cut.getTemplateEngine(mock(Message.class));
            final TemplateContext otherTemplateContext = otherTemplateEngine.getTemplateContext();

            // Template engine and template context are per message.
            assertThat(templateEngine).isNotSameAs(otherTemplateEngine);
            assertThat(templateContext).isNotSameAs(otherTemplateContext);

            // But evaluable request/response/context are common.
            Stream
                .of(TEMPLATE_ATTRIBUTE_REQUEST, TEMPLATE_ATTRIBUTE_RESPONSE, TEMPLATE_ATTRIBUTE_CONTEXT)
                .forEach(key -> assertThat(otherTemplateContext.lookupVariable(key)).isSameAs(otherTemplateContext.lookupVariable(key)));
        }
    }

    @Test
    void should_provide_template_variables_when_providers_are_specified() {
        final var templateVariableProvider = mock(ExecutionContextTemplateVariableProvider.class);
        cut.templateVariableProviders(List.of(templateVariableProvider));

        cut.getTemplateEngine();

        verify(templateVariableProvider).provide(cut);
    }

    @Test
    void should_fetch_null_attribute_as_list() {
        cut.putAttribute(ATTRIBUTE_KEY, null);
        assertThat(cut.getAttributeAsList(ATTRIBUTE_KEY)).isNull();
        assertThat(cut.getAttributeAsList("foo")).isNull();
    }

    static Stream<Arguments> listableAttribute() {
        return Stream.of(
            arguments("a,b,c", List.of("a", "b", "c")),
            arguments("a , b, c ", List.of("a", "b", "c")),
            arguments(" a , b, c ", List.of("a", "b", "c")),
            arguments("a   ,   b,    c    ", List.of("a", "b", "c")),
            arguments("a\t,\tb,\tc\t", List.of("a", "b", "c")),
            arguments("a\t\t,\t\tb,\t\tc\t\t", List.of("a", "b", "c")),
            arguments("a b c", List.of("a b c")),
            arguments(" a b c ", List.of("a b c")),
            arguments("[\"a\", \"b\", \"c\"]", List.of("a", "b", "c")),
            arguments("[\"a\", {}, \"c\"]", List.of("a", "{}", "c")),
            arguments("[\"a\", 1, \"c\"]", List.of("a", "1", "c")),
            arguments("[\"a\", 123456789123456789123456789123456789, \"c\"]", List.of("a", "123456789123456789123456789123456789", "c")),
            arguments("[\"a\", true, \"c\"]", List.of("a", "true", "c")),
            arguments(List.of("a", "b", "c"), List.of("a", "b", "c")),
            arguments(new ArrayList<>(List.of(1, 2, 3)), List.of(1, 2, 3)),
            arguments(Collections.emptyList(), Collections.emptyList()),
            arguments(1, List.of(1))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("listableAttribute")
    void should_fetch_attribute_as_list(Object attributeValue, List<Object> expectedList) {
        cut.putAttribute(ATTRIBUTE_KEY, attributeValue);
        List<Object> actual = cut.getAttributeAsList(ATTRIBUTE_KEY);
        assertThat(actual).containsAll(expectedList);
        assertThat(actual).isNotSameAs(expectedList);
        assertThatCode(() -> actual.add(new Object())).isOfAnyClassIn(UnsupportedOperationException.class);
    }

    /**
     * Use a dedicated test to use specific floating point assertions methods.
     * The previous test was broken since JDK19 because of <a href="https://bugs.openjdk.org/browse/JDK-4511638">JDK-4511638</a>
     * (cf: <a href="https://inside.java/2022/09/23/quality-heads-up/">Quality Outreach Heads-up - JDK 19 - Double.toString() and Float.toString() changes</a>)
     */
    @Test
    void should_fetch_attribute_as_list_float_with_great_precision() {
        // Given
        String attributeValue = "[\"a\", 123456789123456789.123456789123456789, \"c\"]";

        // When
        cut.putAttribute(ATTRIBUTE_KEY, attributeValue);
        List<Object> actual = cut.getAttributeAsList(ATTRIBUTE_KEY);

        // Then
        var expectedList = List.of("a", "1.23456789123456784E17", "c");
        assertThat(actual).isNotSameAs(expectedList);
        assertThat(actual).hasSize(3);
        assertThat(actual.get(0)).isEqualTo("a");
        assertThat(actual.get(2)).isEqualTo("c");
        assertThat(parseDouble(actual.get(1).toString()))
            .isEqualTo(123_456_789_123_456_789.123_456_789_123_456_789, offset(0.000_000_000_000_000_1d));
        assertThatCode(() -> actual.add(new Object())).isOfAnyClassIn(UnsupportedOperationException.class);
    }

    @Nested
    @DisplayName("Warning functionality")
    class WarningFunctionalityTests {

        @Test
        void should_add_warning_when_warnings_enabled() {
            // Given
            ExecutionWarn warning = new ExecutionWarn("WARNING_KEY").message("This is a warning");
            ((DefaultExecutionContext) cut).setWarningsEnabled(true);

            // When
            cut.warnWith(warning);

            // Then
            List<ExecutionWarn> warnings = cut.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_WARN);
            assertThat(warnings).isNotNull();
            assertThat(warnings).hasSize(1);
            assertThat(warnings.get(0)).isEqualTo(warning);
        }

        @Test
        void should_not_add_warning_when_warnings_disabled() {
            // Given
            ExecutionWarn warning = new ExecutionWarn("WARNING_KEY").message("This is a warning");
            ((DefaultExecutionContext) cut).setWarningsEnabled(false);

            // When
            cut.warnWith(warning);

            // Then
            List<ExecutionWarn> warnings = cut.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_WARN);
            assertThat(warnings).isNull();
        }

        @Test
        void should_add_multiple_warnings_when_warnings_enabled() {
            // Given
            ExecutionWarn warning1 = new ExecutionWarn("WARNING_KEY_1").message("First warning");
            ExecutionWarn warning2 = new ExecutionWarn("WARNING_KEY_2").message("Second warning");
            ((DefaultExecutionContext) cut).setWarningsEnabled(true);

            // When
            cut.warnWith(warning1);
            cut.warnWith(warning2);

            // Then
            List<ExecutionWarn> warnings = cut.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_WARN);
            assertThat(warnings).isNotNull();
            assertThat(warnings).hasSize(2);
            assertThat(warnings).containsExactly(warning1, warning2);
        }

        @Test
        void should_add_warning_with_cause() {
            // Given
            RuntimeException cause = new RuntimeException("Root cause");
            ExecutionWarn warning = new ExecutionWarn("WARNING_KEY").message("This is a warning").cause(cause);
            ((DefaultExecutionContext) cut).setWarningsEnabled(true);

            // When
            cut.warnWith(warning);

            // Then
            List<ExecutionWarn> warnings = cut.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_WARN);
            assertThat(warnings).isNotNull();
            assertThat(warnings).hasSize(1);
            assertThat(warnings.get(0).cause()).isEqualTo(cause);
        }

        @Test
        void should_default_warnings_enabled_to_true() {
            // Given
            ExecutionWarn warning = new ExecutionWarn("WARNING_KEY").message("This is a warning");

            // When
            cut.warnWith(warning);

            // Then
            List<ExecutionWarn> warnings = cut.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_WARN);
            assertThat(warnings).isNotNull();
            assertThat(warnings).hasSize(1);
        }
    }
}
