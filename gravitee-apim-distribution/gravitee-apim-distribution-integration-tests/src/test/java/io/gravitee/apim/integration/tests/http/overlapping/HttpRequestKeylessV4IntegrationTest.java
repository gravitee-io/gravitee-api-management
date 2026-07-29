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
package io.gravitee.apim.integration.tests.http.overlapping;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.dynamicrouting.DynamicRoutingPolicy;
import io.gravitee.policy.dynamicrouting.configuration.DynamicRoutingPolicyConfiguration;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Benoit Bordigoni (benoit.bordigoni at graviteesource.com)
 * @author Gravitee Team
 */
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HttpRequestKeylessV4IntegrationTest extends AbstractGatewayTest {

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.put(
            "dynamic-routing",
            PolicyBuilder.build("dynamic-routing", DynamicRoutingPolicy.class, DynamicRoutingPolicyConfiguration.class)
        );
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
    }

    @Override
    protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
        gatewayConfigurationBuilder.setYamlProperty("api.allowOverlappingContext", "true");
    }

    @Test
    @DisplayName("Should receive 200 - OK on overlapping context paths in the same API")
    @DeployApi("/apis/v4/http/overlapping/api-2-in-1.json")
    void should_get200_overlapping_path_in_single_api(HttpClient httpClient) throws InterruptedException {
        assertCalls(httpClient);
    }

    @Test
    @DisplayName("Should receive 200 - OK on overlapping context two APIs")
    @DeployApi({ "/apis/v4/http/overlapping/api-0.json", "/apis/v4/http/overlapping/api-2.json" })
    void should_get200_overlapping_path_in_2_apis(HttpClient httpClient) throws InterruptedException {
        assertCalls(httpClient);
    }

    private void assertCalls(HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(get("/endpoint0").willReturn(ok("response from backend 0")));
        wiremock.stubFor(get("/endpoint0/foo").willReturn(ok("response from backend 0/foo")));
        wiremock.stubFor(get("/endpoint2").willReturn(ok("response from backend 2")));
        httpClient
            .rxRequest(HttpMethod.GET, "/test/v2")
            .flatMap(HttpClientRequest::rxSend)
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertComplete()
            .assertValue(body -> {
                assertThat(body).hasToString("response from backend 2");
                return true;
            })
            .assertNoErrors();

        httpClient
            .rxRequest(HttpMethod.GET, "/foo")
            .flatMap(HttpClientRequest::rxSend)
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertComplete()
            .assertValue(body -> {
                assertThat(body).hasToString("response from backend 0/foo");
                return true;
            })
            .assertNoErrors();
        httpClient
            .rxRequest(HttpMethod.GET, "")
            .flatMap(HttpClientRequest::rxSend)
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertComplete()
            .assertValue(body -> {
                assertThat(body).hasToString("response from backend 0");
                return true;
            })
            .assertNoErrors();
    }
}
