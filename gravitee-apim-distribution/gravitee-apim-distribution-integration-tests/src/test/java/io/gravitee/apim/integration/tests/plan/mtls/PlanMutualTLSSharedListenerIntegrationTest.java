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
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.PLAN_APIKEY_ID;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.configurePlans;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.getApiPath;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.getUrl;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static io.gravitee.common.http.HttpStatusCode.UNAUTHORIZED_401;
import static io.gravitee.gateway.reactive.api.policy.SecurityToken.TokenType.API_KEY;
import static io.vertx.core.http.HttpMethod.GET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.graviteesource.entrypoint.http.get.HttpGetEntrypointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.parameters.GatewayDynamicConfig;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.integration.tests.plan.PlanHelper;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.ApiKeyService;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.handlers.api.services.SubscriptionCacheService;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.security.core.SubscriptionTrustStoreLoaderManager;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.endpoint.mock.MockEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.apikey.ApiKeyPolicy;
import io.gravitee.policy.apikey.ApiKeyPolicyInitializer;
import io.gravitee.policy.apikey.configuration.ApiKeyPolicyConfiguration;
import io.gravitee.policy.mtls.MtlsPolicy;
import io.gravitee.policy.mtls.configuration.MtlsPolicyConfiguration;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.PoolOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.junit5.Timeout;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ParameterContext;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * One listener in {@code clientAuth: request} serving both an mTLS-plan API and an API Key one, which is how a
 * gateway is usually deployed. A consumer of the API Key API that happens to present a client certificate of its
 * own must be served, and an unknown certificate on the mTLS API must be answered at plan level rather than by a
 * TLS alert -- in both cases whether or not an mTLS subscription is currently registered on the listener.
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@GatewayTest
@DeployApi(value = { "/apis/plan/v4-proxy-api.json", "/apis/plan/v4-message-api.json" })
class PlanMutualTLSSharedListenerIntegrationTest extends AbstractGatewayTest {

    private static final String API_KEY_API = "v4-proxy-api";
    private static final String MTLS_API = "v4-message-api";
    private static final String ENDPOINT_RESPONSE = "endpoint response";

    private SubscriptionTrustStoreLoaderManager subscriptionTrustStoreLoaderManager;
    private Subscription mtlsSubscription;

    @Override
    public void configurePolicies(final Map<String, PolicyPlugin> policies) {
        policies.put("mtls", PolicyBuilder.build("mtls", MtlsPolicy.class, MtlsPolicyConfiguration.class));
        policies.put(
            "api-key",
            PolicyBuilder.build("api-key", ApiKeyPolicy.class, ApiKeyPolicyConfiguration.class, ApiKeyPolicyInitializer.class)
        );
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
        entrypoints.putIfAbsent("http-get", EntrypointBuilder.build("http-get", HttpGetEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
        endpoints.putIfAbsent("mock", EndpointBuilder.build("mock", MockEndpointConnectorFactory.class));
    }

    @Override
    public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
        reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
    }

    /**
     * Two APIs on the same listener, each with the single plan its consumers use.
     */
    @Override
    public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
        if (isV4Api(definitionClass)) {
            final Api apiDefinition = (Api) api.getDefinition();
            configurePlans(apiDefinition, Set.of(MTLS_API.equals(apiDefinition.getId()) ? "mtls" : "api-key"));
        }
    }

    @SneakyThrows
    @Override
    protected void configureGateway(GatewayConfigurationBuilder config) {
        config
            .httpSecured(true)
            .set("http.ssl.clientAuth", "request")
            .set("http.ssl.keystore.type", KeyStoreLoader.CERTIFICATE_FORMAT_SELF_SIGNED);
    }

    /**
     * The client always presents a certificate the gateway knows nothing about: it belongs to no subscription and
     * no configured trust store, exactly like a consumer whose framework sends a certificate it was never asked for.
     */
    @Override
    protected void configureHttpClient(
        HttpClientOptions options,
        PoolOptions poolOptions,
        GatewayDynamicConfig.Config gatewayConfig,
        ParameterContext parameterContext
    ) {
        options.setSsl(true).setTrustAll(true).setDefaultPort(gatewayConfig.httpPort()).setDefaultHost("localhost");
        options.setKeyCertOptions(
            new PemKeyCertOptions()
                .addCertPath(getUrl("plans/mtls/client2.cer").getPath())
                .addKeyPath(getUrl("plans/mtls/client2.key").getPath())
        );
    }

