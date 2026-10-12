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

import io.gravitee.apim.rest.api.mcp.security.oauth.McpProtectedResource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects any request that does not carry a bearer token.
 *
 * {@link io.gravitee.rest.api.security.filter.TokenAuthenticationFilter} falls back to the console session cookie when the
 * Authorization header is empty. Requiring the header here keeps that cookie from ever authenticating an MCP request.
 *
 * @author GraviteeSource Team
 */
public class BearerOnlyFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            challenge(request, response, null);
            return;
        }
        chain.doFilter(request, response);
    }

    public static void challenge(HttpServletRequest request, HttpServletResponse response, AuthenticationException cause)
        throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, bearerChallenge(request));
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * @param parameters such as {@code error="invalid_token"}; where OAuth protects the URL, {@code resource_metadata}
     *        follows them, telling the client where to find its authorization server (RFC 9728)
     */
    public static String bearerChallenge(HttpServletRequest request, String... parameters) {
        var all = new ArrayList<>(List.of(parameters));
        McpProtectedResource.of(request)
            .flatMap(McpProtectedResource::metadataUrl)
            .ifPresent(url -> all.add("resource_metadata=\"" + url + "\""));
        return all.isEmpty() ? "Bearer" : "Bearer " + String.join(", ", all);
    }
}
