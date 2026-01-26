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
package io.gravitee.rest.api.management.v2.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Extracts schemas from OpenAPI specifications and converts them to JSON Schema format.
 * Used to generate MCP resources for schema definitions.
 *
 * @author GraviteeSource Team
 */
@Component
@CustomLog
public class OpenApiSchemaExtractor {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * Extracts all schemas from an OpenAPI specification file.
     *
     * @param specPath the classpath path to the OpenAPI specification
     * @return map of schema name to JSON Schema content
     */
    public Map<String, String> extractSchemas(String specPath) {
        Map<String, String> schemas = new HashMap<>();

        try {
            URL specUrl = getClass().getClassLoader().getResource(specPath);
            if (specUrl == null) {
                log.warn("OpenAPI spec not found: {}", specPath);
                return schemas;
            }

            OpenAPI openApi = new OpenAPIV3Parser().read(specUrl.toString());
            if (openApi == null) {
                log.warn("Failed to parse OpenAPI spec: {}", specPath);
                return schemas;
            }

            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                log.debug("No schemas found in OpenAPI spec: {}", specPath);
                return schemas;
            }

            openApi.getComponents().getSchemas().forEach((schemaName, schema) -> {
                try {
                    String jsonSchema = convertToJsonSchema(schemaName, schema);
                    schemas.put(schemaName, jsonSchema);
                    log.debug("Extracted schema: {}", schemaName);
                } catch (Exception e) {
                    log.error("Failed to convert schema {}: {}", schemaName, e.getMessage());
                }
            });

            log.info("Extracted {} schemas from {}", schemas.size(), specPath);
        } catch (Exception e) {
            log.error("Error extracting schemas from {}: {}", specPath, e.getMessage(), e);
        }

        return schemas;
    }

    /**
     * Converts an OpenAPI Schema to JSON Schema format.
     *
     * @param schemaName the name of the schema
     * @param schema the OpenAPI schema object
     * @return JSON Schema as string
     */
    private String convertToJsonSchema(String schemaName, Schema<?> schema) throws IOException {
        Map<String, Object> jsonSchema = new LinkedHashMap<>();

        jsonSchema.put("$schema", "http://json-schema.org/draft-07/schema#");
        jsonSchema.put("title", schemaName);

        if (schema.getDescription() != null) {
            jsonSchema.put("description", schema.getDescription());
        }

        // Convert the OpenAPI schema to a plain object structure
        // The swagger-parser already provides a Map-like structure
        Map<String, Object> schemaMap = convertSchemaToMap(schema);
        jsonSchema.putAll(schemaMap);

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonSchema);
    }

    /**
     * Converts an OpenAPI Schema object to a Map representation.
     * This handles the conversion of complex OpenAPI schema structures to simple maps.
     *
     * @param schema the OpenAPI schema
     * @return map representation of the schema
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertSchemaToMap(Schema<?> schema) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (schema.getType() != null) {
            result.put("type", schema.getType());
        }

        if (schema.getFormat() != null) {
            result.put("format", schema.getFormat());
        }

        if (schema.getProperties() != null) {
            Map<String, Object> properties = new LinkedHashMap<>();
            schema.getProperties().forEach((name, propSchema) -> {
                properties.put(name, convertSchemaToMap((Schema<?>) propSchema));
            });
            result.put("properties", properties);
        }

        if (schema.getRequired() != null && !schema.getRequired().isEmpty()) {
            result.put("required", schema.getRequired());
        }

        if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
            result.put("enum", schema.getEnum());
        }

        if (schema.getItems() != null) {
            result.put("items", convertSchemaToMap(schema.getItems()));
        }

        if (schema.getAllOf() != null && !schema.getAllOf().isEmpty()) {
            result.put("allOf", schema.getAllOf().stream()
                .map(this::convertSchemaToMap)
                .toList());
        }

        if (schema.getOneOf() != null && !schema.getOneOf().isEmpty()) {
            result.put("oneOf", schema.getOneOf().stream()
                .map(this::convertSchemaToMap)
                .toList());
        }

        if (schema.getAnyOf() != null && !schema.getAnyOf().isEmpty()) {
            result.put("anyOf", schema.getAnyOf().stream()
                .map(this::convertSchemaToMap)
                .toList());
        }

        // Handle discriminator
        if (schema.getDiscriminator() != null) {
            Map<String, Object> discriminator = new LinkedHashMap<>();
            discriminator.put("propertyName", schema.getDiscriminator().getPropertyName());
            if (schema.getDiscriminator().getMapping() != null) {
                discriminator.put("mapping", schema.getDiscriminator().getMapping());
            }
            result.put("discriminator", discriminator);
        }

        // Handle $ref
        if (schema.get$ref() != null) {
            String ref = schema.get$ref();
            // Convert OpenAPI reference to a more readable format
            if (ref.startsWith("#/components/schemas/")) {
                ref = ref.replace("#/components/schemas/", "schema://");
            }
            result.put("$ref", ref);
        }

        if (schema.getDefault() != null) {
            result.put("default", schema.getDefault());
        }

        if (schema.getExample() != null) {
            result.put("example", schema.getExample());
        }

        if (schema.getMinimum() != null) {
            result.put("minimum", schema.getMinimum());
        }

        if (schema.getMaximum() != null) {
            result.put("maximum", schema.getMaximum());
        }

        if (schema.getMinLength() != null) {
            result.put("minLength", schema.getMinLength());
        }

        if (schema.getMaxLength() != null) {
            result.put("maxLength", schema.getMaxLength());
        }

        if (schema.getPattern() != null) {
            result.put("pattern", schema.getPattern());
        }

        return result;
    }
}
