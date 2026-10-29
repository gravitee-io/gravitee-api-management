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
package io.gravitee.gamma.rest.resource.observability.filters;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricValueType;
import io.gravitee.gamma.rest.core.observability.catalog.model.CatalogOrigin;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.resources.observability.filters.dto.FilterSpecDto;
import io.gravitee.gamma.rest.resources.observability.filters.dto.FilterSpecsResponseDto;
import io.gravitee.gamma.rest.resources.observability.metricdefinitions.dto.DeclareMetricDefinitionRequestDto;
import io.gravitee.gamma.rest.resources.observability.metricdefinitions.dto.MetricDefinitionDto;
import io.gravitee.gamma.rest.resources.observability.metricdefinitions.dto.MetricDefinitionEntryDto;
import io.gravitee.gamma.rest.resources.observability.metricdefinitions.dto.MetricDefinitionsResponseDto;
import io.gravitee.gamma.rest.resources.observability.metricdefinitions.dto.SaveMetricDefinitionRequestDto;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The module hand-writes its DTOs, so nothing but this test ties their JSON shape to the published
 * OpenAPI schemas.
 */
class FilterCatalogSchemaTest {

    private static final String SPEC = "/openapi/observability/openapi-observability.yaml";
    private static final String SCHEMA_REF_PREFIX = "#/components/schemas/";
    private static final ObjectMapper JSON = new ObjectMapper();

    private static JsonNode schemas;

    @BeforeAll
    static void loadSpec() throws IOException {
        try (InputStream spec = FilterCatalogSchemaTest.class.getResourceAsStream(SPEC)) {
            // A schema declared twice would otherwise be read as its last declaration, the other one silently dropped.
            var yaml = new ObjectMapper(new YAMLFactory()).enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
            schemas = yaml.readTree(spec).path("components").path("schemas");
        }
    }

    @Test
    void should_serialize_a_filter_spec_with_the_properties_of_its_schema_catalog_flags_included() {
        assertThat(propertyNames("FilterSpec")).contains("origin", "rawKey", "valueType", "lastSeen", "exposed");
        assertThat(serializedNames(FilterSpecDto.class)).containsExactlyInAnyOrderElementsOf(propertyNames("FilterSpec"));
    }

    @Test
    void should_serialize_the_filter_catalog_response_with_the_properties_of_its_schema() {
        assertThat(serializedNames(FilterSpecsResponseDto.class)).containsExactlyInAnyOrderElementsOf(propertyNames("FilterSpecsResponse"));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("metricDefinitionSchemas")
    void should_serialize_the_metric_definitions_with_the_properties_of_their_schemas(Class<?> dto, String schemaName) {
        assertThat(serializedNames(dto)).containsExactlyInAnyOrderElementsOf(propertyNames(schemaName));
    }

    static List<Arguments> metricDefinitionSchemas() {
        return List.of(
            Arguments.of(MetricDefinitionEntryDto.class, "MetricDefinitionEntry"),
            Arguments.of(MetricDefinitionsResponseDto.class, "MetricDefinitionsResponse"),
            Arguments.of(MetricDefinitionDto.class, "MetricDefinitionRecord"),
            Arguments.of(SaveMetricDefinitionRequestDto.class, "SaveMetricDefinitionRequest"),
            Arguments.of(DeclareMetricDefinitionRequestDto.class, "DeclareMetricDefinitionRequest")
        );
    }

    @Test
    void should_publish_every_operator_the_requests_accept() {
        assertThat(publishedEnum("FilterOperator")).containsExactlyInAnyOrderElementsOf(names(FilterOperator.values()));
    }

    @Test
    void should_publish_every_catalog_origin_and_value_type_the_catalog_serves() {
        assertThat(publishedEnum("CatalogOrigin")).containsExactlyInAnyOrderElementsOf(names(CatalogOrigin.values()));
        assertThat(publishedEnum("AdditionalMetricValueType")).containsExactlyInAnyOrderElementsOf(
            names(AdditionalMetricValueType.values())
        );
    }

    private static List<String> publishedEnum(String schemaName) {
        return StreamSupport.stream(schemas.path(schemaName).path("enum").spliterator(), false).map(JsonNode::asText).toList();
    }

    private static List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }

    private static Set<String> propertyNames(String schemaName) {
        JsonNode schema = schemas.path(schemaName);
        assertThat(schema.isObject()).as("schema %s is defined", schemaName).isTrue();
        return propertyNames(schema);
    }

    private static Set<String> propertyNames(JsonNode schema) {
        Set<String> names = new HashSet<>();
        schema.path("properties").fieldNames().forEachRemaining(names::add);
        for (JsonNode composed : schema.path("allOf")) {
            if (composed.has("$ref")) {
                String ref = composed.path("$ref").asText();
                assertThat(ref).as("allOf member reference").startsWith(SCHEMA_REF_PREFIX);
                names.addAll(propertyNames(ref.substring(SCHEMA_REF_PREFIX.length())));
            } else {
                names.addAll(propertyNames(composed));
            }
        }
        return names;
    }

    private static Set<String> serializedNames(Class<?> type) {
        return JSON.getSerializationConfig()
            .introspect(JSON.constructType(type))
            .findProperties()
            .stream()
            .map(BeanPropertyDefinition::getName)
            .collect(Collectors.toSet());
    }
}
