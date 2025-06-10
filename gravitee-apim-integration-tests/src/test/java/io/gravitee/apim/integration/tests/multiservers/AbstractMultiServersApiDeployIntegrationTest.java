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
package io.gravitee.apim.integration.tests.multiservers;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
abstract class AbstractMultiServersApiDeployIntegrationTest extends AbstractGatewayTest {

    int firstServerPort;
    int secondServerPort;

    @Override
    protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
        super.configureGateway(gatewayConfigurationBuilder);

        firstServerPort = gatewayPort();
        gatewayConfigurationBuilder.set("servers[0].id", "first");
        gatewayConfigurationBuilder.set("servers[0].type", "http");
        gatewayConfigurationBuilder.set("servers[0].port", gatewayPort());

        secondServerPort = getAvailablePort();
        gatewayConfigurationBuilder.set("servers[1].id", "second");
        gatewayConfigurationBuilder.set("servers[1].type", "http");
        gatewayConfigurationBuilder.set("servers[1].port", secondServerPort);
    }

    void should_get_200_when_calling_on_both_first_and_second_servers(HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

        // Call the gateway on the 'first' server.
        httpClient
            .rxRequest(HttpMethod.GET, firstServerPort, "localhost", "/test")
            .flatMap(HttpClientRequest::rxSend)
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertComplete()
            .assertValue(body -> {
                assertThat(body.toString()).isEqualTo("response from backend");
                return true;
            })
            .assertNoErrors();

        // Call the gateway on the 'second' server.
        httpClient
            .rxRequest(HttpMethod.GET, secondServerPort, "localhost", "/test")
            .flatMap(HttpClientRequest::rxSend)
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertComplete()
            .assertValue(body -> {
                assertThat(body.toString()).isEqualTo("response from backend");
                return true;
            })
            .assertNoErrors();

        // Check that 2 calls effectively reached the endpoint.
        wiremock.verify(2, getRequestedFor(urlPathEqualTo("/endpoint")));
    }

    void should_get_200_when_calling_on_second_server_and_404_on_first_server(HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

        // Call the gateway on the 'first' server and expect a 404 No context-path.
        httpClient
            .rxRequest(HttpMethod.GET, firstServerPort, "localhost", "/test")
            .flatMap(HttpClientRequest::rxSend)
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(404);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertComplete()
            .assertValue(body -> {
                assertThat(body.toString()).isEqualTo("No context-path matches the request URI.");
                return true;
            })
            .assertNoErrors();

        // Call the gateway on the 'second' server.
        httpClient
            .rxRequest(HttpMethod.GET, secondServerPort, "localhost", "/test")
            .flatMap(HttpClientRequest::rxSend)
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertComplete()
            .assertValue(body -> {
                assertThat(body.toString()).isEqualTo("response from backend");
                return true;
            })
            .assertNoErrors();

        // Check that 1 calls effectively reached the endpoint.
        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));
    }
}
