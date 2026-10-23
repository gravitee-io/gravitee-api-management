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
package io.gravitee.apim.integration.tests.http.mcp;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.configurePlans;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.entrypoint.mcp.MCPEntrypointConnectorFactory;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.keyless.KeylessPolicy;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * Calls MCP tools through a real gateway and checks both sides: the request the backend receives and the JSON-RPC
 * result the MCP client gets back.
 */
@GatewayTest
@DeployApi("/apis/v4/http/mcp/mcp-tool-server.json")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class McpToolCallIntegrationTest extends AbstractGatewayTest {

    private static final String MCP_URI = "/mcp-tool-server/mcp";
    private static final String FORM_URLENCODED = "application/x-www-form-urlencoded";

    @Override
    public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
        if (isV4Api(definitionClass)) {
            configurePlans((Api) api.getDefinition(), Set.of("key-less"));
        }
    }

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.put("key-less", PolicyBuilder.build("key-less", KeylessPolicy.class));
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("mcp", EntrypointBuilder.build("mcp", MCPEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
    }

    @Test
    void should_send_a_form_urlencoded_body_to_the_backend_for_a_form_tool(HttpClient client) {
        wiremock.stubFor(post("/endpoint/users").willReturn(okJson("{\"id\":\"1\"}")));

        JsonObject result = callTool(client, 1, "createUserWithForm", "{\"bodySchema\":{\"name\":\"Jane Doe\",\"age\":42}}");

        wiremock.verify(
            postRequestedFor(urlPathEqualTo("/endpoint/users"))
                .withHeader("Content-Type", equalTo(FORM_URLENCODED))
                .withRequestBody(equalTo("name=Jane+Doe&age=42"))
        );
        assertThat(result.getBoolean("isError")).isFalse();
    }

    @Test
    void should_send_a_json_body_unchanged_for_a_json_tool(HttpClient client) {
        wiremock.stubFor(post("/endpoint/users").willReturn(okJson("{\"id\":\"1\"}")));

        callTool(client, 2, "createUserWithJson", "{\"bodySchema\":{\"name\":\"Jane Doe\",\"age\":42}}");

        wiremock.verify(
            postRequestedFor(urlPathEqualTo("/endpoint/users"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson("{\"name\":\"Jane Doe\",\"age\":42}"))
        );
    }

    @Test
    void should_return_structured_content_when_the_backend_succeeds(HttpClient client) {
        wiremock.stubFor(get("/endpoint/users/1").willReturn(okJson("{\"name\":\"Jane Doe\"}")));

        JsonObject result = callTool(client, 3, "getUser", "{\"id\":\"1\"}");

        assertThat(result.getBoolean("isError")).isFalse();
        assertThat(result.getJsonObject("structuredContent")).isEqualTo(new JsonObject("{\"bodySchema\":{\"name\":\"Jane Doe\"}}"));
    }

    @Test
    void should_report_a_backend_client_error_as_a_tool_error_without_structured_content(HttpClient client) {
        wiremock.stubFor(get("/endpoint/users/404").willReturn(aResponse().withStatus(404).withBody("{\"message\":\"User not found\"}")));

        JsonObject result = callTool(client, 4, "getUser", "{\"id\":\"404\"}");

        assertThat(result.getBoolean("isError")).isTrue();
        assertThat(result.containsKey("structuredContent")).isFalse();
        assertThat(textOf(result)).isEqualTo("{\"message\":\"User not found\"}");
    }

    @Test
    void should_report_a_backend_error_without_body_with_its_status(HttpClient client) {
        wiremock.stubFor(get("/endpoint/users/gone").willReturn(aResponse().withStatus(404)));

        JsonObject result = callTool(client, 5, "getUser", "{\"id\":\"gone\"}");

        assertThat(result.getBoolean("isError")).isTrue();
        assertThat(textOf(result)).isEqualTo("HTTP 404");
    }

    @Test
    void should_report_a_backend_error_that_is_not_json_as_text(HttpClient client) {
        wiremock.stubFor(get("/endpoint/users/broken").willReturn(aResponse().withStatus(502).withBody("<html>Bad Gateway</html>")));

        JsonObject result = callTool(client, 6, "getUser", "{\"id\":\"broken\"}");

        assertThat(result.getBoolean("isError")).isTrue();
        assertThat(textOf(result)).isEqualTo("<html>Bad Gateway</html>");
    }

    /** Sends a tools/call request and returns its JSON-RPC result, after checking the response answers that request. */
    private static JsonObject callTool(HttpClient client, int requestId, String toolName, String arguments) {
        String request =
            "{\"jsonrpc\":\"2.0\",\"id\":" +
            requestId +
            ",\"method\":\"tools/call\",\"params\":{\"name\":\"" +
            toolName +
            "\",\"arguments\":" +
            arguments +
            "}}";
        JsonObject body = client
            .rxRequest(HttpMethod.POST, MCP_URI)
            .flatMap(httpRequest ->
                httpRequest
                    .putHeader("Accept", "application/json, text/event-stream")
                    .putHeader("Content-Type", "application/json")
                    .rxSend(request)
            )
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.body();
            })
            .map(buffer -> new JsonObject(buffer.toString()))
            .timeout(10, SECONDS)
            .blockingGet();

        assertThat(body.getInteger("id")).isEqualTo(requestId);
        return body.getJsonObject("result");
    }

    private static String textOf(JsonObject result) {
        return result.getJsonArray("content").getJsonObject(0).getString("text");
    }
}
