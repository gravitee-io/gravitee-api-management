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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import fixtures.core.model.GraviteeDefinitionFixtures;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

class GraviteeDefinitionJacksonJsonSerializerTest {

    private final GraviteeDefinitionJacksonJsonSerializer serializer = new GraviteeDefinitionJacksonJsonSerializer();

    @Test
    @SneakyThrows
    void should_serialize_a_proxy_gravitee_definition() {
        // Given
        var importDefinition = GraviteeDefinitionFixtures.aGraviteeDefinitionProxy();

        // When
        var result = serializer.serialize(importDefinition);

        // Then
        assertThatJson(result).isEqualTo(
            IOUtils.toString(new FileInputStream("src/test/resources/export/export_proxy.json"), StandardCharsets.UTF_8)
        );
    }

    @Test
    @SneakyThrows
    void should_serialize_channel_selector_operations_as_uppercase() {
        // Given
        var messageDefinition = GraviteeDefinitionFixtures.aGraviteeDefinitionMessage();

        // When
        var result = serializer.serialize(messageDefinition);

        // Then - operations must be uppercase to match REST API model's OperationsEnum.fromValue()
        assertThatJson(result).inPath("$.api.flows[0].selectors[0].operations").isArray().containsExactlyInAnyOrder("SUBSCRIBE", "PUBLISH");
        assertThatJson(result).inPath("$.api.flows[0].selectors[0].type").isEqualTo("CHANNEL");
    }

    @Test
    @SneakyThrows
    void should_serialize_sampling_type_as_uppercase() {
        // Given
        var messageDefinition = GraviteeDefinitionFixtures.aGraviteeDefinitionMessage();

        // When
        var result = serializer.serialize(messageDefinition);

        // Then - sampling type must be uppercase to match REST API model's TypeEnum.fromValue()
        assertThatJson(result).inPath("$.api.analytics.messageSampling.type").isEqualTo("PROBABILITY");
    }

    @Test
    @SneakyThrows
    void should_serialize_message_api_type_as_uppercase() {
        // Given
        var messageDefinition = GraviteeDefinitionFixtures.aGraviteeDefinitionMessage();

        // When
        var result = serializer.serialize(messageDefinition);

        // Then
        assertThatJson(result).inPath("$.api.type").isEqualTo("MESSAGE");
    }
}
