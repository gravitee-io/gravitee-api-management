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
package io.gravitee.apim.integration.tests.plan.mtls;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.configurePlans;
import static io.vertx.core.http.HttpMethod.GET;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.security.core.SubscriptionTrustStoreLoaderManager;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.mtls.MtlsPolicy;
import io.gravitee.policy.mtls.configuration.MtlsPolicyConfiguration;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLHandshakeException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployApi(value = { "/apis/plan/v4-proxy-api.json" })
public class PlanMutualTLSIntegrationTest extends AbstractGatewayTest {

    @Override
    public void configurePolicies(final Map<String, PolicyPlugin> policies) {
        policies.put("mtls", PolicyBuilder.build("mtls", MtlsPolicy.class, MtlsPolicyConfiguration.class));
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
    public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
        if (isV4Api(definitionClass)) {
            final Api apiDefinition = (Api) api.getDefinition();
            configurePlans(apiDefinition, Set.of("mtls"));
        }
    }

    @SneakyThrows
    @Override
    protected void configureGateway(GatewayConfigurationBuilder config) {
        config
            .httpSecured(true)
            .set("http.ssl.clientAuth", "request")
            .set("http.ssl.keystore.type", KeyStoreLoader.CERTIFICATE_FORMAT_SELF_SIGNED)
            // Gateway requires an empty truststore to work properly with clientAuth: request mode.
            .set("http.ssl.truststore.path", getUrl("plans/mtls/empty-truststore.jks").getPath())
            .set("http.ssl.truststore.password", "secret");
    }

    @Override
    protected void configureHttpClient(HttpClientOptions options) {
        options.setSsl(true).setTrustAll(true).setVerifyHost(false).setDefaultPort(gatewayPort());
    }

    @Test
    void should_not_be_able_to_call_api_with_mtls_plan_if_no_cert_in_request(final HttpClient client) throws Exception {
        wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));
        client
            .rxRequest(GET, "/v4-proxy-api")
            .flatMap(HttpClientRequest::rxSend)
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(401);
                return response.rxBody();
            })
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue(body -> {
                assertThat(body.toString()).contains("Unauthorized");
                return true;
            });
    }

    @Test
    void should_not_be_able_to_call_api_with_mtls_plan_if_certificate_not_registered_from_subscription(final Vertx vertx) throws Exception {
        wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));
        final HttpClient client = createTrustedHttpClient(vertx);
        client
            .rxRequest(GET, "/v4-proxy-api")
            .flatMap(HttpClientRequest::rxSend)
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertError(t -> {
                assertThat(t.getCause()).isInstanceOf(SSLHandshakeException.class);
                return true;
            });
    }

    @Test
    void should_be_able_to_call_api_with_mtls_plan(final Vertx vertx) throws Exception {
        wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));
        final Subscription subscription = aSubscription();
        final SubscriptionTrustStoreLoaderManager subscriptionTrustStoreLoaderManager = getBean(SubscriptionTrustStoreLoaderManager.class);
        // Directly use the SubscriptionTrustStoreLoaderManager to fake the sync process of a subscription and register the certificate
        subscriptionTrustStoreLoaderManager.registerSubscription(subscription, Set.of());
        final HttpClient client = createTrustedHttpClient(vertx);
        client
            .rxRequest(GET, "/v4-proxy-api")
            .flatMap(HttpClientRequest::rxSend)
            .flatMap(response -> {
                // FIXME: assertions will change when mTLS policy will be properly implemented
                assertThat(response.statusCode()).isEqualTo(401);
                return response.rxBody();
            })
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue(body -> {
                assertThat(body.toString()).contains("Unauthorized");
                return true;
            });
    }

    @SneakyThrows
    Subscription aSubscription() {
        final Subscription subscription = new Subscription();
        subscription.setApplication("application-id");
        subscription.setId("subscription-id");
        subscription.setPlan("plan-id");
        final String clientCertificate = Files.readString(Paths.get(getUrl("plans/mtls/client.cer").getPath()));
        subscription.setClientCertificate(Base64.getEncoder().encodeToString(clientCertificate.getBytes()));
        return subscription;
    }

    HttpClient createTrustedHttpClient(Vertx vertx) {
        var options = new HttpClientOptions()
            .setSsl(true)
            .setTrustAll(true)
            .setDefaultPort(gatewayPort())
            .setDefaultHost("localhost")
            .setPemKeyCertOptions(
                new PemKeyCertOptions()
                    .addCertPath(getUrl("plans/mtls/client.cer").getPath())
                    .addKeyPath(getUrl("plans/mtls/client.key").getPath())
            );

        return vertx.createHttpClient(options);
    }

    URL getUrl(String name) {
        return getClass().getClassLoader().getResource(name);
    }
}
