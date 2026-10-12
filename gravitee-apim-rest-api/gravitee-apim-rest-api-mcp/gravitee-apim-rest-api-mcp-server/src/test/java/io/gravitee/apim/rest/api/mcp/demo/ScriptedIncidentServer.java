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
package io.gravitee.apim.rest.api.mcp.demo;

import io.gravitee.apim.rest.api.mcp.core.use_case.BreakDownTrafficUseCase;
import io.gravitee.apim.rest.api.mcp.core.use_case.GetHealthOverviewUseCase;
import io.gravitee.apim.rest.api.mcp.core.use_case.GetTrafficTimelineUseCase;
import io.gravitee.apim.rest.api.mcp.core.use_case.ListRecentChangesUseCase;
import io.gravitee.apim.rest.api.mcp.core.use_case.SearchFailedRequestsUseCase;
import io.gravitee.apim.rest.api.mcp.fakes.Fakes;
import io.gravitee.apim.rest.api.mcp.sdk.McpEndpointServlet;
import io.gravitee.apim.rest.api.mcp.sdk.McpServerFactory;
import io.gravitee.apim.rest.api.mcp.spi.McpTool;
import io.gravitee.apim.rest.api.mcp.spring.McpServerConfiguration;
import io.gravitee.apim.rest.api.mcp.tools.BreakDownTrafficTool;
import io.gravitee.apim.rest.api.mcp.tools.GetHealthOverviewTool;
import io.gravitee.apim.rest.api.mcp.tools.GetTrafficTimelineTool;
import io.gravitee.apim.rest.api.mcp.tools.IncidentTriageGuide;
import io.gravitee.apim.rest.api.mcp.tools.ListRecentChangesTool;
import io.gravitee.apim.rest.api.mcp.tools.SearchFailedRequestsTool;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Serves the real tools, contracts and protocol layer over a {@link ScriptedIncident}, on localhost and without
 * authentication: a way to try the server with an MCP client, or to run the conformance suite, with no APIM stack.
 *
 * <pre>
 * mvn -pl gravitee-apim-rest-api/gravitee-apim-rest-api-mcp/gravitee-apim-rest-api-mcp-server test-compile exec:java \
 *   -Dexec.classpathScope=test -Dexec.mainClass=io.gravitee.apim.rest.api.mcp.demo.ScriptedIncidentServer
 * </pre>
 *
 * Arguments: the port (default 18084) and how many minutes ago the key was revoked (default 6).
 */
public final class ScriptedIncidentServer {

    public static final String ENDPOINT = "/mcp";

    private ScriptedIncidentServer() {}

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 18084;
        long minutesAgo = args.length > 1 ? Long.parseLong(args[1]) : 6;
        var incident = new ScriptedIncident(Instant.now().minus(Duration.ofMinutes(minutesAgo)));

        var server = start("127.0.0.1", port, servlet(incident, List.of()));
        System.out.println("MCP endpoint: http://localhost:" + port + ENDPOINT);
        server.join();
    }

    /** The real server over the incident, plus any tools a test adds. */
    public static McpEndpointServlet servlet(ScriptedIncident incident, List<McpTool<?>> extraTools) throws IOException {
        var access = new Fakes.Access();
        var tools = new ArrayList<McpTool<?>>(
            List.of(
                new GetHealthOverviewTool(new GetHealthOverviewUseCase(incident, incident)),
                new BreakDownTrafficTool(new BreakDownTrafficUseCase(incident, incident, incident)),
                new GetTrafficTimelineTool(new GetTrafficTimelineUseCase(incident, incident, incident)),
                new SearchFailedRequestsTool(new SearchFailedRequestsUseCase(incident, incident, incident)),
                new ListRecentChangesTool(new ListRecentChangesUseCase(incident, access, incident, incident))
            )
        );
        tools.addAll(extraTools);
        return McpServerFactory.create("scripted", instructions(), tools, List.of(new IncidentTriageGuide()));
    }

    /** Mounts the servlet at {@link #ENDPOINT} behind a filter that stands for the security chain. Port 0 picks a free one. */
    public static Server start(String host, int port, McpEndpointServlet servlet) throws Exception {
        var context = new ServletContextHandler(ENDPOINT);
        context.setAllowNullPathInContext(true);
        context.addFilter(new FilterHolder(asUser("demo", "DEFAULT")), "/*", EnumSet.allOf(DispatcherType.class));
        context.addServlet(new ServletHolder(servlet), "/*");
        var server = new Server();
        var connector = new ServerConnector(server);
        connector.setHost(host);
        connector.setPort(port);
        server.addConnector(connector);
        server.setHandler(context);
        server.start();
        return server;
    }

    private static String instructions() throws IOException {
        try (var stream = McpServerConfiguration.class.getResourceAsStream("/mcp/instructions.md")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** What the security chain of the mount does: binds the user and his organization to the request thread. */
    private static Filter asUser(String userId, String organizationId) {
        return (request, response, chain) -> {
            var user = new UserDetails(userId, "", List.of());
            user.setSource("memory");
            user.setSourceId(userId);
            user.setOrganizationId(organizationId);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
            GraviteeContext.fromExecutionContext(new ExecutionContext(organizationId));
            try {
                chain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
    }
}
