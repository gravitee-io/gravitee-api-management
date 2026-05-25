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
package io.gravitee.apim.core.mcp_tool.model;

import lombok.Builder;

/**
 * An MCP tool derived from an OpenAPI operation. The shape is aligned byte-for-byte
 * with the {@code MCPTool} configuration consumed by the {@code gravitee-entrypoint-mcp}
 * gateway plugin: any tool produced here can be persisted then handed to the gateway
 * without further transformation.
 */
@Builder(toBuilder = true)
public record McpTool(McpToolDefinition toolDefinition, McpToolGatewayMapping gatewayMapping) {}
