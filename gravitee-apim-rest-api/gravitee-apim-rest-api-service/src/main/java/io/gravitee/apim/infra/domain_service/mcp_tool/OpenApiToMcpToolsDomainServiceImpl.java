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
package io.gravitee.apim.infra.domain_service.mcp_tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.apim.core.mcp_tool.domain_service.OpenApiToMcpToolsDomainService;
import io.gravitee.apim.core.mcp_tool.model.McpTool;
import io.gravitee.apim.core.mcp_tool.model.McpToolAnnotations;
import io.gravitee.apim.core.mcp_tool.model.McpToolDefinition;
import io.gravitee.apim.core.mcp_tool.model.McpToolGatewayMapping;
import io.gravitee.apim.core.mcp_tool.model.McpToolGatewayMappingHttp;
import io.gravitee.apim.core.mcp_tool.model.OpenApiToMcpToolsResult;
import io.gravitee.apim.core.mcp_tool.model.ParseError;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.CustomLog;
import org.springframework.stereotype.Service;

@Service
@CustomLog
public class OpenApiToMcpToolsDomainServiceImpl implements OpenApiToMcpToolsDomainService {

    private static final List<String> SUCCESS_STATUS_CODES = List.of("200", "201", "202");
    private static final Pattern OAS_PATH_PARAM = Pattern.compile("\\{([a-zA-Z0-9_-]+)\\}");
    private static final ObjectMapper MAPPER = Json.mapper();

    @Override
    public OpenApiToMcpToolsResult parse(String openApiSpec) {
        if (openApiSpec == null || openApiSpec.isBlank()) {
            return error(ParseError.INVALID_FORMAT, "Failed to parse specification");
        }
        ParseOutcome outcome = tryParse(openApiSpec);
        if (outcome.openApi() == null) {
            return error(outcome.errorKey(), outcome.errorMessage());
        }
        return buildResult(outcome.openApi());
    }

    private static ParseOutcome tryParse(String openApiSpec) {
        SwaggerParseResult parseResult;
        try {
            var options = new ParseOptions();
            options.setResolve(true);
            options.setResolveFully(true);
            parseResult = new OpenAPIParser().readContents(openApiSpec, null, options);
        } catch (Exception e) {
            return ParseOutcome.failure(ParseError.INVALID_FORMAT, "Failed to parse specification: " + e.getMessage());
        }
        OpenAPI api = parseResult.getOpenAPI();
        if (api != null) {
            return ParseOutcome.success(api);
        }
        String messages = String.join("; ", Optional.ofNullable(parseResult.getMessages()).orElse(List.of()));
        String key = messages.toLowerCase(Locale.ROOT).contains("$ref") ? ParseError.INVALID_REFS : ParseError.INVALID_SPEC;
        return ParseOutcome.failure(key, messages.isBlank() ? "Invalid OpenAPI specification" : messages);
    }

    private static OpenApiToMcpToolsResult buildResult(OpenAPI api) {
        List<McpTool> tools = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();
        Set<String> usedNames = new HashSet<>();
        Map<String, PathItem> paths = Optional.<Map<String, PathItem>>ofNullable(api.getPaths()).orElse(Map.of());
        paths.forEach((path, pathItem) -> appendToolsForPath(path, pathItem, tools, errors, usedNames));
        return new OpenApiToMcpToolsResult(tools, errors, buildServerDescription(api.getInfo()));
    }

    private static void appendToolsForPath(
        String path,
        PathItem pathItem,
        List<McpTool> tools,
        List<ParseError> errors,
        Set<String> usedNames
    ) {
        if (pathItem == null) return;
        List<Parameter> pathLevelParams = Optional.ofNullable(pathItem.getParameters()).orElse(List.of());
        pathItem
            .readOperationsMap()
            .forEach((httpMethod, op) -> {
                if (op == null) return;
                String method = httpMethod.name().toLowerCase(Locale.ROOT);
                tools.add(buildTool(path, method, op, pathLevelParams, usedNames, errors));
            });
    }

