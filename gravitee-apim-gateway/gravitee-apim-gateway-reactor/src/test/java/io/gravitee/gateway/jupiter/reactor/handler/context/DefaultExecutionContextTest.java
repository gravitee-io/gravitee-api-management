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
package io.gravitee.gateway.jupiter.reactor.handler.context;

import static io.gravitee.gateway.jupiter.api.context.HttpExecutionContext.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.gravitee.definition.model.Api;
import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateEngine;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.context.ContextAttributes;
import io.gravitee.gateway.jupiter.api.context.InternalContextAttributes;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.core.context.MutableExecutionContext;
import io.gravitee.gateway.jupiter.core.context.MutableRequest;
import io.gravitee.gateway.jupiter.core.context.MutableResponse;
import io.gravitee.gateway.jupiter.core.context.interruption.InterruptionException;
import io.gravitee.gateway.jupiter.core.context.interruption.InterruptionFailureException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
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
    void shouldPutAndGetAttributes() {
        for (int i = 0; i < 10; i++) {
            cut.putAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        for (int i = 0; i < 10; i++) {
            assertEquals(ATTRIBUTE_VALUE + i, cut.getAttribute(ATTRIBUTE_KEY + i));
        }
    }

    @Test
    void shouldGetAllAttributes() {
        for (int i = 0; i < 10; i++) {
            cut.putAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        final Map<String, Object> attributes = cut.getAttributes();

        for (int i = 0; i < 10; i++) {
            assertEquals(ATTRIBUTE_VALUE + i, attributes.get(ATTRIBUTE_KEY + i));
        }
    }

    @Test
    void shouldGetAllPrefixedAttributes() {
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
    void shouldRemoveAttributes() {
        for (int i = 0; i < 10; i++) {
            cut.putAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        for (int i = 0; i < 10; i++) {
            cut.removeAttribute(ATTRIBUTE_KEY + i);
            assertNull(cut.getAttribute(ATTRIBUTE_KEY + i));
        }
    }

    @Test
    void shouldGetCastAttributes() {
        cut.putAttribute(ATTRIBUTE_KEY, 1.0f);
        assertEquals(1.0f, (float) cut.getAttribute(ATTRIBUTE_KEY));

        cut.putAttribute(ATTRIBUTE_KEY, ATTRIBUTE_VALUE);
        assertEquals(ATTRIBUTE_VALUE, cut.getAttribute(ATTRIBUTE_KEY));

        final Object object = mock(Object.class);
        cut.putAttribute(ATTRIBUTE_KEY, object);
        assertEquals(object, cut.getAttribute(ATTRIBUTE_KEY));
    }

    @Test
    void shouldReturnClassCastExceptionWhenInvalidCastAttribute() {
        cut.putAttribute(ATTRIBUTE_KEY, ATTRIBUTE_VALUE);

        assertThrows(
            ClassCastException.class,
            () -> {
                final Float value = cut.getAttribute(ATTRIBUTE_KEY);
            }
        );
    }

    @Test
    void shouldReturnNullWhenGetUnknownAttribute() {
        assertNull(cut.getAttribute(ATTRIBUTE_KEY));
    }

    @Test
    void shouldPutAndGetInternalAttributes() {
        for (int i = 0; i < 10; i++) {
            cut.putInternalAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        for (int i = 0; i < 10; i++) {
            assertEquals(ATTRIBUTE_VALUE + i, cut.getInternalAttribute(ATTRIBUTE_KEY + i));
        }
    }

    @Test
    void shouldGetAllInternalAttributes() {
        for (int i = 0; i < 10; i++) {
            cut.putInternalAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        final Map<String, Object> internalAttributes = cut.getInternalAttributes();

        for (int i = 0; i < 10; i++) {
            assertEquals(ATTRIBUTE_VALUE + i, internalAttributes.get(ATTRIBUTE_KEY + i));
        }
    }

    @Test
    void shouldRemoveInternalAttributes() {
        for (int i = 0; i < 10; i++) {
            cut.putInternalAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        for (int i = 0; i < 10; i++) {
            cut.removeInternalAttribute(ATTRIBUTE_KEY + i);
            assertNull(cut.getInternalAttribute(ATTRIBUTE_KEY + i));
        }
    }

    @Test
    void shouldGetCastInternalAttributes() {
        cut.putInternalAttribute(ATTRIBUTE_KEY, 1.0f);
        assertEquals(1.0f, (float) cut.getInternalAttribute(ATTRIBUTE_KEY));

        cut.putInternalAttribute(ATTRIBUTE_KEY, ATTRIBUTE_VALUE);
        assertEquals(ATTRIBUTE_VALUE, cut.getInternalAttribute(ATTRIBUTE_KEY));

        final Object object = mock(Object.class);
        cut.putInternalAttribute(ATTRIBUTE_KEY, object);
        assertEquals(object, cut.getInternalAttribute(ATTRIBUTE_KEY));
    }

    @Test
    void shouldReturnClassCastExceptionWhenInvalidCastInternalAttribute() {
        cut.putInternalAttribute(ATTRIBUTE_KEY, ATTRIBUTE_VALUE);

        assertThrows(
            ClassCastException.class,
            () -> {
                final Float value = cut.getInternalAttribute(ATTRIBUTE_KEY);
            }
        );
    }

    @Test
    void shouldReturnNullWhenGetUnknownInternalAttribute() {
        assertNull(cut.getInternalAttribute(ATTRIBUTE_KEY));
    }

    @Test
    void shouldPopulateTemplateContextWithVariables() {
        final TemplateEngine templateEngine = cut.getTemplateEngine();
        final TemplateContext templateContext = templateEngine.getTemplateContext();

        assertNotNull(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST));
        assertNotNull(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_RESPONSE));
        assertNotNull(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_CONTEXT));
    }

    @Test
    void shouldInitializeTemplateEngineOnlyOnce() {
        final TemplateEngine templateEngine = cut.getTemplateEngine();

        for (int i = 0; i < 10; i++) {
            assertEquals(templateEngine, cut.getTemplateEngine());
        }
    }

    @Test
    void shouldInterruptWithInterruptionException() {
        cut.interrupt().test().assertError(InterruptionException.class);
    }

    @Test
    void shouldInterruptWithInterruptionFailureException() {
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
    void shouldInterruptBodyWithInterruptionException() {
        cut.interruptBody().test().assertError(InterruptionException.class);
    }

    @Test
    void shouldInterruptBodyWithInterruptionFailureException() {
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
    void shouldInterruptMessagesWithInterruptionException() {
        cut.interruptMessages().test().assertError(InterruptionException.class);
    }

    @Test
    void shouldInterruptMessagesWithInterruptionFailureException() {
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
    void shouldInterruptMessageWithInterruptionException() {
        cut.interruptMessage().test().assertError(InterruptionException.class);
    }

    @Test
    void shouldInterruptMessageWithInterruptionFailureException() {
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
    void shouldCreateTemplateEngineOnce() {
        final TemplateEngine templateEngine = cut.getTemplateEngine();

        for (int i = 0; i < 10; i++) {
            assertSame(templateEngine, cut.getTemplateEngine());
        }
    }

    @Test
    void shouldCreateTemplateEnginePerMessage() {
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
    void shouldProvideTemplateVariablesWhenProvidersAreSpecified() {
        final TemplateVariableProvider templateVariableProvider = mock(TemplateVariableProvider.class);
        cut.templateVariableProviders(List.of(templateVariableProvider));

        cut.getTemplateEngine();

        verify(templateVariableProvider).provide(cut);
    }
}
