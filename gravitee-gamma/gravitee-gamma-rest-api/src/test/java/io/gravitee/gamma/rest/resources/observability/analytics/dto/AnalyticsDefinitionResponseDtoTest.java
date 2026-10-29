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
package io.gravitee.gamma.rest.resources.observability.analytics.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.gravitee.gamma.rest.core.observability.analytics.model.CatalogOrigin;
import io.gravitee.gamma.rest.core.observability.analytics.model.MetricFamily;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** The module generates no model from its OpenAPI: this keeps the hand-written response and the documented one alike. */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AnalyticsDefinitionResponseDtoTest {

    private static JsonNode schemas;

    @BeforeAll
    static void loadOpenApi() throws IOException {
        try (var spec = AnalyticsDefinitionResponseDtoTest.class.getResourceAsStream("/openapi/observability/openapi-observability.yaml")) {
            schemas = new YAMLMapper().readTree(spec).at("/components/schemas");
        }
    }

    @ParameterizedTest
    @CsvSource(
        {
            "AnalyticsDefinitionResponse, io.gravitee.gamma.rest.resources.observability.analytics.dto.AnalyticsDefinitionResponseDto",
            "MetricFamily, io.gravitee.gamma.rest.resources.observability.analytics.dto.AnalyticsDefinitionResponseDto$MetricFamilyDto",
            "MetricDefinition, io.gravitee.gamma.rest.resources.observability.analytics.dto.AnalyticsDefinitionResponseDto$MetricDefinitionDto",
            "DimensionDefinition, io.gravitee.gamma.rest.resources.observability.analytics.dto.AnalyticsDefinitionResponseDto$DimensionDefinitionDto",
        }
    )
    void should_carry_exactly_the_documented_properties_all_of_them_required(String schemaName, Class<? extends Record> dto) {
        var schema = schemas.get(schemaName);
        var components = Arrays.stream(dto.getRecordComponents())
            .map(component -> component.getName())
            .toList();

        assertThat(fieldNames(schema.get("properties"))).containsExactlyElementsOf(components);
        assertThat(texts(schema.get("required"))).containsExactlyInAnyOrderElementsOf(components);
    }

    @Test
    void should_document_every_family_in_display_order() {
        assertThat(texts(schemas.at("/MetricFamily/properties/name/enum"))).containsExactlyElementsOf(
            Arrays.stream(MetricFamily.values()).map(Enum::name).toList()
        );
    }

    @ParameterizedTest
    @CsvSource({ "MetricDefinition", "DimensionDefinition" })
    void should_document_every_origin_an_entry_is_served_with(String schemaName) {
        assertThat(texts(schemas.at("/" + schemaName + "/properties/origin/enum"))).containsAll(
            Arrays.stream(CatalogOrigin.values()).map(Enum::name).toList()
        );
    }

    private static List<String> fieldNames(JsonNode node) {
        return StreamSupport.stream(((Iterable<String>) node::fieldNames).spliterator(), false).toList();
    }

    private static List<String> texts(JsonNode array) {
        return StreamSupport.stream(array.spliterator(), false).map(JsonNode::asText).toList();
    }
}