    private static McpTool buildTool(
        String path,
        String method,
        Operation op,
        List<Parameter> pathLevelParams,
        Set<String> usedNames,
        List<ParseError> errors
    ) {
        String toolName = disambiguate(computeToolName(op.getOperationId(), method, path), usedNames, errors);
        ParameterSchema paramSchema = extractParameterSchema(mergeParameters(pathLevelParams, op.getParameters()));
        var definition = new McpToolDefinition(
            toolName,
            buildDescription(op, method, path),
            buildInputSchema(paramSchema, extractRequestBodySchema(op.getRequestBody())),
            buildOutputSchema(extractResponseSchema(op.getResponses())),
            extractAnnotations(op)
        );
        return new McpTool(definition, generateGatewayMapping(op, method, path, paramSchema));
    }

    private static String disambiguate(String baseName, Set<String> usedNames, List<ParseError> errors) {
        String name = baseName;
        int suffix = 2;
        while (!usedNames.add(name)) {
            name = baseName + "_" + suffix++;
        }
        if (!name.equals(baseName)) {
            errors.add(new ParseError(ParseError.DUPLICATE_NAME, "Duplicate tool name '" + baseName + "' resolved to '" + name + "'"));
        }
        return name;
    }

    private record ParseOutcome(@org.jspecify.annotations.Nullable OpenAPI openApi, String errorKey, String errorMessage) {
        static ParseOutcome success(OpenAPI api) {
            return new ParseOutcome(api, "", "");
        }

        static ParseOutcome failure(String key, String message) {
            return new ParseOutcome(null, key, message);
        }
    }

    private static OpenApiToMcpToolsResult error(String key, String message) {
        return new OpenApiToMcpToolsResult(List.of(), List.of(new ParseError(key, message)), null);
    }

    private static String computeToolName(String operationId, String method, String path) {
        if (operationId != null && !operationId.isBlank()) {
            return operationId;
        }
        return snakeCase(method + "_" + path);
    }

    private static String snakeCase(String value) {
        var snake = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        // Linear scan instead of a regex like "^_+|_+$" to avoid a polynomial-backtracking surface
        // (Sonar java:S5852). Stripping underscores from both ends is O(n) and intent-explicit.
        int start = 0;
        int end = snake.length();
        while (start < end && snake.charAt(start) == '_') start++;
        while (end > start && snake.charAt(end - 1) == '_') end--;
        return snake.substring(start, end);
    }

    private static List<Parameter> mergeParameters(List<Parameter> pathLevel, List<Parameter> operationLevel) {
        // Operation-level parameters override path-level ones with the same (in, name) key (OpenAPI §4.8.9).
        Map<String, Parameter> merged = new LinkedHashMap<>();
        for (Parameter p : pathLevel) {
            if (p != null && p.getIn() != null && p.getName() != null) {
                merged.put(p.getIn() + ":" + p.getName(), p);
            }
        }
        if (operationLevel != null) {
            for (Parameter p : operationLevel) {
                if (p != null && p.getIn() != null && p.getName() != null) {
                    merged.put(p.getIn() + ":" + p.getName(), p);
                }
            }
        }
        return new ArrayList<>(merged.values());
    }

    private record ParameterSchema(
        Map<String, JsonNode> pathParams,
        Map<String, JsonNode> queryParams,
        Map<String, JsonNode> headers,
        List<String> required
    ) {}

    private record ResponseSchema(JsonNode bodySchema, Map<String, JsonNode> headers) {}

    private static ParameterSchema extractParameterSchema(List<Parameter> parameters) {
        Map<String, JsonNode> pathParams = new LinkedHashMap<>();
        Map<String, JsonNode> queryParams = new LinkedHashMap<>();
        Map<String, JsonNode> headers = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        for (Parameter param : parameters) {
            String name = param.getName();
            ObjectNode property = schemaToObjectNode(param.getSchema());
            if (param.getDescription() != null) {
                property.put("description", param.getDescription());
            }
            if (Boolean.TRUE.equals(param.getRequired())) {
                required.add(name);
            }
            switch (param.getIn()) {
                case "path" -> pathParams.put(name, property);
                case "query" -> queryParams.put(name, property);
                case "header" -> headers.put(name, property);
                default -> {
                    // cookie or other locations are not surfaced as MCP tool parameters
                }
            }
        }

        return new ParameterSchema(pathParams, queryParams, headers, required);
    }

