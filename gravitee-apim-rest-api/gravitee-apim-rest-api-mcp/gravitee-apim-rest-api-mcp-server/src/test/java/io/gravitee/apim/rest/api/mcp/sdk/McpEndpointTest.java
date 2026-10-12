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

import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.NOW;
import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.traffic;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.rest.api.mcp.core.model.ApiRef;
import io.gravitee.apim.rest.api.mcp.core.model.Change;
import io.gravitee.apim.rest.api.mcp.core.model.FailedRequest;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import io.gravitee.apim.rest.api.mcp.core.model.TimelinePoint;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficSample;
import io.gravitee.apim.rest.api.mcp.core.port.RequestLogs;
import io.gravitee.apim.rest.api.mcp.core.use_case.BreakDownTrafficUseCase;
import io.gravitee.apim.rest.api.mcp.core.use_case.GetHealthOverviewUseCase;
import io.gravitee.apim.rest.api.mcp.core.use_case.GetTrafficTimelineUseCase;
import io.gravitee.apim.rest.api.mcp.core.use_case.ListRecentChangesUseCase;
import io.gravitee.apim.rest.api.mcp.core.use_case.SearchFailedRequestsUseCase;
import io.gravitee.apim.rest.api.mcp.fakes.Fakes;
import io.gravitee.apim.rest.api.mcp.fakes.InMemoryTrafficAnalytics;
import io.gravitee.apim.rest.api.mcp.spi.McpCaller;
import io.gravitee.apim.rest.api.mcp.spi.McpTool;
import io.gravitee.apim.rest.api.mcp.spi.ToolDescriptor;
import io.gravitee.apim.rest.api.mcp.tools.BreakDownTrafficTool;
import io.gravitee.apim.rest.api.mcp.tools.GetHealthOverviewTool;
import io.gravitee.apim.rest.api.mcp.tools.GetTrafficTimelineTool;
import io.gravitee.apim.rest.api.mcp.tools.IncidentTriageGuide;
import io.gravitee.apim.rest.api.mcp.tools.ListRecentChangesTool;
import io.gravitee.apim.rest.api.mcp.tools.SearchFailedRequestsTool;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Talks to the mounted servlet with the SDK's own client, over HTTP.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class McpEndpointTest {

    private static final String ENDPOINT = "/mcp/organizations/DEFAULT/environments/DEFAULT";
    private static final ApiRef ORDERS = new ApiRef("orders-id", "Orders");
    private static final ObjectMapper JSON = new ObjectMapper();

    private Server server;
    private String baseUrl;
    private McpSyncClient client;

    @BeforeAll
    void start() throws Exception {
        TimeProvider.overrideClock(Clock.fixed(NOW, ZoneOffset.UTC));

        var context = new ServletContextHandler("/mcp");
        context.addFilter(new FilterHolder(authenticatedAs("api-owner")), "/*", EnumSet.allOf(DispatcherType.class));
        context.addServlet(new ServletHolder(servlet()), "/*");
        server = new Server();
        var connector = new ServerConnector(server);
        server.addConnector(connector);
        server.setHandler(context);
        server.start();
        baseUrl = "http://localhost:" + connector.getLocalPort();

        client = McpClient.sync(HttpClientStreamableHttpTransport.builder(baseUrl).endpoint(ENDPOINT).build()).build();
    }

    @AfterAll
    void stop() throws Exception {
        client.close();
        server.stop();
        TimeProvider.reset();
    }

    @Test
    void should_introduce_the_triage_loop_when_a_client_connects() {
        var initialization = client.initialize();

        assertThat(initialization.serverInfo().name()).isEqualTo("gravitee-apim");
        assertThat(initialization.instructions()).contains("Triage loop").contains("Never pass epoch values");
    }

    @Test
    void should_list_the_tools_in_triage_order_as_read_only() {
        var tools = client.listTools().tools();

        assertThat(tools)
            .extracting(Tool::name)
            .containsExactly(
                "get_health_overview",
                "break_down_traffic",
                "get_traffic_timeline",
                "search_failed_requests",
                "list_recent_changes",
                "who_am_i",
                "broken"
            );
        assertThat(tools).allSatisfy(tool -> {
            assertThat(tool.annotations().readOnlyHint()).isTrue();
            assertThat(tool.description()).isNotBlank();
            assertThat(tool.inputSchema()).containsEntry("type", "object");
            assertThat(tool.outputSchema()).containsEntry("type", "object");
        });
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
            "get_health_overview | {}",
            "break_down_traffic | {\"by\":\"application\",\"api\":\"Orders\",\"statusGroup\":\"4XX\"}",
            "get_traffic_timeline | {\"api\":\"orders-id\",\"window\":\"30m\"}",
            "search_failed_requests | {\"api\":\"Orders\",\"limit\":5}",
            "list_recent_changes | {\"api\":\"Orders\"}",
        }
    )
    void should_return_a_result_that_honors_the_tool_contract(String tool, String arguments) throws Exception {
        var result = call(tool, arguments);

        // The server validates the structured content against the output schema of the contract before answering.
        assertThat(result.isError()).as(textOf(result)).isNotEqualTo(Boolean.TRUE);
        assertThat(JSON.readTree(textOf(result))).isEqualTo(JSON.valueToTree(result.structuredContent()));
        assertThat(textOf(result)).contains("\"summary\"").doesNotContain("null");
    }

    @Test
    void should_report_times_as_iso_8601_instants() throws Exception {
        var overview = JSON.readTree(textOf(call("get_health_overview", "{}")));

        assertThat(overview.at("/window/from").asText()).isEqualTo("2026-01-31T13:45:00Z");
        assertThat(overview.at("/window/to").asText()).isEqualTo("2026-01-31T14:00:00Z");
        assertThat(overview.at("/findings/0/kind").asText()).isEqualTo("CLIENT_ERROR_RATE_UP");
    }

    @Test
    void should_answer_a_tool_error_that_names_the_fix_when_an_argument_is_invalid() throws Exception {
        var result = call("get_health_overview", "{\"window\":\"15 minutes\"}");

        assertThat(result.isError()).isTrue();
        assertThat(textOf(result)).contains("for example 15m, 2h or 1d");
    }

    @Test
    void should_run_a_tool_as_the_authenticated_user_on_the_environment_of_the_endpoint() throws Exception {
        var seen = JSON.readTree(textOf(call("who_am_i", "{}")));

        assertThat(seen.get("caller").asText()).isEqualTo("api-owner@DEFAULT/DEFAULT");
        assertThat(seen.get("threadBoundUser").asText()).isEqualTo("api-owner");
    }

    @Test
    void should_not_leak_a_server_failure_to_the_client() throws Exception {
        var result = call("broken", "{}");

        assertThat(result.isError()).isTrue();
        assertThat(textOf(result)).isEqualTo("The tool broken failed on the server. Retry, or narrow the query.");
    }

    @Test
    void should_serve_the_triage_guide() {
        assertThat(client.listResources().resources()).extracting("uri").containsExactly("gravitee://guides/incident-triage");

        var contents = client.readResource(new ReadResourceRequest("gravitee://guides/incident-triage")).contents();

        assertThat(((TextResourceContents) contents.getFirst()).text()).contains("# Incident triage on a Gravitee environment");
    }

    @ParameterizedTest
    @CsvSource({ "/mcp/", "/mcp/organizations/DEFAULT", "/mcp/organizations/DEFAULT/environments/DEFAULT/extra" })
    void should_answer_not_found_outside_an_environment_endpoint(String path) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(baseUrl + path))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .POST(HttpRequest.BodyPublishers.ofString("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}"))
            .build();

        var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(404);
    }

    private CallToolResult call(String tool, String arguments) throws Exception {
        return client.callTool(new CallToolRequest(tool, JSON.readValue(arguments, Map.class)));
    }

    private static String textOf(CallToolResult result) {
        return ((TextContent) result.content().getFirst()).text();
    }

    private static McpEndpointServlet servlet() {
        var window = TimeWindow.parse("15m", null, NOW);
        var access = new Fakes.Access();
        var apis = Fakes.apis(ORDERS);
        var analytics = new InMemoryTrafficAnalytics()
            .total(window, traffic(1000, 600, 0))
            .total(window.baseline(), traffic(1000, 5, 0))
            .samples(window, new TrafficSample("1", "Unknown", traffic(600, 600, 0)))
            .samples(window.baseline(), new TrafficSample("app-id", "Mobile app", traffic(600, 0, 0)))
            .timeline(new TimelinePoint(window.from(), traffic(100, 0, 0)), new TimelinePoint(NOW.minusSeconds(60), traffic(100, 80, 0)));
        var logs = new Fakes.InMemoryRequestLogs();
        logs.page = new RequestLogs.Page(
            600,
            List.of(new FailedRequest(NOW, ORDERS, "GET", "/orders", 401, "API_KEY_INVALID", null, null, "Unknown", null, "gw-1", 2, "id"))
        );
        var changeLog = new Fakes.InMemoryChangeLog();
        changeLog.audit.add(new Change(NOW.minusSeconds(300), Change.Source.AUDIT, "APIKEY_REVOKED", "admin", Map.of("API_KEY", "key-id")));

        return McpServerFactory.create(
            "test",
            "Triage loop. Never pass epoch values.",
            List.of(
                new GetHealthOverviewTool(new GetHealthOverviewUseCase(access, analytics)),
                new BreakDownTrafficTool(new BreakDownTrafficUseCase(access, apis, analytics)),
                new GetTrafficTimelineTool(new GetTrafficTimelineUseCase(access, apis, analytics)),
                new SearchFailedRequestsTool(new SearchFailedRequestsUseCase(access, apis, logs)),
                new ListRecentChangesTool(new ListRecentChangesUseCase(access, apis, changeLog)),
                new WhoAmI(),
                new Broken()
            ),
            List.of(new IncidentTriageGuide())
        );
    }

    /** Stands for the Spring Security chain of the mount: binds the user and the environment to the request thread. */
    private static Filter authenticatedAs(String userId) {
        return (request, response, chain) -> {
            var user = new UserDetails(userId, "", List.of());
            user.setSource("memory");
            user.setSourceId(userId);
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

    private record NoArguments() {}

    private static ToolDescriptor describe(String name) {
        var object = JSON.createObjectNode().put("type", "object");
        return new ToolDescriptor(name, name, "A tool of the test.", object, object);
    }

    private static class WhoAmI implements McpTool<NoArguments> {

        @Override
        public ToolDescriptor descriptor() {
            return describe("who_am_i");
        }

        @Override
        public Class<NoArguments> inputType() {
            return NoArguments.class;
        }

        @Override
        public Object call(McpCaller caller, NoArguments input) {
            var info = caller.auditInfo();
            return Map.of(
                "caller",
                info.actor().userId() + "@" + info.organizationId() + "/" + info.environmentId(),
                "threadBoundUser",
                SecurityContextHolder.getContext().getAuthentication().getName()
            );
        }
    }

    private static class Broken implements McpTool<NoArguments> {

        @Override
        public ToolDescriptor descriptor() {
            return describe("broken");
        }

        @Override
        public Class<NoArguments> inputType() {
            return NoArguments.class;
        }

        @Override
        public Object call(McpCaller caller, NoArguments input) {
            throw new IllegalStateException("jdbc:postgresql://internal-host:5432 refused the connection");
        }
    }
}
