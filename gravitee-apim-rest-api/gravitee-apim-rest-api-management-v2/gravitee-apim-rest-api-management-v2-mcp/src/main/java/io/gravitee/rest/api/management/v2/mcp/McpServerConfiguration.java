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
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import lombok.CustomLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the MCP (Model Context Protocol) server.
 * The transport is created in JettyEmbeddedContainer and registered as a singleton.
 * This configuration creates the McpSyncServer using that transport.
 *
 * @author GraviteeSource Team
 */
@Configuration
@ComponentScan(basePackages = "io.gravitee.rest.api.management.v2.mcp")
@CustomLog
public class McpServerConfiguration {

    @Bean
    public McpJsonMapper mcpJsonMapper() {
        return new JacksonMcpJsonMapper(new ObjectMapper());
    }

    @Bean
    public McpSyncServer mcpServer(
        HttpServletStreamableServerTransportProvider mcpTransportProvider,
        McpToolRegistry toolRegistry,
        McpResourceRegistry resourceRegistry
    ) {

        McpSyncServer server = McpServer.sync(mcpTransportProvider)
            .serverInfo("gravitee-apim", "4.0.0")
            .capabilities(ServerCapabilities.builder()
                .tools(true)
                .resources(true, true)
                .build())
            .build();

        toolRegistry.getTools().forEach(server::addTool);
        resourceRegistry.getResources().forEach(server::addResource);

        log.info("McpSyncServer fully initialized with {} tools and {} resources",
            toolRegistry.getTools().size(),
            resourceRegistry.getResources().size());
        return server;
    }
}
