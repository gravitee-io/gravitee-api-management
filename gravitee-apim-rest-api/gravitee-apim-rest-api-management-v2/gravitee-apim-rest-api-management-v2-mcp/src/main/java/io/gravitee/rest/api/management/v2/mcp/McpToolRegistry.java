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

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.CustomLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Registry for MCP tools generated from OpenAPI specifications.
 * Aggregates tools from multiple OpenAPI spec files at startup.
 *
 * @author GraviteeSource Team
 */
@Component
@CustomLog
public class McpToolRegistry {

    private final List<SyncToolSpecification> tools = new ArrayList<>();

    @Autowired
    private OpenApiToolGenerator toolGenerator;

    /**
     * List of OpenAPI specification files to process.
     * These files should be located in the classpath under the openapi directory.
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
        log.info("Initializing MCP Tool Registry...");

        for (String spec : OPENAPI_SPECS) {
            try {
                List<SyncToolSpecification> specTools = toolGenerator.generateFromSpec(spec);
                tools.addAll(specTools);
                log.info("Loaded {} tools from {}", specTools.size(), spec);
            } catch (Exception e) {
                log.error("Failed to load tools from {}: {}", spec, e.getMessage(), e);
            }
        }

        log.info("MCP Tool Registry initialized with {} total tools", tools.size());
    }

    /**
     * Returns an unmodifiable list of all registered tools.
     *
     * @return list of MCP tool specifications
     */
    public List<SyncToolSpecification> getTools() {
        return Collections.unmodifiableList(tools);
    }
}
