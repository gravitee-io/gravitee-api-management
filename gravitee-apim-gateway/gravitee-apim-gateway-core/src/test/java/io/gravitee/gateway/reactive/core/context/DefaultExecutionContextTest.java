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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.gravitee.definition.model.Api;
import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateEngine;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionException;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionFailureException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
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

    @BeforeEach
    void init() {
        cut = new DefaultExecutionContext(request, response);
    }

    @Test
    void should_put_and_get_attributes() {
        for (int i = 0; i < 10; i++) {
            cut.putAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        for (int i = 0; i < 10; i++) {
            assertEquals(ATTRIBUTE_VALUE + i, cut.getAttribute(ATTRIBUTE_KEY + i));
        }
    }

    @Test
    void should_get_all_attributes() {
        for (int i = 0; i < 10; i++) {
            cut.putAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        final Map<String, Object> attributes = cut.getAttributes();

        for (int i = 0; i < 10; i++) {
            assertEquals(ATTRIBUTE_VALUE + i, attributes.get(ATTRIBUTE_KEY + i));
        }
    }

    @Test
    void should_get_all_prefixed_attributes() {
        for (int i = 0; i < 10; i++) {
            // Put attribute with prefix.
            cut.putAttribute(ContextAttributes.ATTR_PREFIX + ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        final Map<String, Object> attributes = cut.getAttributes();

        for (int i = 0; i < 10; i++) {
            // Get attribute without prefix.
            assertEquals(ATTRIBUTE_VALUE + i, attributes.get(ATTRIBUTE_KEY + i));
        }
    }

    @Test
    void should_remove_attributes() {
        for (int i = 0; i < 10; i++) {
            cut.putAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        for (int i = 0; i < 10; i++) {
            cut.removeAttribute(ATTRIBUTE_KEY + i);
            assertNull(cut.getAttribute(ATTRIBUTE_KEY + i));
        }
    }

    @Test
    void should_get_cast_attributes() {
        cut.putAttribute(ATTRIBUTE_KEY, 1.0f);
        assertEquals(1.0f, (float) cut.getAttribute(ATTRIBUTE_KEY));

        cut.putAttribute(ATTRIBUTE_KEY, ATTRIBUTE_VALUE);
        assertEquals(ATTRIBUTE_VALUE, cut.getAttribute(ATTRIBUTE_KEY));

        final Object object = mock(Object.class);
        cut.putAttribute(ATTRIBUTE_KEY, object);
        assertEquals(object, cut.getAttribute(ATTRIBUTE_KEY));
    }

    @Test
    void should_return_class_cast_exception_when_invalid_cast_attribute() {
        cut.putAttribute(ATTRIBUTE_KEY, ATTRIBUTE_VALUE);

        assertThrows(
            ClassCastException.class,
            () -> {
                final Float value = cut.getAttribute(ATTRIBUTE_KEY);
            }
        );
    }

    @Test
    void should_return_null_when_get_unknown_attribute() {
        assertNull(cut.getAttribute(ATTRIBUTE_KEY));
    }

    @Test
    void should_put_and_get_internal_attributes() {
        for (int i = 0; i < 10; i++) {
            cut.putInternalAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        for (int i = 0; i < 10; i++) {
            assertEquals(ATTRIBUTE_VALUE + i, cut.getInternalAttribute(ATTRIBUTE_KEY + i));
        }
    }

    @Test
    void should_get_all_internal_attributes() {
        for (int i = 0; i < 10; i++) {
            cut.putInternalAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        final Map<String, Object> internalAttributes = cut.getInternalAttributes();

        for (int i = 0; i < 10; i++) {
            assertEquals(ATTRIBUTE_VALUE + i, internalAttributes.get(ATTRIBUTE_KEY + i));
        }
    }

    @Test
    void should_remove_internal_attributes() {
        for (int i = 0; i < 10; i++) {
            cut.putInternalAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        for (int i = 0; i < 10; i++) {
            cut.removeInternalAttribute(ATTRIBUTE_KEY + i);
            assertNull(cut.getInternalAttribute(ATTRIBUTE_KEY + i));
        }
    }

    @Test
    void should_get_cast_internal_attributes() {
        cut.putInternalAttribute(ATTRIBUTE_KEY, 1.0f);
        assertEquals(1.0f, (float) cut.getInternalAttribute(ATTRIBUTE_KEY));

        cut.putInternalAttribute(ATTRIBUTE_KEY, ATTRIBUTE_VALUE);
        assertEquals(ATTRIBUTE_VALUE, cut.getInternalAttribute(ATTRIBUTE_KEY));

        final Object object = mock(Object.class);
        cut.putInternalAttribute(ATTRIBUTE_KEY, object);
        assertEquals(object, cut.getInternalAttribute(ATTRIBUTE_KEY));
    }

    @Test
    void should_return_class_cast_exception_when_invalid_cast_internal_attribute() {
        cut.putInternalAttribute(ATTRIBUTE_KEY, ATTRIBUTE_VALUE);

        assertThrows(
            ClassCastException.class,
            () -> {
                final Float value = cut.getInternalAttribute(ATTRIBUTE_KEY);
            }
        );
    }

    @Test
    void should_return_null_when_get_unknown_internal_attribute() {
        assertNull(cut.getInternalAttribute(ATTRIBUTE_KEY));
    }

    @Test
    void should_populate_template_context_with_variables() {
        final TemplateEngine templateEngine = cut.getTemplateEngine();
        final TemplateContext templateContext = templateEngine.getTemplateContext();

        assertNotNull(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST));
        assertNotNull(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_RESPONSE));
        assertNotNull(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_CONTEXT));
    }

    @Test
    void should_initialize_template_engine_only_once() {
        final TemplateEngine templateEngine = cut.getTemplateEngine();

        for (int i = 0; i < 10; i++) {
            assertEquals(templateEngine, cut.getTemplateEngine());
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
            assertSame(templateEngine, cut.getTemplateEngine());
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
            assertNotSame(templateEngine, otherTemplateEngine);
            assertNotSame(templateContext, otherTemplateContext);

            // But evaluable request/response/context are common.
            assertSame(
                templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST),
                otherTemplateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST)
            );
            assertSame(
                templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_RESPONSE),
                otherTemplateContext.lookupVariable(TEMPLATE_ATTRIBUTE_RESPONSE)
            );
            assertSame(
                templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_CONTEXT),
                otherTemplateContext.lookupVariable(TEMPLATE_ATTRIBUTE_CONTEXT)
            );
        }
    }

    @Test
    void should_provide_template_variables_when_providers_are_specified() {
        final TemplateVariableProvider templateVariableProvider = mock(TemplateVariableProvider.class);
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
            Arguments.arguments("a,b,c", List.of("a", "b", "c")),
            Arguments.arguments("a , b, c ", List.of("a", "b", "c")),
            Arguments.arguments(" a , b, c ", List.of("a", "b", "c")),
            Arguments.arguments("a   ,   b,    c    ", List.of("a", "b", "c")),
            Arguments.arguments("a\t,\tb,\tc\t", List.of("a", "b", "c")),
            Arguments.arguments("a\t\t,\t\tb,\t\tc\t\t", List.of("a", "b", "c")),
            Arguments.arguments("a b c", List.of("a b c")),
            Arguments.arguments(" a b c ", List.of("a b c")),
            Arguments.arguments("[\"a\", \"b\", \"c\"]", List.of("a", "b", "c")),
            Arguments.arguments("[\"a\", {}, \"c\"]", List.of("a", "{}", "c")),
            Arguments.arguments("[\"a\", 1, \"c\"]", List.of("a", "1", "c")),
            Arguments.arguments(
                "[\"a\", 123456789123456789123456789123456789, \"c\"]",
                List.of("a", "123456789123456789123456789123456789", "c")
            ),
            Arguments.arguments("[\"a\", 123456789123456789.123456789123456789, \"c\"]", List.of("a", "1.23456789123456784E17", "c")),
            Arguments.arguments("[\"a\", true, \"c\"]", List.of("a", "true", "c")),
            Arguments.arguments(List.of("a", "b", "c"), List.of("a", "b", "c")),
            Arguments.arguments(new ArrayList<>(List.of(1, 2, 3)), List.of(1, 2, 3)),
            Arguments.arguments(Collections.emptyList(), Collections.emptyList()),
            Arguments.arguments(1, List.of(1))
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
}
