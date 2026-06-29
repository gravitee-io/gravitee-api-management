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
package io.gravitee.gateway.services.sync.process.local.synchronizer;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LocalApiSynchronizerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void folds_pure_json_payload_and_nested_definition_into_strings() throws Exception {
        JsonNode root = mapper.readTree(
            """
            { "apiEvent": { "id": "x", "type": "PUBLISH_API",
              "payload": { "id": "x", "type": "agent",
                "definition": { "kind": "standalone", "id": "x" } } } }
            """
        );

        JsonNode folded = LocalApiSynchronizer.foldEmbeddedJson(mapper, root);

        // payload is now an (escaped) JSON string, as the repository Event.payload (String) expects
        JsonNode payloadNode = folded.path("apiEvent").path("payload");
        assertThat(payloadNode.isTextual()).isTrue();

        // ... and the nested definition inside it is also a string (repository Api.definition is String)
        JsonNode payload = mapper.readTree(payloadNode.asText());
        assertThat(payload.path("definition").isTextual()).isTrue();
        assertThat(mapper.readTree(payload.path("definition").asText()).path("kind").asText()).isEqualTo("standalone");
    }

    @Test
    void leaves_a_legacy_escaped_string_payload_untouched() throws Exception {
        String legacy = "{\"apiEvent\":{\"id\":\"x\",\"type\":\"PUBLISH_API\",\"payload\":\"{\\\"id\\\":\\\"x\\\"}\"}}";
        JsonNode root = mapper.readTree(legacy);

        JsonNode folded = LocalApiSynchronizer.foldEmbeddedJson(mapper, root);

        assertThat(folded.path("apiEvent").path("payload").asText()).isEqualTo("{\"id\":\"x\"}");
    }
}
