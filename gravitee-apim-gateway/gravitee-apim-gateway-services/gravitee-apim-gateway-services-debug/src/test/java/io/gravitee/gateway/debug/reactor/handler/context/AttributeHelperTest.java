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
package io.gravitee.gateway.debug.reactor.handler.context;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gateway.core.invoker.EndpointInvoker;
import io.gravitee.gateway.reactor.handler.VirtualHost;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class AttributeHelperTest {

    static class SimpleSerializableClass implements Serializable {

        private final String name = "name";

        @Override
        public String toString() {
            return "{" + "name='" + name + '\'' + '}';
        }
    }

    @Test
    public void shouldKeepSerializableValues() {
        SimpleSerializableClass serializableClassInstance = new SimpleSerializableClass();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("gravitee.attribute.string", "a value");
        attributes.put("gravitee.attribute.number", 42);
        attributes.put("gravitee.attribute.boolean", true);
        attributes.put("gravitee.attribute.class", serializableClassInstance);

        Map<String, Serializable> expected = new HashMap<>();
        expected.put("gravitee.attribute.string", "a value");
        expected.put("gravitee.attribute.number", 42);
        expected.put("gravitee.attribute.boolean", true);
        expected.put("gravitee.attribute.class", serializableClassInstance);
        assertThat(AttributeHelper.filterAndSerializeAttributes(attributes)).isEqualTo(expected);
    }

    @Test
    public void shouldRemoveNonSerializableValues() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("gravitee.attribute.invoker", new EndpointInvoker(null));
        attributes.put("gravitee.attribute.entrypoint", new VirtualHost(""));
        attributes.put("gravitee.attribute.path", "a value");

        assertThat(AttributeHelper.filterAndSerializeAttributes(attributes)).isEqualTo(Map.of("gravitee.attribute.path", "a value"));
    }

    @Test
    public void shouldRemoveNullValues() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("gravitee.attribute.application", null);
        attributes.put("gravitee.attribute.path", "a value");

        assertThat(AttributeHelper.filterAndSerializeAttributes(attributes)).isEqualTo(Map.of("gravitee.attribute.path", "a value"));
    }

    @Test
    public void shouldRemoveContextPath() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("gravitee.attribute.context-path", "a value");
        attributes.put("gravitee.attribute.path", "a value");

        assertThat(AttributeHelper.filterAndSerializeAttributes(attributes))
            .isEqualTo(Map.of("gravitee.attribute.context-path", "a value", "gravitee.attribute.path", "a value"));
    }

    @Test
    public void shouldReturnNullIfInputIsNull() {
        assertThat(AttributeHelper.filterAndSerializeAttributes(null)).isNull();
    }
}
