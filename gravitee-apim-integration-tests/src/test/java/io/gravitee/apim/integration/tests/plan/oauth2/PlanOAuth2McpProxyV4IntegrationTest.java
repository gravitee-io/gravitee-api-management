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
package io.gravitee.apim.integration.tests.plan.oauth2;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.OAUTH2_CLIENT_ID;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.OAUTH2_SUCCESS_TOKEN;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.OAUTH2_UNAUTHORIZED_TOKEN;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.PLAN_OAUTH2_ID;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.configurePlans;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.createSubscription;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.getApiPath;
import static io.vertx.core.http.HttpMethod.GET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graviteesource.endpoint.mcp_proxy.MCPProxyEndpointConnectorFactory;
import com.graviteesource.entrypoint.mcp_proxy.MCPProxyEntrypointConnectorFactory;
import com.graviteesource.reactor.mcp_proxy.MCPProxyApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.parameters.GatewayDynamicConfig;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.gateway.tests.sdk.resource.ResourceBuilder;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.reactive.api.policy.SecurityToken;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.resource.ResourcePlugin;
import io.gravitee.policy.oauth2.Oauth2Policy;
import io.gravitee.policy.oauth2.configuration.OAuth2PolicyConfiguration;
import io.gravitee.resource.oauth2.api.OAuth2ResourceMetadata;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.OngoingStubbing;

/**
 * Integration tests for OAuth2 plan on MCP APIs.
 *
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployApi("/apis/plan/v4-mcp-proxy-api.json")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class PlanOAuth2McpProxyV4IntegrationTest extends AbstractGatewayTest {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String API_ID = "v4-mcp-proxy-api";

    @Override
    public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
        reactors.add(ReactorBuilder.build(MCPProxyApiReactorFactory.class));
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("mcp-proxy", EntrypointBuilder.build("mcp-proxy", MCPProxyEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("mcp-proxy", EndpointBuilder.build("mcp-proxy", MCPProxyEndpointConnectorFactory.class));
    }

    @Override
    public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
        if (isV4Api(definitionClass)) {
            final Api apiDefinition = (Api) api.getDefinition();
            configurePlans(apiDefinition, Set.of("oauth2"));
        }
    }

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.put("oauth2", PolicyBuilder.build("oauth2", Oauth2Policy.class, OAuth2PolicyConfiguration.class));
    }

    @Override
    public void configureResources(Map<String, ResourcePlugin> resources) {
        resources.put("mock-oauth2-resource", ResourceBuilder.build("mock-oauth2-resource", MockOAuth2Resource.class));
    }

    @Test
    void should_return_oauth_protected_resource_metadata_on_well_known_endpoint(
        final HttpClient client,
        GatewayDynamicConfig.HttpConfig httpConfig
    ) {
        client
            .rxRequest(GET, "/.well-known/oauth-protected-resource/v4-mcp-proxy-api")
            .flatMap(HttpClientRequest::rxSend)
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.rxBody();
            })
            .test()
            .awaitDone(60, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue(body -> {
                OAuth2ResourceMetadata resourceMetadata = mapper.readValue(body.toString(), OAuth2ResourceMetadata.class);
                String protectedResourceUri = String.format("http://localhost:%s/v4-mcp-proxy-api", httpConfig.httpPort());
                assertAll(
                    () -> assertThat(resourceMetadata.protectedResourceUri()).isEqualTo(protectedResourceUri),
                    () -> assertThat(resourceMetadata.authorizationServers()).isEqualTo(List.of("https://some.keycloak.com/realms/test")),
                    () -> assertThat(resourceMetadata.scopesSupported()).containsExactlyInAnyOrder("read", "write")
                );
                return true;
            });
    }

    @Test
    void should_return_200_with_valid_oauth2_token_and_subscription(final HttpClient client, GatewayDynamicConfig.HttpConfig httpConfig) {
        whenSearchingSubscription(API_ID, OAUTH2_CLIENT_ID, PLAN_OAUTH2_ID).thenReturn(
            Optional.of(createSubscription(API_ID, PLAN_OAUTH2_ID, false))
        );

        wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));

        client
            .rxRequest(GET, getApiPath(API_ID))
            .flatMap(request -> {
                request.putHeader("Authorization", "Bearer " + OAUTH2_SUCCESS_TOKEN);
                return request.rxSend();
            })
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.rxBody();
            })
            .test()
            .awaitDone(60, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue(body -> {
                assertThat(body.toString()).contains("endpoint response");
                return true;
            });
    }

    @Test
    void should_return_401_with_unauthorized_token_and_www_authenticate_header(
        final HttpClient client,
        GatewayDynamicConfig.HttpConfig httpConfig
    ) {
        client
            .rxRequest(GET, getApiPath(API_ID))
            .flatMap(request -> {
                request.putHeader("Authorization", "Bearer " + OAUTH2_UNAUTHORIZED_TOKEN);
                return request.rxSend();
            })
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(401);
                String expectedWwwAuthenticate = String.format(
                    "Bearer resource_metadata=\"http://localhost:%s/.well-known/oauth-protected-resource/%s\"",
                    httpConfig.httpPort(),
                    API_ID
                );
                assertThat(response.headers().get("WWW-Authenticate")).isEqualTo(expectedWwwAuthenticate);
                return response.rxBody();
            })
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue(body -> {
                assertThat(body).hasToString("Unauthorized");
                return true;
            });
    }

    @SneakyThrows
    private OngoingStubbing<Optional<Subscription>> whenSearchingSubscription(String api, String clientId, String plan) {
        return when(
            getBean(SubscriptionService.class).getByApiAndSecurityToken(
                eq(api),
                argThat(
                    securityToken ->
                        securityToken.getTokenType().equals(SecurityToken.TokenType.CLIENT_ID.name()) &&
                        securityToken.getTokenValue().equals(clientId)
                ),
                eq(plan)
            )
        );
    }
}
