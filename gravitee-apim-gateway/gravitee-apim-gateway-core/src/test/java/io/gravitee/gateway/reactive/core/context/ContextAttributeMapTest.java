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

import static io.gravitee.gateway.reactive.api.context.ContextAttributes.ATTR_PREFIX;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ContextAttributeMapTest {

    @Nested
    class WithEnableGraviteePrefix {

        @Test
        void should_retrieve_attribute_with_prefix() {
            var contextAttributeMap = new ContextAttributeMap();
            contextAttributeMap.put(ATTR_PREFIX + "key", "value");

            assertEquals("value", contextAttributeMap.get("key"));
            assertTrue(contextAttributeMap.containsKey("key"));
            assertEquals("value", contextAttributeMap.get(ATTR_PREFIX + "key"));
            assertTrue(contextAttributeMap.containsKey(ATTR_PREFIX + "key"));
        }

        @Test
        void should_retrieve_attribute_with_prefix_from_fallback() {
            var fallbackContextAttributeMap = new ContextAttributeMap();
            fallbackContextAttributeMap.put(ATTR_PREFIX + "fallback", "fallbackValue");

            var contextAttributeMap = new ContextAttributeMap(fallbackContextAttributeMap);
            contextAttributeMap.put(ATTR_PREFIX + "key", "value");

            assertEquals("value", contextAttributeMap.get("key"));
            assertTrue(contextAttributeMap.containsKey("key"));
            assertEquals("value", contextAttributeMap.get(ATTR_PREFIX + "key"));
            assertTrue(contextAttributeMap.containsKey(ATTR_PREFIX + "key"));
            assertEquals("fallbackValue", contextAttributeMap.get("fallback"));
            assertTrue(contextAttributeMap.containsKey("fallback"));
            assertEquals("fallbackValue", contextAttributeMap.get(ATTR_PREFIX + "fallback"));
            assertTrue(contextAttributeMap.containsKey(ATTR_PREFIX + "fallback"));
        }
    }

    @Nested
    class WithoutEnableGraviteePrefix {

        @Test
        void should_not_retrieve_attribute_with_prefix() {
            ContextAttributeMap contextAttributeMap = new ContextAttributeMap(false);
            contextAttributeMap.put("key", "value");
            assertEquals("value", contextAttributeMap.get("key"));
            assertTrue(contextAttributeMap.containsKey("key"));
            assertNull(contextAttributeMap.get(ATTR_PREFIX + "key"));
            assertFalse(contextAttributeMap.containsKey(ATTR_PREFIX + "key"));
        }

        @Test
        void should_retrieve_attribute_with_prefix_from_fallback() {
            var fallbackContextAttributeMap = new ContextAttributeMap(false);
            fallbackContextAttributeMap.put("fallback", "fallbackValue");

            var contextAttributeMap = new ContextAttributeMap(fallbackContextAttributeMap, false);
            contextAttributeMap.put("key", "value");

            assertEquals("value", contextAttributeMap.get("key"));
            assertTrue(contextAttributeMap.containsKey("key"));
            assertNull(contextAttributeMap.get(ATTR_PREFIX + "key"));
            assertFalse(contextAttributeMap.containsKey(ATTR_PREFIX + "key"));
            assertEquals("fallbackValue", contextAttributeMap.get("fallback"));
            assertTrue(contextAttributeMap.containsKey("fallback"));
            assertNull(contextAttributeMap.get(ATTR_PREFIX + "fallback"));
            assertFalse(contextAttributeMap.containsKey(ATTR_PREFIX + "fallback"));
        }
    }

    @Test
    void should_getFallbackContextAttributeMap_when_fallback_is_not_null() {
        var fallbackContextAttributeMap = new ContextAttributeMap();
        fallbackContextAttributeMap.put(ATTR_PREFIX + "fallback", "fallbackValue");

        var contextAttributeMap = new ContextAttributeMap(fallbackContextAttributeMap);
        assertEquals(fallbackContextAttributeMap, contextAttributeMap.getFallbackContextAttributeMap());
    }

    @Test
    void should_getFallbackContextAttributeMap_when_fallback_is_null() {
        var contextAttributeMap = new ContextAttributeMap();
        assertNull(contextAttributeMap.getFallbackContextAttributeMap());
    }
}
