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

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpSchema.ToolAnnotations;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.CustomLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Generates MCP tools from OpenAPI specifications.
 * Reads OpenAPI specs and creates MCP tool definitions for operations
 * that have the x-mcp extension enabled.
 *
 * @author GraviteeSource Team
 */
@Component
@CustomLog
public class OpenApiToolGenerator {
    private static final String X_MCP_EXTENSION = "x-mcp";
    private static final String AUTH_HEADER_KEY = "authorizationHeader";

    @Autowired
    private McpHttpToolInvoker httpInvoker;

    @Autowired
    private McpJsonMapper mcpJsonMapper;

    /**
     * Generates MCP tool specifications from an OpenAPI spec file.
     *
     * @param specPath the classpath path to the OpenAPI specification
     * @return list of tool specifications generated from the spec
     */
    public List<SyncToolSpecification> generateFromSpec(String specPath) {
        List<SyncToolSpecification> tools = new ArrayList<>();

        try {
            URL specUrl = getClass().getClassLoader().getResource(specPath);
            if (specUrl == null) {
                log.warn("OpenAPI spec not found: {}", specPath);
                return tools;
            }

            OpenAPI openApi = new OpenAPIV3Parser().read(specUrl.toString());
            if (openApi == null) {
                log.warn("Failed to parse OpenAPI spec: {}", specPath);
                return tools;
            }

            if (openApi.getPaths() == null) {
                log.debug("No paths found in OpenAPI spec: {}", specPath);
                return tools;
            }

            openApi
                .getPaths()
                .forEach((path, pathItem) -> {
                    pathItem
                        .readOperationsMap()
                        .forEach((method, operation) -> {
                            Map<String, Object> mcpConfig = getMcpExtension(operation);
                            if (mcpConfig != null && Boolean.TRUE.equals(mcpConfig.get("enabled"))) {
                                try {
                                    SyncToolSpecification toolSpec = createToolSpec(path, method, operation, mcpConfig);
                                    tools.add(toolSpec);
                                    log.info("Registered MCP tool: {} for {} {}", mcpConfig.get("name"), method, path);
                                } catch (Exception e) {
                                    log.error("Failed to create tool spec for {} {}: {}", method, path, e.getMessage());
                                }
                            }
                        });
                });
        } catch (Exception e) {
            log.error("Error processing OpenAPI spec {}: {}", specPath, e.getMessage(), e);
        }

        return tools;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMcpExtension(Operation operation) {
        if (operation.getExtensions() == null) {
            return null;
        }
        Object extension = operation.getExtensions().get(X_MCP_EXTENSION);
        if (extension instanceof Map) {
            return (Map<String, Object>) extension;
        }
        return null;
    }

    /**
     * Builds MCP tool annotations based on HTTP method semantics.
     * These annotations help LLMs understand the behavior of each tool.
     *
     * @param method the HTTP method of the operation
     * @param mcpConfig the x-mcp extension configuration (may contain overrides)
     * @return ToolAnnotations with appropriate hints
     */
    private ToolAnnotations buildToolAnnotations(PathItem.HttpMethod method, Map<String, Object> mcpConfig) {
        // Determine hints based on HTTP method semantics
        boolean isReadOnly = method == PathItem.HttpMethod.GET || method == PathItem.HttpMethod.HEAD;
        boolean isDestructive = method == PathItem.HttpMethod.DELETE;
        boolean isIdempotent = method == PathItem.HttpMethod.GET
            || method == PathItem.HttpMethod.HEAD
            || method == PathItem.HttpMethod.PUT
            || method == PathItem.HttpMethod.DELETE;

        // Allow override via x-mcp config
        if (mcpConfig.containsKey("readOnly")) {
            isReadOnly = Boolean.TRUE.equals(mcpConfig.get("readOnly"));
        }
        if (mcpConfig.containsKey("destructive")) {
            isDestructive = Boolean.TRUE.equals(mcpConfig.get("destructive"));
        }
        if (mcpConfig.containsKey("idempotent")) {
            isIdempotent = Boolean.TRUE.equals(mcpConfig.get("idempotent"));
        }

        return new ToolAnnotations(
                (String) mcpConfig.get("title"),
                isReadOnly,
                isDestructive,
                isIdempotent,
                false, // Internal Gravitee API, no external interactions,
                false
        );
    }

    private SyncToolSpecification createToolSpec(
        String path,
        PathItem.HttpMethod method,
        Operation operation,
        Map<String, Object> mcpConfig
    ) {
        String toolName = (String) mcpConfig.getOrDefault("name", operation.getOperationId());
        String description = buildEnhancedDescription(operation, mcpConfig);
        ToolAnnotations annotations = buildToolAnnotations(method, mcpConfig);

        String inputSchema = buildInputSchema(operation);
        String outputSchema = buildOutputSchema(operation);

        Tool.Builder toolBuilder = Tool.builder()
            .name(toolName)
            .description(description)
            .annotations(annotations)
            .inputSchema(mcpJsonMapper, inputSchema);

        // Add outputSchema if present
        if (outputSchema != null) {
            // TODO: check if we want or not. mcp inspector have an issur on it :/
            // https://github.com/modelcontextprotocol/modelcontextprotocol/pull/2106
//            toolBuilder.outputSchema(mcpJsonMapper, outputSchema);
        }

        Tool tool = toolBuilder.build();

        return new SyncToolSpecification(tool, (exchange, arguments) -> {
            try {
                String authHeader = extractAuthHeader(exchange);
                return httpInvoker.invoke(path, method, arguments, authHeader);
            } catch (Exception e) {
                log.error("Error invoking tool {}: {}", toolName, e.getMessage(), e);
                return new CallToolResult(List.of(new TextContent("Error: " + e.getMessage())), true);
            }
        });
    }

    private String buildInputSchema(Operation operation) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        // Process path and query parameters
        if (operation.getParameters() != null) {
            for (Parameter param : operation.getParameters()) {
                Map<String, Object> paramSchema = new LinkedHashMap<>();

                if (param.getSchema() != null) {
                    paramSchema.put("type", mapSchemaType(param.getSchema()));
                    if (param.getSchema().getDescription() != null) {
                        paramSchema.put("description", param.getSchema().getDescription());
                    }
                } else {
                    paramSchema.put("type", "string");
                }

                if (param.getDescription() != null) {
                    paramSchema.put("description", param.getDescription());
                }

                properties.put(param.getName(), paramSchema);

                if (Boolean.TRUE.equals(param.getRequired())) {
                    required.add(param.getName());
                }
            }
        }

        // Process request body if present
        if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
            var jsonContent = operation.getRequestBody().getContent().get("application/json");
            if (jsonContent != null && jsonContent.getSchema() != null) {
                Schema<?> bodySchema = jsonContent.getSchema();
                Map<String, Object> bodySchemaMap = new LinkedHashMap<>();

                // If it's a direct reference, use schema://
                if (bodySchema.get$ref() != null) {
                    String schemaName = extractSchemaName(bodySchema.get$ref());
                    bodySchemaMap.put("$ref", "schema://" + schemaName);
                    bodySchemaMap.put("description", "Request body - read schema://" + schemaName + " for structure");
                } else {
                    // Otherwise convert with schema:// references for nested types
                    bodySchemaMap = convertSchemaToMap(bodySchema);
                    bodySchemaMap.put("description", "Request body");
                }
                properties.put("body", bodySchemaMap);

                if (Boolean.TRUE.equals(operation.getRequestBody().getRequired())) {
                    required.add("body");
                }
            }
        }

        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }

        try {
            return mcpJsonMapper.writeValueAsString(schema);
        } catch (IOException e) {
            log.error("Failed to serialize schema: {}", e.getMessage());
            return "{}";
        }
    }

    private String buildOutputSchema(Operation operation) {
        if (operation.getResponses() == null) {
            return null;
        }

        // Look for 200 response
        ApiResponse response200 = operation.getResponses().get("200");
        if (response200 == null || response200.getContent() == null) {
            return null;
        }

        // Get JSON content
        MediaType jsonContent = response200.getContent().get("application/json");
        if (jsonContent == null || jsonContent.getSchema() == null) {
            return null;
        }

        try {
            Map<String, Object> outputSchema = convertSchemaToMap(jsonContent.getSchema());

            // Collect all schema:// references for LLM guidance
            Set<String> schemaRefs = new HashSet<>();
            collectSchemaRefs(outputSchema, schemaRefs);

            if (!schemaRefs.isEmpty()) {
                outputSchema.put("_mcpSchemaRefs", new ArrayList<>(schemaRefs));
            }

            return mcpJsonMapper.writeValueAsString(outputSchema);
        } catch (IOException e) {
            log.error("Failed to serialize output schema: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Converts an OpenAPI Schema to a Map structure with schema:// references.
     * This allows MCP clients to link tool outputs to resource schemas.
     */
    private Map<String, Object> convertSchemaToMap(Schema<?> schema) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Handle $ref - convert OpenAPI references to MCP resource URIs
        if (schema.get$ref() != null) {
            String ref = schema.get$ref();
            if (ref.startsWith("#/components/schemas/")) {
                String schemaName = ref.substring("#/components/schemas/".length());
                result.put("$ref", "schema://" + schemaName);
                return result;
            }
        }

        if (schema.getType() != null) {
            result.put("type", schema.getType());
        }

        if (schema.getDescription() != null) {
            result.put("description", schema.getDescription());
        }

        // Handle array items
        if (schema instanceof ArraySchema) {
            ArraySchema arraySchema = (ArraySchema) schema;
            if (arraySchema.getItems() != null) {
                result.put("items", convertSchemaToMap(arraySchema.getItems()));
            }
        }

        // Handle object properties
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

        // Handle composition keywords
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

        return result;
    }

    private String mapSchemaType(Schema<?> schema) {
        if (schema.getType() != null) {
            return schema.getType();
        }
        return "string";
    }

    /**
     * Extracts the schema name from an OpenAPI $ref.
     * @param ref the $ref string (e.g., "#/components/schemas/ApiV4")
     * @return the schema name (e.g., "ApiV4")
     */
    private String extractSchemaName(String ref) {
        if (ref != null && ref.startsWith("#/components/schemas/")) {
            return ref.substring("#/components/schemas/".length());
        }
        return ref;
    }

    /**
     * Extracts a schema reference from a Schema object.
     * Returns the schema name if it's a direct $ref or from array items.
     */
    private String extractSchemaRef(Schema<?> schema) {
        if (schema == null) {
            return null;
        }
        if (schema.get$ref() != null) {
            return extractSchemaName(schema.get$ref());
        }
        if (schema instanceof ArraySchema) {
            ArraySchema arraySchema = (ArraySchema) schema;
            if (arraySchema.getItems() != null && arraySchema.getItems().get$ref() != null) {
                return extractSchemaName(arraySchema.getItems().get$ref());
            }
        }
        return null;
    }

    /**
     * Builds an enhanced description with hints for the LLM to read schema resources.
     */
    private String buildEnhancedDescription(Operation operation, Map<String, Object> mcpConfig) {
        StringBuilder desc = new StringBuilder();

        // Base description
        String baseDesc = (String) mcpConfig.getOrDefault("description",
            operation.getSummary() != null ? operation.getSummary() : "");
        desc.append(baseDesc);

        // Hint for request body if present
        if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
            var jsonContent = operation.getRequestBody().getContent().get("application/json");
            if (jsonContent != null && jsonContent.getSchema() != null && jsonContent.getSchema().get$ref() != null) {
                String schemaName = extractSchemaName(jsonContent.getSchema().get$ref());
                desc.append("\n\nInput: Read resource schema://").append(schemaName)
                    .append(" to understand the request body structure.");
            }
        }

        // Hint for response
        if (operation.getResponses() != null) {
            ApiResponse response200 = operation.getResponses().get("200");
            if (response200 != null && response200.getContent() != null) {
                var jsonContent = response200.getContent().get("application/json");
                if (jsonContent != null && jsonContent.getSchema() != null) {
                    String schemaRef = extractSchemaRef(jsonContent.getSchema());
                    if (schemaRef != null) {
                        desc.append("\n\nOutput: Read resource schema://").append(schemaRef)
                            .append(" to understand the response structure.");
                    }
                }
            }
        }

        return desc.toString();
    }

    /**
     * Recursively collects all schema:// references from a schema map.
     */
    @SuppressWarnings("unchecked")
    private void collectSchemaRefs(Map<String, Object> schema, Set<String> refs) {
        if (schema == null) {
            return;
        }

        // Check for direct $ref
        if (schema.containsKey("$ref")) {
            String ref = (String) schema.get("$ref");
            if (ref != null && ref.startsWith("schema://")) {
                refs.add(ref.substring("schema://".length()));
            }
        }

        // Recursively process properties
        if (schema.containsKey("properties")) {
            Object properties = schema.get("properties");
            if (properties instanceof Map) {
                ((Map<String, Object>) properties).values().forEach(propValue -> {
                    if (propValue instanceof Map) {
                        collectSchemaRefs((Map<String, Object>) propValue, refs);
                    }
                });
            }
        }

        // Recursively process items (for arrays)
        if (schema.containsKey("items")) {
            Object items = schema.get("items");
            if (items instanceof Map) {
                collectSchemaRefs((Map<String, Object>) items, refs);
            }
        }

        // Recursively process allOf, oneOf, anyOf
        for (String keyword : List.of("allOf", "oneOf", "anyOf")) {
            if (schema.containsKey(keyword)) {
                Object composition = schema.get(keyword);
                if (composition instanceof List) {
                    ((List<Object>) composition).forEach(item -> {
                        if (item instanceof Map) {
                            collectSchemaRefs((Map<String, Object>) item, refs);
                        }
                    });
                }
            }
        }
    }

    private String extractAuthHeader(McpSyncServerExchange exchange) {
        // The auth header is extracted from the MCP transport context
        // which is set by the contextExtractor during request processing
        try {
            var context = exchange.transportContext();
            if (context != null) {
                Object authHeader = context.get(AUTH_HEADER_KEY);
                if (authHeader != null) {
                    return authHeader.toString();
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract auth header from context: {}", e.getMessage());
        }
        return null;
    }
}
