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
package io.gravitee.apim.integration.tests.resource;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.resource.ResourceBuilder;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.resource.ResourcePlugin;
import io.gravitee.policy.oauth2.Oauth2Policy;
import io.gravitee.policy.oauth2.configuration.OAuth2PolicyConfiguration;
import io.gravitee.policy.v3.oauth2.Oauth2PolicyV3;
import io.gravitee.resource.oauth2.generic.OAuth2GenericResource;
import io.gravitee.resource.oauth2.generic.configuration.OAuth2ResourceConfiguration;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

@GatewayTest
public class Oauth2GenericResourceIntegrationTest extends AbstractGatewayTest {

    private static final String CLIENT_SECRET = "adminPassword";

    @Override
    public void configureResources(Map<String, ResourcePlugin> resources) {
        resources.putIfAbsent("oauth2", ResourceBuilder.build("oauth2", OAuth2GenericResource.class, OAuth2ResourceConfiguration.class));
    }

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.putIfAbsent("oauth2", PolicyBuilder.build("oauth2", Oauth2Policy.class, OAuth2PolicyConfiguration.class));
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
    }

    @Test
    @DeployApi("/apis/v4/resource/api-generic-oauth2-unsecured.json")
    void should_call_oauth2_introspect__unsecured_endpoint(HttpClient httpClient) {
        shouldCallApiSuccessfully(httpClient);
    }

    @Test
    @DeployApi("/apis/v4/resource/api-generic-oauth2-secured-no-ssl-conf.json")
    void should_call_oauth2_introspect__secured_endpoint_no_ssl_configuration(HttpClient httpClient) {
        shouldCallApiSuccessfully(httpClient);
    }

    @Test
    @DeployApi("/apis/v4/resource/api-generic-oauth2-secured-with-ssl-conf.json")
    void should_call_oauth2_introspect__secured_endpoint_ssl_configuration(HttpClient httpClient) {
        shouldCallApiSuccessfully(httpClient);
    }

    private void shouldCallApiSuccessfully(HttpClient httpClient) {
        String jwt = new PlainJWT(
            new JWTClaimsSet.Builder()
                .subject("test")
                .issuer("ITTest")
                .audience("OAuth")
                .expirationTime(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .issueTime(new Date())
                .claim("test", "hello")
                .build()
        ).serialize();

        // stub for introspect endpoint (called by oauth policy via oauth2 resource)
        wiremock.stubFor(
            get("/oauth/check_token")
                .withBasicAuth("admin", CLIENT_SECRET)
                .withHeader("token", equalTo(jwt))
                .willReturn(
                    jsonResponse(
                        """
                        {
                            "client_id": "admin",
                            "sub": "the_user",
                            "scope": []
                        }
                        """,
                        200
                    )
                )
        );

        // the backend
        wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

        callApi(httpClient, Map.of("Authorization", "Bearer " + jwt));

        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/oauth/check_token")));
        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));
    }

    private static void callApi(HttpClient httpClient, Map<String, String> map) {
        httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .doOnSuccess(req -> map.forEach(req::putHeader))
            .flatMap(HttpClientRequest::rxSend)
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.body();
            })
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertValue(Buffer.buffer("response from backend"))
            .assertComplete();
    }
}
