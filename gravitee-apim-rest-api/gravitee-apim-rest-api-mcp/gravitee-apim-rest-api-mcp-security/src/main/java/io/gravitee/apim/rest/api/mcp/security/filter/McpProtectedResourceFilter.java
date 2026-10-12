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
package io.gravitee.apim.rest.api.mcp.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.rest.api.mcp.security.oauth.IdentityProviderException;
import io.gravitee.apim.rest.api.mcp.security.oauth.IdentityProviderTokenIntrospector;
import io.gravitee.apim.rest.api.mcp.security.oauth.McpAuthorizationServerResolver;
import io.gravitee.apim.rest.api.mcp.security.oauth.McpMount;
import io.gravitee.apim.rest.api.mcp.security.oauth.McpMountResolver;
import io.gravitee.apim.rest.api.mcp.security.oauth.McpProtectedResource;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import lombok.CustomLog;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * First on the MCP mount: binds the organization of the URL to the request, and serves the RFC 9728 metadata that tells
 * an OAuth client which authorization server issues tokens for it, without authentication.
 *
 * @author GraviteeSource Team
 */
@CustomLog
public class McpProtectedResourceFilter extends OncePerRequestFilter {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final McpMountResolver mounts;
    private final McpAuthorizationServerResolver authorizationServers;
    private final IdentityProviderTokenIntrospector introspector;

    public McpProtectedResourceFilter(
        McpMountResolver mounts,
        McpAuthorizationServerResolver authorizationServers,
        IdentityProviderTokenIntrospector introspector
    ) {
        this.mounts = mounts;
        this.authorizationServers = authorizationServers;
        this.introspector = introspector;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        try {
            var path = request.getRequestURI().substring(request.getContextPath().length());
            var metadata = path.endsWith(McpMount.METADATA_PATH);
            var mount = mounts.resolve(
                metadata ? path.substring(0, path.length() - McpMount.METADATA_PATH.length()) : path,
                request.getServerName(),
                request.getServerPort()
            );
            if (mount.isEmpty()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            var resource = new McpProtectedResource(mount.get(), () -> authorizationServers.resolve(mount.get().organizationId()));
            resource.bindTo(request);
            if (metadata) {
                serveMetadata(request, response, resource);
                return;
            }
            chain.doFilter(request, response);
        } finally {
            // Parameters are memoized on the request thread's context, which nothing else clears on a rejected request.
            GraviteeContext.cleanContext();
        }
    }

    private void serveMetadata(HttpServletRequest request, HttpServletResponse response, McpProtectedResource resource) throws IOException {
        if (!HttpMethod.GET.matches(request.getMethod())) {
            response.setHeader(HttpHeaders.ALLOW, HttpMethod.GET.name());
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        var authorizationServer = resource.authorizationServer();
        if (authorizationServer.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String issuer;
        try {
            issuer = introspector.issuer(authorizationServer.get().identityProvider());
        } catch (IdentityProviderException e) {
            log.warn(
                "Cannot serve the MCP protected resource metadata of organization {}: {}",
                resource.mount().organizationId(),
                e.getMessage()
            );
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
        var document = new LinkedHashMap<String, Object>();
        document.put("resource", resource.mount().resource());
        document.put("authorization_servers", List.of(issuer));
        document.put("bearer_methods_supported", List.of("header"));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        JSON.writeValue(response.getOutputStream(), document);
    }
}
