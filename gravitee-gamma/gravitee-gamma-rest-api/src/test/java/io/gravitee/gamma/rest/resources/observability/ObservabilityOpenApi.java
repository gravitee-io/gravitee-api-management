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
package io.gravitee.gamma.rest.resources.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The published observability OpenAPI, read by the tests that tie the hand-written DTOs to it: the module generates no
 * model from the spec, so nothing else keeps the two alike.
 */
public final class ObservabilityOpenApi {

    private static final String SPEC = "/openapi/observability/openapi-observability.yaml";
    private static final String SCHEMA_REF_PREFIX = "#/components/schemas/";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final JsonNode SCHEMAS = loadSchemas();

    private ObservabilityOpenApi() {}

    public static JsonNode schemas() {
        return SCHEMAS;
    }

    /** The properties a schema documents, those of the schemas it composes with {@code allOf} included. */
    public static Set<String> propertyNames(String schemaName) {
        JsonNode schema = SCHEMAS.path(schemaName);
        assertThat(schema.isObject()).as("schema %s is defined", schemaName).isTrue();
        return propertyNames(schema);
    }

    /** The names Jackson writes, in declaration order: what the wire carries, not the record's component names. */
    public static List<String> serializedNames(Class<?> type) {
        return JSON.getSerializationConfig()
            .introspect(JSON.constructType(type))
            .findProperties()
            .stream()
            .map(BeanPropertyDefinition::getName)
            .toList();
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

    private static JsonNode loadSchemas() {
        try (InputStream spec = ObservabilityOpenApi.class.getResourceAsStream(SPEC)) {
            // A schema declared twice would otherwise be read as its last declaration, the other one silently dropped.
            var yaml = new ObjectMapper(new YAMLFactory()).enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
            return yaml.readTree(spec).path("components").path("schemas");
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read the observability OpenAPI " + SPEC, e);
        }
    }
}
