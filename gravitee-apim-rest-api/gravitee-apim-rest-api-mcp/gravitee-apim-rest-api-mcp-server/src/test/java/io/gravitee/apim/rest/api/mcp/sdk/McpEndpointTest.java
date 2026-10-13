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

import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.MCP_CALLER;
import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.NOW;
import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.PROD;
import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.STAGING;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.analytics_engine.model.FacetBucketResponse;
import io.gravitee.apim.core.analytics_engine.model.FacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.Measure;
import io.gravitee.apim.core.analytics_engine.model.MeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricFacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesBucketResponse;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesMetricResponse;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesResponse;
import io.gravitee.apim.core.analytics_engine.use_case.GetApiMetricSpecUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetApiSpecUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetMetricFacetSpecUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetMetricFilterSpecUseCase;
import io.gravitee.apim.core.logs_engine.model.ApiLog;
import io.gravitee.apim.core.logs_engine.model.Pagination;
import io.gravitee.apim.core.logs_engine.model.SearchLogsResponse;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import io.gravitee.apim.rest.api.mcp.core.model.ApiRef;
import io.gravitee.apim.rest.api.mcp.core.model.Change;
import io.gravitee.apim.rest.api.mcp.core.use_case.DescribeMetricsUseCase;
import io.gravitee.apim.rest.api.mcp.core.use_case.ListApisUseCase;
import io.gravitee.apim.rest.api.mcp.core.use_case.ListChangesUseCase;
import io.gravitee.apim.rest.api.mcp.core.use_case.ListEnvironmentsUseCase;
import io.gravitee.apim.rest.api.mcp.core.use_case.ListFilterValuesUseCase;
import io.gravitee.apim.rest.api.mcp.core.use_case.QueryAnalyticsUseCase;
import io.gravitee.apim.rest.api.mcp.core.use_case.SearchLogsUseCase;
import io.gravitee.apim.rest.api.mcp.fakes.Fakes;
import io.gravitee.apim.rest.api.mcp.infra.DefinitionAnalyticsCatalog;
import io.gravitee.apim.rest.api.mcp.spi.McpCaller;
import io.gravitee.apim.rest.api.mcp.spi.McpTool;
import io.gravitee.apim.rest.api.mcp.spi.ToolDescriptor;
import io.gravitee.apim.rest.api.mcp.tools.DescribeMetricsTool;
import io.gravitee.apim.rest.api.mcp.tools.IncidentTriageGuide;
import io.gravitee.apim.rest.api.mcp.tools.ListApisTool;
import io.gravitee.apim.rest.api.mcp.tools.ListChangesTool;
import io.gravitee.apim.rest.api.mcp.tools.ListEnvironmentsTool;
import io.gravitee.apim.rest.api.mcp.tools.ListFilterValuesTool;
import io.gravitee.apim.rest.api.mcp.tools.QueryAnalyticsTool;
import io.gravitee.apim.rest.api.mcp.tools.SearchLogsTool;
import io.gravitee.apim.rest.api.mcp.tools.TriagePrompt;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
 * Talks to the mounted servlet with the SDK's own client, over HTTP: the real catalog, stub engines.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class McpEndpointTest {

    private static final String ENDPOINT = "/mcp";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ApiRef ORDERS = new ApiRef("orders-id", "Orders");

    private Server server;
    private String baseUrl;
    private McpSyncClient client;

    @BeforeAll
    void start() throws Exception {
        TimeProvider.overrideClock(Clock.fixed(NOW, ZoneOffset.UTC));
        var context = new ServletContextHandler(ENDPOINT);
        context.setAllowNullPathInContext(true);
        context.addFilter(new FilterHolder(asUser("demo", "DEFAULT")), "/*", EnumSet.allOf(DispatcherType.class));
        context.addServlet(new ServletHolder(servlet()), "/*");
        server = new Server();
        var connector = new ServerConnector(server);
        connector.setHost("localhost");
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
    void should_introduce_the_server_when_a_client_connects() {
        var initialization = client.initialize();

        assertThat(initialization.serverInfo().name()).isEqualTo("gravitee-apim");
        assertThat(initialization.instructions()).contains("what they mean is yours to judge").contains("Never pass epoch values");
    }

    @Test
    void should_list_the_tools_handles_first_as_read_only() {
        var tools = client.listTools().tools();

        assertThat(tools)
            .extracting(Tool::name)
            .containsExactly(
                "list_environments",
                "list_apis",
                "describe_metrics",
                "query_analytics",
                "list_filter_values",
                "search_logs",
                "list_changes",
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
            "list_environments | {}",
            "list_apis | {}",
            "list_apis | {\"environment\":\"staging\",\"query\":\"sto\"}",
            "describe_metrics | {}",
            "describe_metrics | {\"apiType\":\"LLM\"}",
            "describe_metrics | {\"metric\":\"HTTP_REQUESTS\"}",
            "query_analytics | {\"environment\":\"prod\",\"mode\":\"measures\",\"metrics\":[{\"name\":\"HTTP_REQUESTS\",\"measures\":[\"COUNT\"]}],\"compareWith\":\"previous\"}",
            "query_analytics | {\"environment\":\"prod\",\"mode\":\"facets\",\"by\":[\"API\",\"HTTP_STATUS_CODE_GROUP\"],\"metrics\":[{\"name\":\"HTTP_REQUESTS\",\"measures\":[\"COUNT\"]}],\"filters\":[{\"name\":\"API\",\"operator\":\"EQ\",\"value\":\"Orders\"}],\"limit\":5}",
            "query_analytics | {\"environment\":\"prod\",\"mode\":\"time_series\",\"interval\":\"2m\",\"metrics\":[{\"name\":\"HTTP_GATEWAY_RESPONSE_TIME\",\"measures\":[\"P95\"]}],\"window\":\"30m\"}",
            "list_filter_values | {\"environment\":\"prod\",\"filter\":\"APPLICATION\"}",
            "search_logs | {\"environment\":\"prod\",\"filters\":[{\"name\":\"HTTP_STATUS\",\"operator\":\"GTE\",\"value\":400}],\"perPage\":5}",
            "list_changes | {\"api\":\"Orders\"}",
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
    void should_describe_the_shipped_catalog() throws Exception {
        var catalog = JSON.readTree(textOf(call("describe_metrics", "{\"metric\":\"HTTP_REQUESTS\"}")));

        assertThat(catalog.at("/metric/metric/unit").asText()).isEqualTo("NUMBER");
        assertThat(catalog.at("/metric/facets").findValuesAsText("name")).contains("API", "HTTP_STATUS_CODE_GROUP", "CONSUMER_IP");
    }

    @Test
    void should_return_the_numbers_of_both_ranges_as_they_are() throws Exception {
        var result = JSON.readTree(
            textOf(
                call(
                    "query_analytics",
                    "{\"environment\":\"prod\",\"mode\":\"measures\",\"metrics\":[{\"name\":\"HTTP_REQUESTS\",\"measures\":[\"COUNT\"]}],\"compareWith\":\"previous\"}"
                )
            )
        );

        assertThat(result.at("/window/from").asText()).isEqualTo("2026-01-31T13:00:00Z");
        assertThat(result.at("/previousWindow/from").asText()).isEqualTo("2026-01-31T12:00:00Z");
        assertThat(result.at("/current/0/measures/COUNT").asInt()).isEqualTo(1200);
        assertThat(result.at("/previous/0/measures/COUNT").asInt()).isEqualTo(1200);
        assertThat(result.at("/environment/hrid").asText()).isEqualTo("prod");
    }

    @Test
    void should_answer_a_tool_error_that_names_the_fix_when_an_argument_is_invalid() throws Exception {
        var result = call(
            "query_analytics",
            "{\"environment\":\"prod\",\"mode\":\"measures\",\"metrics\":[{\"name\":\"HTTP_REQUESTS\",\"measures\":[\"COUNT\"]}],\"window\":\"1769868000000\"}"
        );

        assertThat(result.isError()).isTrue();
        assertThat(textOf(result)).contains("for example 15m, 2h or 1d");
    }

    @Test
    void should_ask_for_the_environment_when_several_are_readable_and_nothing_names_one() throws Exception {
        var result = call("search_logs", "{}");

        assertThat(result.isError()).isTrue();
        assertThat(textOf(result)).isEqualTo("Pass environment: the id, hrid or name of one of Production (prod), Staging (staging).");
    }

    @Test
    void should_run_a_tool_as_the_authenticated_user_of_his_organization() throws Exception {
        var seen = JSON.readTree(textOf(call("who_am_i", "{}")));

        assertThat(seen.get("caller").asText()).isEqualTo("demo@DEFAULT");
        assertThat(seen.get("threadBoundUser").asText()).isEqualTo("demo");
    }

    @Test
    void should_not_leak_a_server_failure_to_the_client() throws Exception {
        var result = call("broken", "{}");

        assertThat(result.isError()).isTrue();
        assertThat(textOf(result)).isEqualTo("The tool broken failed on the server. Retry, or narrow the query.");
    }

    @Test
    void should_serve_the_guide_and_the_triage_prompt_built_on_it() {
        assertThat(client.listResources().resources()).extracting("uri").containsExactly("gravitee://guides/incident-triage");
        var guide = ((TextResourceContents) client
                .readResource(new ReadResourceRequest("gravitee://guides/incident-triage"))
                .contents()
                .getFirst()).text();
        assertThat(guide).startsWith("# Looking for trouble on a Gravitee organization").contains("A method, not a rule.");

        assertThat(client.listPrompts().prompts()).extracting("name").containsExactly("triage");
        var prompt = client.getPrompt(new GetPromptRequest("triage", Map.of("environment", "prod", "api", "Orders")));

        assertThat(prompt.messages()).hasSize(1);
        var text = ((TextContent) prompt.messages().getFirst().content()).text();
        assertThat(text).startsWith("Look for trouble in the environment prod, focusing on the API Orders.").contains(guide);
    }

    @ParameterizedTest
    @CsvSource({ "/mcp, 200", "/mcp/, 200", "/mcp/organizations/DEFAULT, 404", "/mcp/environments/DEFAULT, 404" })
    void should_serve_the_mount_itself_and_nothing_under_it(String path, int status) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(baseUrl + path))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .POST(HttpRequest.BodyPublishers.ofString("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}"))
            .build();

        var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(status);
    }

    private CallToolResult call(String tool, String arguments) throws Exception {
        return client.callTool(new CallToolRequest(tool, JSON.readValue(arguments, Map.class)));
    }

    private static String textOf(CallToolResult result) {
        return ((TextContent) result.content().getFirst()).text();
    }

    private static McpEndpointServlet servlet() throws Exception {
        var environments = Fakes.environments(PROD, STAGING);
        environments.unreadable.add(new io.gravitee.apim.rest.api.mcp.core.model.EnvRef("e-dev", "dev", "Dev"));
        var apis = Fakes.apisByEnvironment(
            Map.of(PROD.id(), List.of(ORDERS, new ApiRef("stock-id", "Stock")), STAGING.id(), List.of(new ApiRef("stock-staging", "Stock")))
        );
        var access = new Fakes.Access();

        var definition = new AnalyticsDefinitionYAMLQueryService();
        var catalog = new DefinitionAnalyticsCatalog(
            new GetApiSpecUseCase(definition),
            new GetApiMetricSpecUseCase(definition),
            new GetMetricFacetSpecUseCase(definition),
            new GetMetricFilterSpecUseCase(definition)
        );

        var analytics = new Fakes.StubAnalyticsQueries();
        analytics.measures = new MeasuresResponse(
            List.of(
                new MetricMeasuresResponse(
                    MetricSpec.Name.HTTP_REQUESTS,
                    MetricSpec.Unit.NUMBER,
                    List.of(new Measure(MetricSpec.Measure.COUNT, 1200))
                )
            )
        );
        analytics.facets = new FacetsResponse(
            List.of(
                new MetricFacetsResponse(
                    MetricSpec.Name.HTTP_REQUESTS,
                    MetricSpec.Unit.NUMBER,
                    List.of(
                        new FacetBucketResponse(
                            ORDERS.id(),
                            ORDERS.name(),
                            List.of(
                                new FacetBucketResponse(
                                    "400-499",
                                    "4xx Client Error",
                                    null,
                                    List.of(new Measure(MetricSpec.Measure.COUNT, 30))
                                )
                            ),
                            null
                        )
                    )
                )
            )
        );
        analytics.timeSeries = new TimeSeriesResponse(
            List.of(
                new TimeSeriesMetricResponse(
                    MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME,
                    MetricSpec.Unit.MILLISECONDS,
                    List.of(
                        new TimeSeriesBucketResponse(
                            "t",
                            null,
                            NOW.minusSeconds(1800).toEpochMilli(),
                            null,
                            List.of(new Measure(MetricSpec.Measure.P95, 48.0))
                        )
                    )
                )
            )
        );

        var logs = new Fakes.StubLogSearch();
        logs.response = new SearchLogsResponse(
            List.of(
                ApiLog.builder()
                    .apiId(ORDERS.id())
                    .apiName(ORDERS.name())
                    .timestamp(NOW.atOffset(ZoneOffset.UTC))
                    .status(401)
                    .errorKey("API_KEY_INVALID")
                    .build()
            ),
            new Pagination(1, 5, 1, 1, 600L)
        );

        var changeLog = new Fakes.InMemoryChangeLog();
        changeLog.audit.add(new Change(NOW.minusSeconds(300), Change.Source.AUDIT, "APIKEY_REVOKED", "admin", Map.of("API_KEY", "key-id")));

        var guide = new IncidentTriageGuide();
        var instructions = new String(
            McpEndpointTest.class.getResourceAsStream("/mcp/instructions.md").readAllBytes(),
            StandardCharsets.UTF_8
        );
        return McpServerFactory.create(
            "test",
            instructions,
            List.of(
                new ListEnvironmentsTool(new ListEnvironmentsUseCase(environments, apis)),
                new ListApisTool(new ListApisUseCase(environments, apis)),
                new DescribeMetricsTool(new DescribeMetricsUseCase(catalog)),
                new QueryAnalyticsTool(new QueryAnalyticsUseCase(environments, apis, analytics)),
                new ListFilterValuesTool(new ListFilterValuesUseCase(environments, apis, analytics)),
                new SearchLogsTool(new SearchLogsUseCase(environments, apis, logs)),
                new ListChangesTool(new ListChangesUseCase(environments, access, apis, changeLog)),
                new WhoAmI(),
                new Broken()
            ),
            List.of(guide),
            List.of(new TriagePrompt(guide))
        );
    }

    /** Stands for the security chain of the mount: binds the user and his organization to the request thread. */
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
            return Map.of(
                "caller",
                caller.userId() + "@" + caller.organizationId(),
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