    private static JsonNode extractRequestBodySchema(RequestBody requestBody) {
        if (requestBody == null || requestBody.getContent() == null) {
            return null;
        }
        MediaType jsonMedia = requestBody.getContent().get("application/json");
        if (jsonMedia == null || jsonMedia.getSchema() == null) {
            return null;
        }
        return schemaToObjectNode(jsonMedia.getSchema());
    }

    private static ResponseSchema extractResponseSchema(ApiResponses responses) {
        if (responses == null) {
            return new ResponseSchema(null, Map.of());
        }
        for (String statusCode : SUCCESS_STATUS_CODES) {
            ApiResponse response = responses.get(statusCode);
            if (response == null) continue;
            JsonNode bodySchema = extractJsonResponseBody(response);
            Map<String, JsonNode> headers = extractResponseHeaders(response);
            if (bodySchema != null || !headers.isEmpty()) {
                return new ResponseSchema(bodySchema, headers);
            }
        }
        return new ResponseSchema(null, Map.of());
    }

    private static JsonNode extractJsonResponseBody(ApiResponse response) {
        if (response.getContent() == null) {
            return null;
        }
        MediaType jsonMedia = response.getContent().get("application/json");
        if (jsonMedia == null || jsonMedia.getSchema() == null) {
            return null;
        }
        return schemaToObjectNode(jsonMedia.getSchema());
    }

    private static Map<String, JsonNode> extractResponseHeaders(ApiResponse response) {
        if (response.getHeaders() == null || response.getHeaders().isEmpty()) {
            return Map.of();
        }
        Map<String, JsonNode> headers = new LinkedHashMap<>();
        response
            .getHeaders()
            .forEach((name, header) -> {
                if (header == null || header.getSchema() == null) return;
                ObjectNode property = schemaToObjectNode(header.getSchema());
                if (header.getDescription() != null) {
                    property.put("description", header.getDescription());
                }
                headers.put(name, property);
            });
        return headers;
    }

    private static McpToolAnnotations extractAnnotations(Operation operation) {
        Map<String, Object> extensions = operation.getExtensions();
        if (extensions == null || extensions.isEmpty()) {
            return null;
        }
        String title = stringExtension(extensions, "x-mcp-title");
        Boolean readOnly = booleanExtension(extensions, "x-mcp-readOnlyHint");
        Boolean destructive = booleanExtension(extensions, "x-mcp-destructiveHint");
        Boolean idempotent = booleanExtension(extensions, "x-mcp-idempotentHint");
        Boolean openWorld = booleanExtension(extensions, "x-mcp-openWorldHint");
        if (title == null && readOnly == null && destructive == null && idempotent == null && openWorld == null) {
            return null;
        }
        return new McpToolAnnotations(title, readOnly, destructive, idempotent, openWorld);
    }

    private static String stringExtension(Map<String, Object> extensions, String key) {
        Object value = extensions.get(key);
        return value instanceof String s ? s : null;
    }

    private static Boolean booleanExtension(Map<String, Object> extensions, String key) {
        Object value = extensions.get(key);
        return value instanceof Boolean b ? b : null;
    }

    private static JsonNode buildInputSchema(ParameterSchema paramSchema, JsonNode bodySchema) {
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        paramSchema.pathParams().forEach(properties::set);
        paramSchema.queryParams().forEach(properties::set);
        paramSchema.headers().forEach(properties::set);
        if (bodySchema != null) {
            properties.set("bodySchema", bodySchema);
        }
        ArrayNode required = schema.putArray("required");
        paramSchema.required().forEach(required::add);
        return removeCircularRefs(schema);
    }

    private static JsonNode buildOutputSchema(ResponseSchema responseSchema) {
        if (responseSchema.bodySchema() == null && responseSchema.headers().isEmpty()) {
            return null;
        }
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        responseSchema.headers().forEach(properties::set);
        if (responseSchema.bodySchema() != null) {
            properties.set("bodySchema", responseSchema.bodySchema());
        }
        schema.putArray("required");
        return removeCircularRefs(schema);
    }

