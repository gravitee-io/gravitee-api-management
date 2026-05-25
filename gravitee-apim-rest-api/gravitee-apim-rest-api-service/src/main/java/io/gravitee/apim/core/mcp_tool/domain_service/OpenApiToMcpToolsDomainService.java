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
package io.gravitee.apim.core.mcp_tool.domain_service;

import io.gravitee.apim.core.mcp_tool.model.OpenApiToMcpToolsResult;

/**
 * Parses an OpenAPI specification and produces the list of MCP tools its operations
 * map to. The output shape matches the {@code MCPTool} configuration consumed by the
 * {@code gravitee-entrypoint-mcp} gateway plugin: tools returned here can be persisted
 * then handed to the gateway without further transformation.
 *
 * <p>Implementations must accept both JSON and YAML payloads, both OpenAPI 3.x and
 * Swagger 2.0 (transparently converted to 3.0). Non-fatal issues encountered while
 * parsing are accumulated in {@link OpenApiToMcpToolsResult#errors()} rather than
 * thrown; a fatal issue (unparseable input, invalid spec, dereferencing failure)
 * returns an empty tool list with the corresponding error key.
 */
public interface OpenApiToMcpToolsDomainService {
    OpenApiToMcpToolsResult parse(String openApiSpec);
}
