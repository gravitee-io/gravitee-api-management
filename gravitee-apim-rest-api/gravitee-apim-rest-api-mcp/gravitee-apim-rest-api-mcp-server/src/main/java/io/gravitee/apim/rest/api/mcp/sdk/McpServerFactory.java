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
package io.gravitee.apim.rest.api.mcp.sdk;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.gravitee.apim.rest.api.mcp.spi.McpCaller;
import io.gravitee.apim.rest.api.mcp.spi.McpPrompt;
import io.gravitee.apim.rest.api.mcp.spi.McpResource;
import io.gravitee.apim.rest.api.mcp.spi.McpTool;
import io.gravitee.apim.rest.api.mcp.spi.ToolException;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptArgument;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpSchema.ToolAnnotations;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import lombok.CustomLog;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Serves tools and resources with the official MCP Java SDK, statelessly: no session, no server-initiated request.
 *
 * Together with {@link McpEndpointServlet}, the only place that knows the SDK. Moving to another protocol revision or
 * another protocol layer is a change to this package.
 */
@CustomLog
public final class McpServerFactory {

    private static final String SERVER_NAME = "gravitee-apim";
    private static final String CALLER = "gravitee.caller";
    private static final ToolAnnotations READ_ONLY = new ToolAnnotations(null, true, false, true, false, null);
    private static final ToolAnnotations ACTS = new ToolAnnotations(null, false, false, false, true, null);

    private static final ObjectMapper RESULTS = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private McpServerFactory() {}

    /**
     * @param tools listed to clients in this order
     */
    public static McpEndpointServlet create(
        String version,
        String instructions,
        List<McpTool<?>> tools,
        List<McpResource> resources,
        List<McpPrompt> prompts
    ) {
        var transport = HttpServletStatelessServerTransport.builder()
            // The servlet owns the path: every URI it lets through is the endpoint.
            .messageEndpoint("")
            .contextExtractor(request -> McpTransportContext.create(Map.of(CALLER, currentCaller())))
            .build();

        McpServer.sync(transport)
            .serverInfo(SERVER_NAME, version)
            .instructions(instructions)
            .capabilities(ServerCapabilities.builder().tools(false).resources(false, false).prompts(false).build())
            // Handlers run on the request thread: the analytics engine reads the caller from Spring Security's thread-bound context.
            .immediateExecution(true)
            .tools(tools.stream().map(McpServerFactory::toSpecification).toList())
            .resources(resources.stream().map(McpServerFactory::toSpecification).toList())
            .prompts(prompts.stream().map(McpServerFactory::toSpecification).toList())
            .build();

        return new McpEndpointServlet(transport);
    }

    private static McpCaller currentCaller() {
        var user = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return new McpCaller(user.getOrganizationId(), user.getUsername(), user.getSource(), user.getSourceId());
    }

    private static <I> SyncToolSpecification toSpecification(McpTool<I> tool) {
        var descriptor = tool.descriptor();
        var jsonMapper = McpJsonDefaults.getMapper();
        var definition = Tool.builder()
            .name(descriptor.name())
            .title(descriptor.title())
            .description(descriptor.description())
            .inputSchema(jsonMapper, descriptor.inputSchema().toString())
            .outputSchema(jsonMapper, descriptor.outputSchema().toString())
            .annotations(descriptor.readOnly() ? READ_ONLY : ACTS)
            .build();
        return SyncToolSpecification.builder()
            .tool(definition)
            .callHandler((transportContext, request) ->
                call(tool, (McpCaller) transportContext.get(CALLER), request.arguments() == null ? Map.of() : request.arguments())
            )
            .build();
    }

    private static <I> CallToolResult call(McpTool<I> tool, McpCaller caller, Map<String, Object> arguments) {
        var name = tool.descriptor().name();
        I input;
        try {
            input = RESULTS.convertValue(arguments, tool.inputType());
        } catch (IllegalArgumentException e) {
            return failure("Invalid arguments for " + name + ": " + e.getMessage());
        }
        try {
            Map<?, ?> structured = RESULTS.convertValue(tool.call(caller, input), Map.class);
            return CallToolResult.builder().structuredContent(structured).addTextContent(RESULTS.writeValueAsString(structured)).build();
        } catch (ToolException e) {
            return failure(e.getMessage());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot serialize the result of MCP tool " + name, e);
        } catch (RuntimeException e) {
            log.error("MCP tool {} failed for user {} of organization {}", name, caller.userId(), caller.organizationId(), e);
            return failure("The tool " + name + " failed on the server. Retry, or narrow the query.");
        }
    }

    private static CallToolResult failure(String message) {
        return CallToolResult.builder().addTextContent(message).isError(true).build();
    }

    private static SyncPromptSpecification toSpecification(McpPrompt prompt) {
        var arguments = prompt
            .arguments()
            .stream()
            .map(argument -> new PromptArgument(argument.name(), argument.description(), argument.required()))
            .toList();
        var definition = new Prompt(prompt.name(), prompt.title(), prompt.description(), arguments);
        return new SyncPromptSpecification(definition, (transportContext, request) -> {
            Map<String, String> values = new java.util.HashMap<>();
            if (request.arguments() != null) {
                request.arguments().forEach((key, value) -> values.put(key, value == null ? null : String.valueOf(value)));
            }
            return new GetPromptResult(prompt.description(), List.of(new PromptMessage(Role.USER, new TextContent(prompt.render(values)))));
        });
    }

    private static SyncResourceSpecification toSpecification(McpResource resource) {
        var definition = Resource.builder()
            .uri(resource.uri())
            .name(resource.name())
            .title(resource.title())
            .description(resource.description())
            .mimeType(resource.mimeType())
            .build();
        return new SyncResourceSpecification(definition, (transportContext, request) ->
            new ReadResourceResult(List.of(new TextResourceContents(resource.uri(), resource.mimeType(), resource.read())))
        );
    }
}
