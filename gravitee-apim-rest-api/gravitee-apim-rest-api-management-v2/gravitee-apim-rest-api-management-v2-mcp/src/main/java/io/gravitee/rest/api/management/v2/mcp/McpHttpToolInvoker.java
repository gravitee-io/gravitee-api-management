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
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.swagger.v3.oas.models.PathItem;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Invokes HTTP requests to the Gravitee REST API on behalf of MCP tool calls.
 * Handles path parameter resolution, query parameter building, and request body serialization.
 *
 * @author GraviteeSource Team
 */
@Component
public class McpHttpToolInvoker {

    private static final Logger LOG = LoggerFactory.getLogger(McpHttpToolInvoker.class);
    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{([^}]+)}");

    private final HttpClient httpClient;

    @Value("${http.api.management.entrypoint:/management}")
    private String managementEntrypoint;

    @Value("${jetty.port:8083}")
    private int jettyPort;

    @Value("${jetty.host:localhost}")
    private String jettyHost;

    @Autowired
    private ObjectMapper mcpObjectMapper;

    public McpHttpToolInvoker() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    /**
     * Invokes an HTTP request to the Gravitee REST API.
     *
     * @param path      the API path with path parameter placeholders
     * @param method    the HTTP method
     * @param arguments the arguments from the MCP tool call
     * @param authToken the authorization header value
     * @return the result of the tool call
     */
    public CallToolResult invoke(String path, PathItem.HttpMethod method, Map<String, Object> arguments, String authToken) {
        try {
            String resolvedPath = resolvePath(path, arguments);
            String queryString = buildQueryString(path, arguments);
            String url = buildUrl(resolvedPath, queryString);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(60));

            if (authToken != null && !authToken.isEmpty()) {
                requestBuilder.header("Authorization", authToken);
            }

            HttpRequest.BodyPublisher bodyPublisher = buildBody(method, arguments);
            requestBuilder.method(method.name(), bodyPublisher);

            HttpRequest request = requestBuilder.build();
            LOG.debug("Invoking MCP tool: {} {}", method, url);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            boolean isError = response.statusCode() >= 400;
            String responseBody = response.body();

            if (isError) {
                LOG.warn("MCP tool call failed with status {}: {}", response.statusCode(), responseBody);
            } else {
                LOG.debug("MCP tool call succeeded with status {}", response.statusCode());
            }

            return new CallToolResult(List.of(new TextContent(responseBody)), isError);
        } catch (Exception e) {
            LOG.error("Error invoking HTTP request: {}", e.getMessage(), e);
            return new CallToolResult(List.of(new TextContent("Error: " + e.getMessage())), true);
        }
    }

    private String buildUrl(String resolvedPath, String queryString) {
        StringBuilder url = new StringBuilder();
        url.append("http://").append(jettyHost).append(":").append(jettyPort);
        url.append(managementEntrypoint).append("/v2").append(resolvedPath);
        if (queryString != null && !queryString.isEmpty()) {
            url.append("?").append(queryString);
        }
        return url.toString();
    }

    private String resolvePath(String path, Map<String, Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return path;
        }

        String resolved = path;
        Matcher matcher = PATH_PARAM_PATTERN.matcher(path);

        while (matcher.find()) {
            String paramName = matcher.group(1);
            Object value = arguments.get(paramName);
            if (value != null) {
                resolved = resolved.replace("{" + paramName + "}", String.valueOf(value));
            }
        }

        return resolved;
    }

    private String buildQueryString(String path, Map<String, Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return "";
        }

        // Find path parameters to exclude them from query params
        Matcher matcher = PATH_PARAM_PATTERN.matcher(path);
        java.util.Set<String> pathParams = new java.util.HashSet<>();
        while (matcher.find()) {
            pathParams.add(matcher.group(1));
        }

        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Skip path parameters and body
            if (pathParams.contains(key) || "body".equals(key)) {
                continue;
            }

            if (value != null) {
                if (queryString.length() > 0) {
                    queryString.append("&");
                }
                queryString.append(java.net.URLEncoder.encode(key, java.nio.charset.StandardCharsets.UTF_8));
                queryString.append("=");
                queryString.append(java.net.URLEncoder.encode(String.valueOf(value), java.nio.charset.StandardCharsets.UTF_8));
            }
        }

        return queryString.toString();
    }

    private HttpRequest.BodyPublisher buildBody(PathItem.HttpMethod method, Map<String, Object> arguments) {
        if (method == PathItem.HttpMethod.GET || method == PathItem.HttpMethod.DELETE) {
            return HttpRequest.BodyPublishers.noBody();
        }

        if (arguments == null || !arguments.containsKey("body")) {
            return HttpRequest.BodyPublishers.noBody();
        }

        try {
            Object body = arguments.get("body");
            String jsonBody;
            if (body instanceof String) {
                jsonBody = (String) body;
            } else {
                jsonBody = mcpObjectMapper.writeValueAsString(body);
            }
            return HttpRequest.BodyPublishers.ofString(jsonBody);
        } catch (Exception e) {
            LOG.error("Failed to serialize request body: {}", e.getMessage());
            return HttpRequest.BodyPublishers.noBody();
        }
    }
}
