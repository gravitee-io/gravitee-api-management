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

import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.ResourceContents;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import jakarta.annotation.PostConstruct;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Registry for MCP resources generated from OpenAPI schema definitions.
 * Exposes JSON schemas as MCP resources for LLMs to understand data structures.
 *
 * @author GraviteeSource Team
 */
@Component
@CustomLog
public class McpResourceRegistry {

    private final Map<String, String> schemaContents = new HashMap<>();
    private final List<SyncResourceSpecification> resources = new ArrayList<>();

    @Autowired
    private OpenApiSchemaExtractor schemaExtractor;

    /**
     * List of OpenAPI specification files to process.
     * These should match the same files used for tools.
     */
    private static final List<String> OPENAPI_SPECS = List.of(
        "openapi/openapi-apis.yaml",
        "openapi/openapi-environments.yaml",
        "openapi/openapi-plugins.yaml",
        "openapi/openapi-installation.yaml",
        "openapi/openapi-analytics.yaml"
    );

    @PostConstruct
    public void init() {
        log.info("Initializing MCP Resource Registry...");

        for (String spec : OPENAPI_SPECS) {
            try {
                Map<String, String> schemas = schemaExtractor.extractSchemas(spec);
                schemas.forEach((schemaName, schemaContent) -> {
                    // Avoid duplicates - keep first definition if schema already exists
                    if (!schemaContents.containsKey(schemaName)) {
                        schemaContents.put(schemaName, schemaContent);
                        SyncResourceSpecification resourceSpec = createResourceSpec(schemaName, schemaContent);
                        resources.add(resourceSpec);
                    } else {
                        log.debug("Schema {} already registered, skipping duplicate from {}", schemaName, spec);
                    }
                });
                log.info("Loaded {} schemas from {}", schemas.size(), spec);
            } catch (Exception e) {
                log.error("Failed to load schemas from {}: {}", spec, e.getMessage(), e);
            }
        }

        log.info("MCP Resource Registry initialized with {} total schemas", schemaContents.size());
    }

    /**
     * Creates an MCP resource specification for a schema.
     *
     * @param schemaName the name of the schema
     * @param schemaContent the JSON Schema content
     * @return resource specification
     */
    private SyncResourceSpecification createResourceSpec(String schemaName, String schemaContent) {
        String uri = "schema://" + schemaName;
        String description = "JSON Schema definition for " + schemaName;

        Resource resource = Resource.builder()
            .uri(uri)
            .name(schemaName)
            .description(description)
            .mimeType("application/schema+json")
            .build();

        return new SyncResourceSpecification(resource, (mcpSyncServerExchange,  readResourceRequest) ->
            new McpSchema.ReadResourceResult(List.of(new TextResourceContents(uri, "application/schema+json", schemaContent))));
    }

    /**
     * Returns an unmodifiable list of all registered resources.
     *
     * @return list of MCP resource specifications
     */
    public List<SyncResourceSpecification> getResources() {
        return Collections.unmodifiableList(resources);
    }

    /**
     * Gets the JSON Schema content for a given schema name.
     *
     * @param schemaName the schema name
     * @return the schema content, or null if not found
     */
    public String getSchemaContent(String schemaName) {
        return schemaContents.get(schemaName);
    }

    /**
     * Returns the set of all schema names.
     *
     * @return set of schema names
     */
    public Set<String> getSchemaNames() {
        return Collections.unmodifiableSet(schemaContents.keySet());
    }
}