    private static McpToolGatewayMapping generateGatewayMapping(Operation op, String method, String path, ParameterSchema paramSchema) {
        Matcher m = OAS_PATH_PARAM.matcher(path);
        String transformedPath = m.replaceAll(":$1");
        String contentType = determineContentType(op);

        // Empty lists are serialised to null so Jackson (NON_NULL inclusion) drops the field entirely,
        // matching the front-end behaviour of omitting absent gatewayMapping fields.
        List<String> pathParamNames = paramSchema.pathParams().isEmpty() ? null : new ArrayList<>(paramSchema.pathParams().keySet());
        List<String> queryParamNames = paramSchema.queryParams().isEmpty() ? null : new ArrayList<>(paramSchema.queryParams().keySet());
        List<String> headerNames = paramSchema.headers().isEmpty() ? null : new ArrayList<>(paramSchema.headers().keySet());

        return new McpToolGatewayMapping(
            new McpToolGatewayMappingHttp(
                method.toUpperCase(Locale.ROOT),
                transformedPath,
                contentType,
                pathParamNames,
                queryParamNames,
                headerNames
            )
        );
    }

    private static String determineContentType(Operation op) {
        if (op.getRequestBody() == null || op.getRequestBody().getContent() == null) {
            return null;
        }
        var entries = op.getRequestBody().getContent().keySet();
        if (entries.isEmpty()) {
            return null;
        }
        return entries.iterator().next();
    }

    private static String buildDescription(Operation op, String method, String path) {
        String summary = op.getSummary();
        String description = op.getDescription();
        if (summary != null && description != null && !summary.isBlank() && !description.isBlank()) {
            return summary + "\n\n" + description;
        }
        if (summary != null && !summary.isBlank()) return summary;
        if (description != null && !description.isBlank()) return description;
        return "API for " + method.toUpperCase(Locale.ROOT) + " " + path;
    }

    private static String buildServerDescription(Info info) {
        if (info == null) return null;
        String title = info.getTitle();
        String description = info.getDescription();
        if (title != null && !title.isBlank() && description != null && !description.isBlank()) {
            return title + "\n\n" + description;
        }
        if (title != null && !title.isBlank()) return title;
        if (description != null && !description.isBlank()) return description;
        return null;
    }

    private static ObjectNode schemaToObjectNode(Schema<?> schema) {
        if (schema == null) {
            return MAPPER.createObjectNode();
        }
        try {
            JsonNode tree = MAPPER.valueToTree(schema);
            return tree.isObject() ? (ObjectNode) tree : MAPPER.createObjectNode();
        } catch (Exception e) {
            log.warn("Failed to serialize schema to JSON node, falling back to empty object: {}", e.getMessage());
            return MAPPER.createObjectNode();
        }
    }

    /**
     * Walks the JSON tree and replaces any ancestor-revisit with an empty object. Shared
     * (non-circular) references are preserved. Mirrors the front-end {@code removeCircularRefs}
     * function so the gateway never receives a schema graph it cannot serialise.
     */
    private static JsonNode removeCircularRefs(JsonNode node) {
        return removeCircularRefs(node, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static JsonNode removeCircularRefs(JsonNode node, Set<JsonNode> ancestors) {
        if (node == null || !(node.isObject() || node.isArray())) {
            return node;
        }
        if (ancestors.contains(node)) {
            return MAPPER.createObjectNode();
        }
        Set<JsonNode> nextAncestors = Collections.newSetFromMap(new IdentityHashMap<>());
        nextAncestors.addAll(ancestors);
        nextAncestors.add(node);
        if (node.isArray()) {
            ArrayNode array = MAPPER.createArrayNode();
            for (JsonNode item : node) {
                array.add(removeCircularRefs(item, nextAncestors));
            }
            return array;
        }
        ObjectNode object = MAPPER.createObjectNode();
        Iterator<Map.Entry<String, JsonNode>> it = node.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> field = it.next();
            object.set(field.getKey(), removeCircularRefs(field.getValue(), nextAncestors));
        }
        return object;
    }
}
