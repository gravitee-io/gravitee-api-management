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

import io.gravitee.rest.api.service.common.GraviteeContext;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * The MCP endpoint of one environment: {@code /organizations/{orgId}/environments/{envId}}.
 *
 * Anything else under the mount is not an endpoint, so that no request is served without a resolved environment.
 */
public class McpEndpointServlet extends HttpServlet {

    private static final Pattern ENDPOINT = Pattern.compile("^/organizations/[^/]+/environments/[^/]+/?$");

    private final transient HttpServletStatelessServerTransport transport;

    McpEndpointServlet(HttpServletStatelessServerTransport transport) {
        this.transport = transport;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        transport.init(config);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            var path = request.getPathInfo();
            if (path == null || !ENDPOINT.matcher(path).matches() || !GraviteeContext.getExecutionContext().hasEnvironmentId()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            transport.service(request, response);
        } finally {
            GraviteeContext.cleanContext();
        }
    }

    @Override
    public void destroy() {
        transport.destroy();
        super.destroy();
    }
}
