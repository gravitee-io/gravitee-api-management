/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.jupiter.reactor.handler.context;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
abstract class AbstractExecutionContextTest {

    protected static final String ATTRIBUTE_KEY = "key";
    protected static final String ATTRIBUTE_VALUE = "value";

    protected ExecutionContext executionContext;

    @Test
    public void shouldPutAndGetAttributes() {
        for (int i = 0; i < 10; i++) {
            executionContext.putAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        for (int i = 0; i < 10; i++) {
            assertEquals(ATTRIBUTE_VALUE + i, executionContext.getAttribute(ATTRIBUTE_KEY + i));
        }
    }

    @Test
    public void shouldGetAllAttributes() {
        for (int i = 0; i < 10; i++) {
            executionContext.putAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        final Map<String, Object> attributes = executionContext.getAttributes();

        for (int i = 0; i < 10; i++) {
            assertEquals(ATTRIBUTE_VALUE + i, attributes.get(ATTRIBUTE_KEY + i));
        }
    }

    @Test
    public void shouldRemoveAttributes() {
        for (int i = 0; i < 10; i++) {
            executionContext.putAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        for (int i = 0; i < 10; i++) {
            executionContext.removeAttribute(ATTRIBUTE_KEY + i);
            assertNull(executionContext.getAttribute(ATTRIBUTE_KEY + i));
        }
    }

    @Test
    public void shouldGetCastAttributes() {
        executionContext.putAttribute(ATTRIBUTE_KEY, 1.0f);
        assertEquals(1.0f, (float) executionContext.getAttribute(ATTRIBUTE_KEY));

        executionContext.putAttribute(ATTRIBUTE_KEY, ATTRIBUTE_VALUE);
        assertEquals(ATTRIBUTE_VALUE, executionContext.getAttribute(ATTRIBUTE_KEY));

        final Object object = mock(Object.class);
        executionContext.putAttribute(ATTRIBUTE_KEY, object);
        assertEquals(object, executionContext.getAttribute(ATTRIBUTE_KEY));
    }

    @Test
    public void shouldReturnClassCastExceptionWhenInvalidCastAttribute() {
        executionContext.putAttribute(ATTRIBUTE_KEY, ATTRIBUTE_VALUE);

        assertThrows(
            ClassCastException.class,
            () -> {
                final Float value = executionContext.getAttribute(ATTRIBUTE_KEY);
            }
        );
    }

    @Test
    public void shouldReturnNullWhenGetUnknownAttribute() {
        assertNull(executionContext.getAttribute(ATTRIBUTE_KEY));
    }

    @Test
    public void shouldPutAndGetInternalAttributes() {
        for (int i = 0; i < 10; i++) {
            executionContext.putInternalAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        for (int i = 0; i < 10; i++) {
            assertEquals(ATTRIBUTE_VALUE + i, executionContext.getInternalAttribute(ATTRIBUTE_KEY + i));
        }
    }

    @Test
    public void shouldGetAllInternalAttributes() {
        for (int i = 0; i < 10; i++) {
            executionContext.putInternalAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        final Map<String, Object> internalAttributes = executionContext.getInternalAttributes();

        for (int i = 0; i < 10; i++) {
            assertEquals(ATTRIBUTE_VALUE + i, internalAttributes.get(ATTRIBUTE_KEY + i));
        }
    }

    @Test
    public void shouldRemoveInternalAttributes() {
        for (int i = 0; i < 10; i++) {
            executionContext.putInternalAttribute(ATTRIBUTE_KEY + i, ATTRIBUTE_VALUE + i);
        }

        for (int i = 0; i < 10; i++) {
            executionContext.removeInternalAttribute(ATTRIBUTE_KEY + i);
            assertNull(executionContext.getInternalAttribute(ATTRIBUTE_KEY + i));
        }
    }

    @Test
    public void shouldGetCastInternalAttributes() {
        executionContext.putInternalAttribute(ATTRIBUTE_KEY, 1.0f);
        assertEquals(1.0f, (float) executionContext.getInternalAttribute(ATTRIBUTE_KEY));

        executionContext.putInternalAttribute(ATTRIBUTE_KEY, ATTRIBUTE_VALUE);
        assertEquals(ATTRIBUTE_VALUE, executionContext.getInternalAttribute(ATTRIBUTE_KEY));

        final Object object = mock(Object.class);
        executionContext.putInternalAttribute(ATTRIBUTE_KEY, object);
        assertEquals(object, executionContext.getInternalAttribute(ATTRIBUTE_KEY));
    }

    @Test
    public void shouldReturnClassCastExceptionWhenInvalidCastInternalAttribute() {
        executionContext.putInternalAttribute(ATTRIBUTE_KEY, ATTRIBUTE_VALUE);

        assertThrows(
            ClassCastException.class,
            () -> {
                final Float value = executionContext.getInternalAttribute(ATTRIBUTE_KEY);
            }
        );
    }

    @Test
    public void shouldReturnNullWhenGetUnknownInternalAttribute() {
        assertNull(executionContext.getInternalAttribute(ATTRIBUTE_KEY));
    }
}
