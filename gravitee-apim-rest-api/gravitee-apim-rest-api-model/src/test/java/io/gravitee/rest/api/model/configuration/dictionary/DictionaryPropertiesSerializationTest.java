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
package io.gravitee.rest.api.model.configuration.dictionary;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DictionaryPropertiesSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_deserialize_a_payload_without_property_options() throws Exception {
        String json =
            """
            {
              "name": "My Dictionary",
              "type": "MANUAL",
              "properties": { "hostname": "api.example.com" }
            }
            """;

        UpdateDictionaryEntity entity = objectMapper.readValue(json, UpdateDictionaryEntity.class);

        assertThat(entity.getProperties()).containsExactlyEntriesOf(Map.of("hostname", "api.example.com"));
        assertThat(entity.getPropertyOptions()).isNull();
    }

    @Test
    void should_deserialize_property_options() throws Exception {
        String json =
            """
            {
              "name": "My Dictionary",
              "type": "MANUAL",
              "properties": { "url": "https://backend", "apiKey": "cipher==", "renewed": "plaintext" },
              "propertyOptions": {
                "apiKey": { "encrypted": true, "encryptable": false },
                "renewed": { "encryptable": true }
              }
            }
            """;

        UpdateDictionaryEntity entity = objectMapper.readValue(json, UpdateDictionaryEntity.class);

        assertThat(entity.getPropertyOptions()).containsExactlyInAnyOrderEntriesOf(
            Map.of(
                "apiKey",
                new DictionaryPropertyOptions(true, false),
                "renewed",
                new DictionaryPropertyOptions(null, true)
            )
        );
        assertThat(entity.getPropertyOptions().get("apiKey").getEncrypted()).isTrue();
    }

    @Test
    void should_read_an_omitted_flag_as_null_rather_than_false() throws Exception {
        String json =
            """
            {
              "name": "My Dictionary",
              "type": "MANUAL",
              "properties": { "apiKey": "cipher==" },
              "propertyOptions": { "apiKey": { } }
            }
            """;

        UpdateDictionaryEntity entity = objectMapper.readValue(json, UpdateDictionaryEntity.class);

        assertThat(entity.getPropertyOptions().get("apiKey").getEncrypted()).isNull();
        assertThat(entity.getPropertyOptions().get("apiKey").getEncryptable()).isNull();
    }

    @Test
    void should_serialize_properties_and_options_side_by_side() throws Exception {
        DictionaryEntity entity = DictionaryEntity.builder()
            .id("dictionary-id")
            .name("My Dictionary")
            .properties(Map.of("apiKey", "••••••••••••"))
            .propertyOptions(Map.of("apiKey", DictionaryPropertyOptions.builder().encrypted(true).build()))
            .build();

        String json = objectMapper.writeValueAsString(entity);

        assertThat(json).contains("\"properties\":{\"apiKey\":").contains("\"propertyOptions\":{\"apiKey\":{\"encrypted\":true");
    }
}
