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
package io.gravitee.apim.rest.api.mcp.security.oauth;

import com.google.common.base.Suppliers;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * What protects the MCP URL a request was sent to, bound to the request by the first filter of the chain.
 *
 * <p>The authorization server is looked up on first use, so a request carrying an APIM token never pays for it.
 */
public final class McpProtectedResource {

    private static final String ATTRIBUTE = McpProtectedResource.class.getName();

    private final McpMount mount;
    private final Supplier<Optional<McpAuthorizationServer>> authorizationServer;

    public McpProtectedResource(McpMount mount, Supplier<Optional<McpAuthorizationServer>> authorizationServer) {
        this.mount = mount;
        this.authorizationServer = Suppliers.memoize(authorizationServer::get);
    }

    public static Optional<McpProtectedResource> of(HttpServletRequest request) {
        return Optional.ofNullable((McpProtectedResource) request.getAttribute(ATTRIBUTE));
    }

    public void bindTo(HttpServletRequest request) {
        request.setAttribute(ATTRIBUTE, this);
    }

    public McpMount mount() {
        return mount;
    }

    /** Present when OAuth protects this URL: the settings name a usable identity provider and the URL is known. */
    public Optional<McpAuthorizationServer> authorizationServer() {
        return mount.resource() == null ? Optional.empty() : authorizationServer.get();
    }

    public Optional<String> metadataUrl() {
        return authorizationServer().map(server -> mount.metadataUrl());
    }
}
