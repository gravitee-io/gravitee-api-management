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
package io.gravitee.repository.management.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class DictionaryPropertyTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void should_construct_with_value_and_encrypted() {
        DictionaryProperty property = new DictionaryProperty("secret", true);

        assertThat(property.value()).isEqualTo("secret");
        assertThat(property.encrypted()).isTrue();
    }

    @Test
    void should_deserialize_legacy_bare_string_as_unencrypted() throws Exception {
        DictionaryProperty property = mapper.readValue("\"localhost\"", DictionaryProperty.class);

        assertThat(property.value()).isEqualTo("localhost");
        assertThat(property.encrypted()).isFalse();
    }

    @Test
    void should_deserialize_typed_object() throws Exception {
        DictionaryProperty property = mapper.readValue("{\"value\":\"cipher\",\"encrypted\":true}", DictionaryProperty.class);

        assertThat(property.value()).isEqualTo("cipher");
        assertThat(property.encrypted()).isTrue();
    }

    @Test
    void should_serialize_an_unencrypted_value_as_a_bare_string() throws Exception {
        String json = mapper.writeValueAsString(new DictionaryProperty("x", false));

        assertThat(json).isEqualTo("\"x\"");
    }

    @Test
    void should_serialize_an_encrypted_value_as_the_typed_object() throws Exception {
        String json = mapper.writeValueAsString(new DictionaryProperty("cipher", true));

        assertThat(json).isEqualTo("{\"value\":\"cipher\",\"encrypted\":true}");
    }

    @Test
    void should_round_trip_an_unencrypted_value_through_serialize_then_deserialize() throws Exception {
        DictionaryProperty original = new DictionaryProperty("plain-value", false);

        DictionaryProperty roundTripped = mapper.readValue(mapper.writeValueAsString(original), DictionaryProperty.class);

        assertThat(roundTripped).isEqualTo(original);
    }
}
