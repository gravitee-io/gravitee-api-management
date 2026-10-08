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
package io.gravitee.apim.infra.json.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class EncryptedPropertyAuditRedactionTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void should_remove_the_value_of_an_encrypted_property_at_any_depth() {
        var node = json(
            """
            {"definition":{"properties":[{"key":"secret","value":"CIPHER","encrypted":true,"dynamic":false},{"key":"plain","value":"plain-value","encrypted":false}]}}"""
        );

        assertThat(EncryptedPropertyAuditRedaction.redact(node)).isEqualTo(
            json(
                """
                {"definition":{"properties":[{"key":"secret","encrypted":true,"dynamic":false},{"key":"plain","value":"plain-value","encrypted":false}]}}"""
            )
        );
    }

    @Test
    void should_remove_the_value_of_an_encrypted_property_in_a_top_level_array() {
        var node = json(
            """
            [{"key":"secret","value":"CIPHER","encrypted":true}]"""
        );

        assertThat(EncryptedPropertyAuditRedaction.redact(node)).isEqualTo(
            json(
                """
                [{"key":"secret","encrypted":true}]"""
            )
        );
    }

    @Test
    void should_remove_the_value_when_encrypted_is_written_as_text() {
        var node = json(
            """
            {"key":"secret","value":"CIPHER","encrypted":"true"}"""
        );

        assertThat(EncryptedPropertyAuditRedaction.redact(node)).isEqualTo(
            json(
                """
                {"key":"secret","encrypted":"true"}"""
            )
        );
    }

    @Test
    void should_keep_the_value_of_a_node_without_key() {
        var node = json(
            """
            {"properties":{"secret":{"value":"CIPHER","encrypted":true}}}"""
        );

        JsonNode redacted = EncryptedPropertyAuditRedaction.redact(node.deepCopy());

        assertThat(redacted).isEqualTo(node);
    }

    @Test
    void should_keep_a_tree_without_encrypted_property_unchanged() {
        var node = json(
            """
            {"name":"my-api","tags":["a","b"],"properties":[{"key":"plain","value":"plain-value","encrypted":false}]}"""
        );

        JsonNode redacted = EncryptedPropertyAuditRedaction.redact(node.deepCopy());

        assertThat(redacted).isEqualTo(node);
    }

    private static JsonNode json(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(json, e);
        }
    }
}
