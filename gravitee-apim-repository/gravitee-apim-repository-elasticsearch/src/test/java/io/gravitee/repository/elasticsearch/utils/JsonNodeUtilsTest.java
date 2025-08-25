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
package io.gravitee.repository.elasticsearch.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class JsonNodeUtilsTest {

    @Test
    void should_get_field_as_text() {
        final JsonNode jsonNode = JsonNodeFactory.instance.objectNode().put("key", "value");
        assertThat(JsonNodeUtils.asTextOrNull(jsonNode.get("key"))).isEqualTo("value");
    }

    @Test
    void should_get_null_text() {
        final JsonNode jsonNode = JsonNodeFactory.instance.objectNode().put("key", "value");
        assertThat(JsonNodeUtils.asTextOrNull(jsonNode.get("absent-key"))).isNull();
    }

    @Test
    void should_get_field_as_int() {
        final JsonNode jsonNode = JsonNodeFactory.instance.objectNode().put("key", 1);
        assertThat(JsonNodeUtils.asIntOr(jsonNode.get("key"), -1)).isOne();
    }

    @Test
    void should_get_default_int() {
        final JsonNode jsonNode = JsonNodeFactory.instance.objectNode().put("key", 1);
        assertThat(JsonNodeUtils.asIntOr(jsonNode.get("absent-key"), -1)).isEqualTo(-1);
    }

    @Test
    void should_get_field_as_boolean() {
        final JsonNode jsonNode = JsonNodeFactory.instance.objectNode().put("key", true);
        assertThat(JsonNodeUtils.asBooleanOrFalse(jsonNode.get("key"))).isTrue();
    }

    @Test
    void should_get_false() {
        final JsonNode jsonNode = JsonNodeFactory.instance.objectNode().put("key", true);
        assertThat(JsonNodeUtils.asBooleanOrFalse(jsonNode.get("absent-key"))).isFalse();
    }

    @Test
    void should_get_field_as_long() {
        final JsonNode jsonNode = JsonNodeFactory.instance.objectNode().put("key", 1);
        assertThat(JsonNodeUtils.asLongOr(jsonNode.get("key"), -1)).isOne();
    }

    @Test
    void should_get_default_long() {
        final JsonNode jsonNode = JsonNodeFactory.instance.objectNode().put("key", 1);
        assertThat(JsonNodeUtils.asLongOr(jsonNode.get("absent-key"), -1)).isEqualTo(-1);
    }
}
