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
package io.gravitee.apim.integration.tests.properties;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.transformheaders.TransformHeadersPolicy;
import io.gravitee.policy.transformheaders.configuration.TransformHeadersPolicyConfiguration;
import io.vertx.core.http.HttpMethod;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * @author GraviteeSource Team
 */
@GatewayTest
class EncryptedApiPropertiesIntegrationTest extends AbstractGatewayTest {

    private static final String SECRET_PROPERTY = "api.properties.encryption.secret";
    private static final String SECRET = "8y9UBFbJtCIcLZ0hLHrlS9CoDLsFVCXH";
    private static final String ANOTHER_SECRET = "Qm3xTz7vLp2wNc9kRf4hJd6sYb8uEa1g";

    private static final String ENCRYPTED_MANUAL_PLAINTEXT = "this is an encrypted manual property";
    private static final String ENCRYPTED_DYNAMIC_PLAINTEXT = "this is an encrypted dynamic property";
    private static final String PLAIN_DYNAMIC_VALUE = "this is a plain dynamic property";

    // Computed once so both uses see the same ciphertext, regardless of whether encryption is deterministic.
    private static final String ENCRYPTED_WITH_ANOTHER_SECRET_VALUE = encryptWith(ANOTHER_SECRET, ENCRYPTED_MANUAL_PLAINTEXT);

    @Override
    protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
        gatewayConfigurationBuilder.set(SECRET_PROPERTY, SECRET);
    }

    @Override
    @SneakyThrows
    public void configurePlaceHolderVariables(Map<String, String> variables) {
        final DataEncryptor dataEncryptor = dataEncryptorWith(SECRET);
        variables.put("ENCRYPTED_MANUAL_VALUE", dataEncryptor.encrypt(ENCRYPTED_MANUAL_PLAINTEXT));
        variables.put("ENCRYPTED_DYNAMIC_VALUE", dataEncryptor.encrypt(ENCRYPTED_DYNAMIC_PLAINTEXT));
        variables.put("ENCRYPTED_WITH_ANOTHER_SECRET_VALUE", ENCRYPTED_WITH_ANOTHER_SECRET_VALUE);
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
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.put(
            "transform-headers",
            PolicyBuilder.build("transform-headers", TransformHeadersPolicy.class, TransformHeadersPolicyConfiguration.class)
        );
    }

    @Test
    @DeployApi("/apis/v4/properties/api-with-encrypted-properties.json")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void should_evaluate_encrypted_and_plain_properties_to_their_plaintext_value(HttpClient httpClient, VertxTestContext vertxTestContext) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

        httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(HttpClientRequest::rxSend)
            .subscribe(
                response ->
                    vertxTestContext.verify(() -> {
                        assertThat(response.statusCode()).isEqualTo(200);

                        wiremock.verify(
                            1,
                            getRequestedFor(urlPathEqualTo("/endpoint"))
                                .withHeader("X-Encrypted-Manual-Property", equalTo(ENCRYPTED_MANUAL_PLAINTEXT))
                                .withHeader("X-Encrypted-Dynamic-Property", equalTo(ENCRYPTED_DYNAMIC_PLAINTEXT))
                                .withHeader("X-Plain-Dynamic-Property", equalTo(PLAIN_DYNAMIC_VALUE))
                        );
                        vertxTestContext.completeNow();
                    }),
                vertxTestContext::failNow
            );
    }

    @Test
    @DeployApi("/apis/v4/properties/api-with-property-encrypted-with-another-secret.json")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void should_serve_the_stored_value_as_is_when_the_gateway_secret_does_not_match(
        HttpClient httpClient,
        VertxTestContext vertxTestContext
    ) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

        httpClient
            .rxRequest(HttpMethod.GET, "/test-another-secret")
            .flatMap(HttpClientRequest::rxSend)
            .subscribe(
                response ->
                    vertxTestContext.verify(() -> {
                        assertThat(response.statusCode()).isEqualTo(200);

                        wiremock.verify(
                            1,
                            getRequestedFor(urlPathEqualTo("/endpoint")).withHeader(
                                "X-Encrypted-Property",
                                equalTo(ENCRYPTED_WITH_ANOTHER_SECRET_VALUE)
                            )
                        );
                        vertxTestContext.completeNow();
                    }),
                vertxTestContext::failNow
            );
    }

    @SneakyThrows
    private static String encryptWith(String secret, String plaintext) {
        return dataEncryptorWith(secret).encrypt(plaintext);
    }

    private static DataEncryptor dataEncryptorWith(String secret) {
        final StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(SECRET_PROPERTY, secret)));
        return new DataEncryptor(environment, SECRET_PROPERTY, secret);
    }
}
