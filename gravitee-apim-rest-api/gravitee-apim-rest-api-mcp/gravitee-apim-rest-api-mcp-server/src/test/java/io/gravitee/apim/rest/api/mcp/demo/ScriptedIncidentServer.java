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
import io.gravitee.apim.rest.api.mcp.sdk.McpServerFactory;
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
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
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

    private ScriptedIncidentServer() {}

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 18084;
        long minutesAgo = args.length > 1 ? Long.parseLong(args[1]) : 6;
        var incident = new ScriptedIncident(Instant.now().minus(Duration.ofMinutes(minutesAgo)));
        var access = new Fakes.Access();

        var servlet = McpServerFactory.create(
            "scripted",
            instructions(),
            List.of(
                new GetHealthOverviewTool(new GetHealthOverviewUseCase(access, incident)),
                new BreakDownTrafficTool(new BreakDownTrafficUseCase(access, incident, incident)),
                new GetTrafficTimelineTool(new GetTrafficTimelineUseCase(access, incident, incident)),
                new SearchFailedRequestsTool(new SearchFailedRequestsUseCase(access, incident, incident)),
                new ListRecentChangesTool(new ListRecentChangesUseCase(access, incident, incident))
            ),
            List.of(new IncidentTriageGuide())
        );

        var context = new ServletContextHandler("/mcp");
        context.addFilter(new FilterHolder(asDemoUser()), "/*", EnumSet.allOf(DispatcherType.class));
        context.addServlet(new ServletHolder(servlet), "/*");
        var server = new Server();
        var connector = new ServerConnector(server);
        connector.setHost("127.0.0.1");
        connector.setPort(port);
        server.addConnector(connector);
        server.setHandler(context);
        server.start();
        System.out.println("MCP endpoint: http://localhost:" + port + "/mcp/organizations/DEFAULT/environments/DEFAULT");
        server.join();
    }

    private static String instructions() throws java.io.IOException {
        try (var stream = McpServerConfiguration.class.getResourceAsStream("/mcp/instructions.md")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Filter asDemoUser() {
        return (request, response, chain) -> {
            var user = new UserDetails("demo", "", List.of());
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
            var path = ((HttpServletRequest) request).getPathInfo();
            GraviteeContext.fromExecutionContext(
                path != null && path.contains("/environments/") ? new ExecutionContext("DEFAULT", "DEFAULT") : new ExecutionContext()
            );
            try {
                chain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
    }
}
