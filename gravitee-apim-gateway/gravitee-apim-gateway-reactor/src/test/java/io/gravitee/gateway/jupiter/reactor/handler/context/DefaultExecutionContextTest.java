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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import io.gravitee.definition.model.Api;
import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.jupiter.api.context.ContextAttributes;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.core.context.MutableRequest;
import io.gravitee.gateway.jupiter.core.context.MutableResponse;
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

    protected ExecutionContext cut;

    @Mock
    protected MutableRequest request;

    @Mock
    protected MutableResponse response;

    @Mock
    protected Api api;

    @BeforeEach
    public void init() {
        cut = new DefaultExecutionContext(request, response);
    }

    @Test
    public void shouldPutAndGetAttributes() {
        for (int i = 0; i < 10; i++) {
            cut.putAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        for (int i = 0; i < 10; i++) {
            assertEquals(ATTRIBUTE_VALUE + i, cut.getAttribute(ATTRIBUTE_KEY + i));
        }
    }

    @Test
    public void shouldGetAllAttributes() {
        for (int i = 0; i < 10; i++) {
            cut.putAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        final Map<String, Object> attributes = cut.getAttributes();

        for (int i = 0; i < 10; i++) {
            assertEquals(ATTRIBUTE_VALUE + i, attributes.get(ATTRIBUTE_KEY + i));
        }
    }

    @Test
    public void shouldGetAllPrefixedAttributes() {
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
    public void shouldRemoveAttributes() {
        for (int i = 0; i < 10; i++) {
            cut.putAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        for (int i = 0; i < 10; i++) {
            cut.removeAttribute(ATTRIBUTE_KEY + i);
            assertNull(cut.getAttribute(ATTRIBUTE_KEY + i));
        }
    }

    @Test
    public void shouldGetCastAttributes() {
        cut.putAttribute(ATTRIBUTE_KEY, 1.0f);
        assertEquals(1.0f, (float) cut.getAttribute(ATTRIBUTE_KEY));

        cut.putAttribute(ATTRIBUTE_KEY, ATTRIBUTE_VALUE);
        assertEquals(ATTRIBUTE_VALUE, cut.getAttribute(ATTRIBUTE_KEY));

        final Object object = mock(Object.class);
        cut.putAttribute(ATTRIBUTE_KEY, object);
        assertEquals(object, cut.getAttribute(ATTRIBUTE_KEY));
    }

    @Test
    public void shouldReturnClassCastExceptionWhenInvalidCastAttribute() {
        cut.putAttribute(ATTRIBUTE_KEY, ATTRIBUTE_VALUE);

        assertThrows(
            ClassCastException.class,
            () -> {
                final Float value = cut.getAttribute(ATTRIBUTE_KEY);
            }
        );
    }

    @Test
    public void shouldReturnNullWhenGetUnknownAttribute() {
        assertNull(cut.getAttribute(ATTRIBUTE_KEY));
    }

    @Test
    public void shouldPutAndGetInternalAttributes() {
        for (int i = 0; i < 10; i++) {
            cut.putInternalAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        for (int i = 0; i < 10; i++) {
            assertEquals(ATTRIBUTE_VALUE + i, cut.getInternalAttribute(ATTRIBUTE_KEY + i));
        }
    }

    @Test
    public void shouldGetAllInternalAttributes() {
        for (int i = 0; i < 10; i++) {
            cut.putInternalAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        final Map<String, Object> internalAttributes = cut.getInternalAttributes();

        for (int i = 0; i < 10; i++) {
            assertEquals(ATTRIBUTE_VALUE + i, internalAttributes.get(ATTRIBUTE_KEY + i));
        }
    }

    @Test
    public void shouldRemoveInternalAttributes() {
        for (int i = 0; i < 10; i++) {
            cut.putInternalAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        for (int i = 0; i < 10; i++) {
            cut.removeInternalAttribute(ATTRIBUTE_KEY + i);
            assertNull(cut.getInternalAttribute(ATTRIBUTE_KEY + i));
        }
    }

    @Test
    public void shouldGetCastInternalAttributes() {
        cut.putInternalAttribute(ATTRIBUTE_KEY, 1.0f);
        assertEquals(1.0f, (float) cut.getInternalAttribute(ATTRIBUTE_KEY));

        cut.putInternalAttribute(ATTRIBUTE_KEY, ATTRIBUTE_VALUE);
        assertEquals(ATTRIBUTE_VALUE, cut.getInternalAttribute(ATTRIBUTE_KEY));

        final Object object = mock(Object.class);
        cut.putInternalAttribute(ATTRIBUTE_KEY, object);
        assertEquals(object, cut.getInternalAttribute(ATTRIBUTE_KEY));
    }

    @Test
    public void shouldReturnClassCastExceptionWhenInvalidCastInternalAttribute() {
        cut.putInternalAttribute(ATTRIBUTE_KEY, ATTRIBUTE_VALUE);

        assertThrows(
            ClassCastException.class,
            () -> {
                final Float value = cut.getInternalAttribute(ATTRIBUTE_KEY);
            }
        );
    }

    @Test
    public void shouldReturnNullWhenGetUnknownInternalAttribute() {
        assertNull(cut.getInternalAttribute(ATTRIBUTE_KEY));
    }

    @Test
    public void shouldPopulateTemplateContextWithVariables() {
        final TemplateEngine templateEngine = cut.getTemplateEngine();
        final TemplateContext templateContext = templateEngine.getTemplateContext();

        assertNotNull(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST));
        assertNotNull(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_RESPONSE));
        assertNotNull(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_CONTEXT));
    }

    @Test
    public void shouldInitializeTemplateEngineOnlyOnce() {
        final TemplateEngine templateEngine = cut.getTemplateEngine();

        for (int i = 0; i < 10; i++) {
            assertEquals(templateEngine, cut.getTemplateEngine());
        }
    }
}
