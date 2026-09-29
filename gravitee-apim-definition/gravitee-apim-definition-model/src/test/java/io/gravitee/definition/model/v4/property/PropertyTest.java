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
package io.gravitee.definition.model.v4.property;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PropertyTest {

    // Mirrors GraviteeMapper, which parses stored definitions: unknown fields must not break deserialization.
    private final ObjectMapper objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    void should_deserialize_a_property_stored_before_encrypted_and_dynamic_existed() throws Exception {
        var read = objectMapper.readValue(
            """
            { "key": "my-key", "value": "my-value" }
            """,
            Property.class
        );

        assertThat(read.isEncrypted()).isFalse();
        assertThat(read.isDynamic()).isFalse();
        assertThat(read.getValue()).isEqualTo("my-value");
    }

    @Test
    void should_tolerate_an_unknown_field_the_same_way_the_production_mapper_does() throws Exception {
        var read = objectMapper.readValue(
            """
            { "key": "my-key", "value": "my-value", "someFieldFromANewerVersion": "ignored" }
            """,
            Property.class
        );

        assertThat(read.getKey()).isEqualTo("my-key");
        assertThat(read.getValue()).isEqualTo("my-value");
    }

    @ParameterizedTest
    @CsvSource({ "false,false", "false,true", "true,false", "true,true" })
    void should_round_trip_encrypted_and_dynamic(boolean encrypted, boolean dynamic) throws Exception {
        var property = new Property("my-key", "my-value", encrypted, dynamic);

        var read = objectMapper.readValue(objectMapper.writeValueAsString(property), Property.class);

        assertThat(read).isEqualTo(property);
    }
}