    @BeforeEach
    void setUp() {
        subscriptionTrustStoreLoaderManager = getBean(SubscriptionTrustStoreLoaderManager.class);
        // Cheat to use the real SubscriptionTrustStoreLoaderManager instance with SubscriptionService mock
        final SubscriptionCacheService subscriptionService = (SubscriptionCacheService) getBean(SubscriptionService.class);
        when(subscriptionService.getByApiAndSecurityToken(any(), any(), any())).thenCallRealMethod();
        ReflectionTestUtils.setField(subscriptionService, "subscriptionTrustStoreLoaderManager", subscriptionTrustStoreLoaderManager);

        // an mTLS subscription of another API is live on the listener, which is what fills its in-memory trust store
        mtlsSubscription = anMtlsSubscription();
        subscriptionTrustStoreLoaderManager.registerSubscription(mtlsSubscription, Set.of());
    }

    @AfterEach
    void tearDown() {
        subscriptionTrustStoreLoaderManager.unregisterSubscription(mtlsSubscription);
    }

    @Test
    @Timeout(value = 30, timeUnit = TimeUnit.SECONDS)
    void should_serve_an_api_key_api_to_a_consumer_presenting_an_unknown_client_certificate(HttpClient client) {
        final ApiKey apiKey = anApiKey();
        when(getBean(ApiKeyService.class).getByApiAndKey(any(), any())).thenReturn(Optional.of(apiKey));
        // doReturn, not when(...): the real method is stubbed in with thenCallRealMethod and when(...) would call it
        doReturn(Optional.of(PlanHelper.createSubscription(API_KEY_API, PLAN_APIKEY_ID, false)))
            .when(getBean(SubscriptionService.class))
            .getByApiAndSecurityToken(
                eq(API_KEY_API),
                argThat(token -> API_KEY.name().equals(token.getTokenType()) && apiKey.getKey().equals(token.getTokenValue())),
                eq(PLAN_APIKEY_ID)
            );
        wiremock.stubFor(get("/endpoint").willReturn(ok(ENDPOINT_RESPONSE)));

        client
            .rxRequest(GET, getApiPath(API_KEY_API))
            .flatMap(request -> {
                request.putHeader("X-Gravitee-Api-Key", apiKey.getKey());
                return request.rxSend();
            })
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(OK_200);
                return response.rxBody();
            })
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue(body -> {
                assertThat(body.toString()).contains(ENDPOINT_RESPONSE);
                return true;
            });

        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));
    }

    @Test
    @Timeout(value = 30, timeUnit = TimeUnit.SECONDS)
    void should_answer_401_on_an_mtls_api_when_the_client_certificate_belongs_to_no_subscription(HttpClient client) {
        client
            .rxRequest(GET, getApiPath(MTLS_API))
            .flatMap(HttpClientRequest::rxSend)
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED_401);
                return response.rxBody();
            })
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue(body -> {
                assertThat(body.toString()).contains(MtlsPolicy.FAILURE_MESSAGE);
                return true;
            });
    }

    private ApiKey anApiKey() {
        final ApiKey apiKey = new ApiKey();
        apiKey.setApi(API_KEY_API);
        apiKey.setApplication(PlanHelper.APPLICATION_ID);
        apiKey.setSubscription(PlanHelper.SUBSCRIPTION_ID);
        apiKey.setPlan(PLAN_APIKEY_ID);
        apiKey.setKey("apiKeyValue");
        return apiKey;
    }

    @SneakyThrows
    private Subscription anMtlsSubscription() {
        final Subscription subscription = new Subscription();
        subscription.setApi(MTLS_API);
        subscription.setApplication(PlanHelper.APPLICATION_ID);
        subscription.setId(PlanHelper.SUBSCRIPTION_ID);
        subscription.setPlan(PlanHelper.PLAN_MTLS_ID);
        final String clientCertificate = Files.readString(Paths.get(getUrl("plans/mtls/client.cer").getPath()));
        subscription.setClientCertificate(Base64.getEncoder().encodeToString(clientCertificate.getBytes()));
        return subscription;
    }
}
