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
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.rest.api.mcp.demo.ScriptedIncident;
import io.gravitee.apim.rest.api.mcp.demo.ScriptedIncidentServer;
import io.gravitee.apim.rest.api.mcp.spi.McpCaller;
import io.gravitee.apim.rest.api.mcp.spi.McpTool;
import io.gravitee.apim.rest.api.mcp.spi.ToolDescriptor;
import io.gravitee.common.utils.TimeProvider;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Talks to the mounted servlet with the SDK's own client, over HTTP, about the scripted incident.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class McpEndpointTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private Server server;
    private String baseUrl;
    private McpSyncClient client;

    @BeforeAll
    void start() throws Exception {
        TimeProvider.overrideClock(Clock.fixed(NOW, ZoneOffset.UTC));
        var incident = new ScriptedIncident(NOW.minus(Duration.ofMinutes(5)));
        server = ScriptedIncidentServer.start(
            "localhost",
            0,
            ScriptedIncidentServer.servlet(incident, List.of(new WhoAmI(), new Broken()))
        );
        baseUrl = "http://localhost:" + ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        client = McpClient.sync(
            HttpClientStreamableHttpTransport.builder(baseUrl).endpoint(ScriptedIncidentServer.ENDPOINT).build()
        ).build();
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
        assertThat(tools.subList(0, 5)).allSatisfy(tool ->
            assertThat(((Map<?, ?>) tool.inputSchema().get("properties")).containsKey("environment")).isTrue()
        );
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
            "get_health_overview | {}",
            "get_health_overview | {\"environment\":\"staging\"}",
            "break_down_traffic | {\"by\":\"application\",\"api\":\"Orders\",\"statusGroup\":\"4XX\"}",
            "break_down_traffic | {\"by\":\"status\",\"environment\":\"staging\"}",
            "get_traffic_timeline | {\"api\":\"0f3c9a52-orders\",\"window\":\"30m\"}",
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
    void should_cover_every_readable_environment_and_name_the_degraded_one_first() throws Exception {
        var overview = JSON.readTree(textOf(call("get_health_overview", "{}")));

        assertThat(overview.get("status").asText()).isEqualTo("DEGRADED");
        assertThat(overview.at("/environments/0/environment/hrid").asText()).isEqualTo("prod");
        assertThat(overview.at("/environments/0/status").asText()).isEqualTo("DEGRADED");
        assertThat(overview.at("/environments/1/environment/hrid").asText()).isEqualTo("staging");
        assertThat(overview.at("/environments/1/status").asText()).isEqualTo("HEALTHY");
        assertThat(overview.at("/omittedEnvironments/0/hrid").asText()).isEqualTo("dev");
        assertThat(overview.at("/findings/0/kind").asText()).isEqualTo("CLIENT_ERROR_RATE_UP");
        assertThat(overview.at("/findings/0/environment/hrid").asText()).isEqualTo("prod");
        assertThat(overview.at("/window/from").asText()).isEqualTo("2026-01-31T13:45:00Z");
        assertThat(overview.at("/window/to").asText()).isEqualTo("2026-01-31T14:00:00Z");
    }

    @Test
    void should_find_the_environment_of_an_api_on_its_own() throws Exception {
        var changes = JSON.readTree(textOf(call("list_recent_changes", "{\"api\":\"Orders\"}")));

        assertThat(changes.at("/environment/hrid").asText()).isEqualTo("prod");
        assertThat(changes.at("/changes/0/event").asText()).isEqualTo("APIKEY_REVOKED");
    }

    @Test
    void should_ask_for_the_environment_when_an_api_exists_in_several() throws Exception {
        var result = call("get_traffic_timeline", "{\"api\":\"Stock\"}");

        assertThat(result.isError()).isTrue();
        assertThat(textOf(result)).isEqualTo(
            "Several environments have an API 'Stock': Production (prod), Staging (staging). Pass environment."
        );
    }

    @Test
    void should_answer_a_tool_error_that_names_the_fix_when_an_argument_is_invalid() throws Exception {
        var result = call("get_health_overview", "{\"window\":\"15 minutes\"}");

        assertThat(result.isError()).isTrue();
        assertThat(textOf(result)).contains("for example 15m, 2h or 1d");
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
    void should_serve_the_triage_guide() {
        assertThat(client.listResources().resources()).extracting("uri").containsExactly("gravitee://guides/incident-triage");

        var contents = client.readResource(new ReadResourceRequest("gravitee://guides/incident-triage")).contents();

        assertThat(((TextResourceContents) contents.getFirst()).text()).contains("# Incident triage on a Gravitee organization");
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
