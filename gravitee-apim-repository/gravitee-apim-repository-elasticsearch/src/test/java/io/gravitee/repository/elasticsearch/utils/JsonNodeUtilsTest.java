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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

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

    @Test
    void should_get_map() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode inner = mapper.createObjectNode();
        inner.put("keyword_mcp-proxy_method", "initialize");
        assertThat(JsonNodeUtils.asMapOrNull(inner)).isEqualTo(Map.of("keyword_mcp-proxy_method", "initialize"));
    }

    @Test
    void should_get_map_null() {
        assertThat(JsonNodeUtils.asMapOrNull(null)).isEqualTo(null);
    }

    @Test
    void should_get_map_incorrect_input() {
        final JsonNode jsonNode = JsonNodeFactory.instance.objectNode().put("key", 1);
        assertThat(JsonNodeUtils.asMapOrNull(jsonNode.get("key"))).isEqualTo(null);
    }

    @Nested
    class AsFlatTextMapOrNull {

        private final ObjectMapper mapper = new ObjectMapper();

        @Test
        void should_keep_a_dotted_key_as_one_key() throws Exception {
            var custom = mapper.readTree("{ \"customer.tier\": \"gold\", \"region\": \"eu-west\" }");

            assertThat(JsonNodeUtils.asFlatTextMapOrNull(custom, "req-1")).containsExactlyInAnyOrderEntriesOf(
                Map.of("customer.tier", "gold", "region", "eu-west")
            );
        }

        @Test
        void should_join_a_nested_object_back_into_its_dotted_key() throws Exception {
            var custom = mapper.readTree("{ \"order\": { \"channel\": { \"name\": \"web\" } }, \"region\": \"eu-west\" }");

            assertThat(JsonNodeUtils.asFlatTextMapOrNull(custom, "req-1")).containsExactlyInAnyOrderEntriesOf(
                Map.of("order.channel.name", "web", "region", "eu-west")
            );
        }

        @Test
        void should_keep_the_document_order_of_the_names() throws Exception {
            var custom = mapper.readTree("{ \"zone\": \"z\", \"order\": { \"channel\": \"web\" }, \"apple\": \"a\", \"mid\": \"m\" }");

            assertThat(JsonNodeUtils.asFlatTextMapOrNull(custom, "req-1").keySet()).containsExactly(
                "zone",
                "order.channel",
                "apple",
                "mid"
            );
        }

        @Test
        void should_answer_an_empty_map_for_an_empty_object() throws Exception {
            assertThat(JsonNodeUtils.asFlatTextMapOrNull(mapper.readTree("{}"), "req-1")).isNotNull().isEmpty();
        }

        @Test
        void should_log_the_null_values_and_the_names_written_twice_at_debug_level_with_the_document_id() throws Exception {
            var logger = (Logger) LoggerFactory.getLogger(JsonNodeUtils.class);
            var logs = new ListAppender<ILoggingEvent>();
            var previousLevel = logger.getLevel();
            logs.start();
            logger.addAppender(logs);
            logger.setLevel(Level.DEBUG);
            try {
                JsonNodeUtils.asFlatTextMapOrNull(mapper.readTree("{ \"empty\": null, \"a.b\": \"x\", \"a\": { \"b\": \"y\" } }"), "req-1");
            } finally {
                logger.detachAppender(logs);
                logger.setLevel(previousLevel);
            }

            assertThat(logs.list)
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
                    assertThat(event.getFormattedMessage()).contains("req-1").contains("[empty, a.b]");
                });
        }

        @Test
        void should_answer_null_when_the_field_is_absent_or_not_an_object() {
            assertThat(JsonNodeUtils.asFlatTextMapOrNull(null, "req-1")).isNull();
            assertThat(JsonNodeUtils.asFlatTextMapOrNull(JsonNodeFactory.instance.textNode("gold"), "req-1")).isNull();
            assertThat(JsonNodeUtils.asFlatTextMapOrNull(JsonNodeFactory.instance.nullNode(), "req-1")).isNull();
        }
    }

    @Nested
    class PutFlattened {

        private final ObjectMapper mapper = new ObjectMapper();

        @Test
        void should_read_a_number_or_a_boolean_as_its_text() throws Exception {
            var flat = new HashMap<String, String>();

            JsonNodeUtils.putFlattened(mapper.readTree("{ \"attempts\": 3, \"vip\": true }"), "", flat);

            assertThat(flat).containsExactlyInAnyOrderEntriesOf(Map.of("attempts", "3", "vip", "true"));
        }

        @Test
        void should_read_an_array_as_an_empty_text() throws Exception {
            var flat = new HashMap<String, String>();

            JsonNodeUtils.putFlattened(mapper.readTree("{ \"tags\": [\"a\", \"b\"] }"), "", flat);

            assertThat(flat).containsExactly(Map.entry("tags", ""));
        }

        @Test
        void should_name_the_members_under_the_prefix() throws Exception {
            var flat = new HashMap<String, String>();

            JsonNodeUtils.putFlattened(mapper.readTree("{ \"http\": { \"status_code\": 200 } }"), "attributes", flat);

            assertThat(flat).containsExactly(Map.entry("attributes.http.status_code", "200"));
        }

        @Test
        void should_report_a_skipped_null_value_as_left_out() throws Exception {
            var flat = new HashMap<String, String>();

            var leftOut = JsonNodeUtils.putFlattened(mapper.readTree("{ \"empty\": null, \"region\": \"eu-west\" }"), "", flat);

            assertThat(flat).containsExactly(Map.entry("region", "eu-west"));
            assertThat(leftOut).containsExactly("empty");
        }

        @Test
        void should_keep_the_last_value_of_a_name_written_twice_and_report_it_as_left_out() throws Exception {
            var flat = new HashMap<String, String>();

            var leftOut = JsonNodeUtils.putFlattened(mapper.readTree("{ \"a.b\": \"x\", \"a\": { \"b\": \"y\" } }"), "", flat);

            assertThat(flat).containsExactly(Map.entry("a.b", "y"));
            assertThat(leftOut).containsExactly("a.b");
        }
    }
}
