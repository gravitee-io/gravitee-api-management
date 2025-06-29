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
package io.gravitee.apim.integration.tests.el;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.gateway.dictionary.model.Dictionary;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.assignattributes.AssignAttributesPolicy;
import io.gravitee.policy.assignattributes.configuration.AssignAttributesPolicyConfiguration;
import io.gravitee.policy.transformheaders.TransformHeadersPolicy;
import io.gravitee.policy.transformheaders.configuration.TransformHeadersPolicyConfiguration;
import io.vertx.core.http.HttpMethod;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
class ELIntegrationTest extends AbstractGatewayTest {

    public static final String DICTIONARY_VALUE = "this is a dictionary value";

    @Override
    public void configureDictionaries(List<Dictionary> dictionaries) {
        Dictionary dictionary = new Dictionary();
        dictionary.setId("test");
        dictionary.setKey("test");
        dictionary.setEnvironmentId("DEFAULT");
        dictionary.setProperties(Map.of("test", DICTIONARY_VALUE));
        dictionaries.add(dictionary);
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
            "policy-assign-attributes",
            PolicyBuilder.build("policy-assign-attributes", AssignAttributesPolicy.class, AssignAttributesPolicyConfiguration.class)
        );
        policies.put(
            "transform-headers",
            PolicyBuilder.build("transform-headers", TransformHeadersPolicy.class, TransformHeadersPolicyConfiguration.class)
        );
    }

    @Test
    @DeployApi("/apis/v4/el/api-with-ELs.json")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void should_get_all_EL_values(HttpClient httpClient, VertxTestContext vertxTestContext) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

        httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(HttpClientRequest::rxSend)
            .subscribe(
                response ->
                    vertxTestContext.verify(() -> {
                        // just asserting we get a response (hence no SSL errors), no need for an API.
                        assertThat(response.statusCode()).isEqualTo(200);
                        assertThat(response.headers().get("X-Response-Content")).isEqualTo("response from backend");

                        wiremock.verify(
                            1,
                            getRequestedFor(urlPathEqualTo("/endpoint"))
                                .withHeader("X-Node-Version", matching("^\\d+\\.\\d+\\.\\d+.*"))
                                .withHeader("X-Dictionary", equalTo(DICTIONARY_VALUE))
                                .withHeader("X-Api-Property", equalTo("this is an API property"))
                                .withHeader("X-Context-Attributes", equalTo("this is an attribute"))
                                .withHeader("X-Request-Path", equalTo("/test/"))
                        );
                        vertxTestContext.completeNow();
                    }),
                vertxTestContext::failNow
            );
    }
}
