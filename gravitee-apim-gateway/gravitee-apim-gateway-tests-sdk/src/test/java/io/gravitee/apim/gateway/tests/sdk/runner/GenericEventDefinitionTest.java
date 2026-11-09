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
package io.gravitee.apim.gateway.tests.sdk.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.api.genericevent.GenericEvent;
import java.time.Instant;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.PreconditionViolationException;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GenericEventDefinitionTest {

    private static final String RESOURCE = "/generic-events/event.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_default_the_environment_the_organization_and_the_update_date() throws Exception {
        Instant before = Instant.now();

        GenericEvent event = read("{\"kind\":\"feature.rule\",\"id\":\"rule-1\",\"payload\":{\"enabled\":true}}").toGenericEvent(RESOURCE);

        assertThat(event.kind()).isEqualTo("feature.rule");
        assertThat(event.id()).isEqualTo("rule-1");
        assertThat(event.environmentId()).isEqualTo("DEFAULT");
        assertThat(event.organizationId()).isEqualTo("DEFAULT");
        assertThat(event.expiresAt()).isNull();
        assertThat(event.updatedAt()).isBetween(before, Instant.now());
    }

    @Test
    void should_carry_the_payload_as_a_json_string() throws Exception {
        GenericEvent event = read(
            "{\"kind\":\"feature.rule\",\"id\":\"rule-1\",\"payload\":{\"enabled\":true,\"tags\":[\"a\"]}}"
        ).toGenericEvent(RESOURCE);

        assertThat(objectMapper.readTree(event.payload())).isEqualTo(objectMapper.readTree("{\"enabled\":true,\"tags\":[\"a\"]}"));
    }

    @Test
    void should_keep_the_declared_environment_organization_and_expiration() throws Exception {
        GenericEvent event = read(
            "{\"kind\":\"feature.rule\",\"id\":\"rule-1\",\"environmentId\":\"env\",\"organizationId\":\"org\",\"expiresAt\":\"2030-01-01T00:00:00Z\",\"payload\":{}}"
        ).toGenericEvent(RESOURCE);

        assertThat(event.environmentId()).isEqualTo("env");
        assertThat(event.organizationId()).isEqualTo("org");
        assertThat(event.expiresAt()).isEqualTo(Instant.parse("2030-01-01T00:00:00Z"));
    }

    @Test
    void should_carry_a_payload_given_as_a_json_string_as_is() throws Exception {
        GenericEvent event = read("{\"kind\":\"feature.rule\",\"id\":\"rule-1\",\"payload\":\"{\\\"enabled\\\":true}\"}").toGenericEvent(
            RESOURCE
        );

        assertThat(event.payload()).isEqualTo("{\"enabled\":true}");
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "{\"kind\":\"feature.rule\",\"id\":\"rule-1\"}",
            "{\"kind\":\"feature.rule\",\"id\":\"rule-1\",\"payload\":null}",
            "{\"id\":\"rule-1\",\"payload\":{}}",
            "{\"kind\":\"feature.rule\",\"payload\":{}}",
        }
    )
    void should_fail_naming_the_resource_when_the_kind_the_id_or_the_payload_is_missing(String json) throws Exception {
        GenericEventDefinition definition = read(json);

        assertThatThrownBy(() -> definition.toGenericEvent(RESOURCE))
            .isInstanceOf(PreconditionViolationException.class)
            .hasMessageContaining(RESOURCE);
    }

    @Test
    void should_fail_naming_the_resource_when_the_expiration_is_not_an_instant() throws Exception {
        GenericEventDefinition definition = read(
            "{\"kind\":\"feature.rule\",\"id\":\"rule-1\",\"expiresAt\":\"2030-01-01\",\"payload\":{}}"
        );

        assertThatThrownBy(() -> definition.toGenericEvent(RESOURCE))
            .isInstanceOf(PreconditionViolationException.class)
            .hasMessage("The generic event resource " + RESOURCE + " has an invalid expiresAt");
    }

    private GenericEventDefinition read(String json) throws Exception {
        return objectMapper.readValue(json, GenericEventDefinition.class);
    }
}
