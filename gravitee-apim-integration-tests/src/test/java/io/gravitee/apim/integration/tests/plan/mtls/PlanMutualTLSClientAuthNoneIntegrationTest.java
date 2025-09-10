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
import static io.gravitee.apim.integration.tests.plan.PlanHelper.configurePlans;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.configureTrustedHttpClient;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.getUrl;
import static io.gravitee.common.http.HttpStatusCode.UNAUTHORIZED_401;
import static io.vertx.core.http.HttpMethod.GET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import io.gravitee.policy.mtls.MtlsPolicy;
import io.gravitee.policy.mtls.configuration.MtlsPolicyConfiguration;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@GatewayTest
@DeployApi(value = { "/apis/plan/v4-proxy-api.json", "/apis/plan/v4-message-api.json" })
public class PlanMutualTLSClientAuthNoneIntegrationTest extends AbstractGatewayTest {

    private SubscriptionTrustStoreLoaderManager subscriptionTrustStoreLoaderManager;

    @Override
    public void configurePolicies(final Map<String, PolicyPlugin> policies) {
        policies.put("mtls", PolicyBuilder.build("mtls", MtlsPolicy.class, MtlsPolicyConfiguration.class));
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
    public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
        if (isV4Api(definitionClass)) {
            final Api apiDefinition = (Api) api.getDefinition();
            configurePlans(apiDefinition, Set.of("mtls"));
        }
    }

    @Override
    public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
        reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
    }

    @Override
    protected void configureHttpClient(
        HttpClientOptions options,
        GatewayDynamicConfig.Config gatewayConfig,
        ParameterContext parameterContext
    ) {
        boolean withCert = parameterContext.findAnnotation(WithCert.class).isPresent();
        configureTrustedHttpClient(options, gatewayConfig.httpPort(), withCert);
    }

    public Stream<Arguments> provideApis() {
        return Stream.of(Arguments.of("v4-proxy-api", true), Arguments.of("v4-message-api", false));
    }

    @SneakyThrows
    @Override
    protected void configureGateway(GatewayConfigurationBuilder config) {
        config
            .httpSecured(true)
            .set("http.ssl.clientAuth", "none")
            .set("http.ssl.keystore.type", KeyStoreLoader.CERTIFICATE_FORMAT_SELF_SIGNED);
    }

    @BeforeEach
    void setUp() {
        subscriptionTrustStoreLoaderManager = getBean(SubscriptionTrustStoreLoaderManager.class);
        // Cheat to use the real SubscriptionTrustStoreLoaderManager instance with SubscriptionService mock
        final SubscriptionCacheService subscriptionService = (SubscriptionCacheService) getBean(SubscriptionService.class);
        when(subscriptionService.getByApiAndSecurityToken(any(), any(), any())).thenCallRealMethod();
        ReflectionTestUtils.setField(subscriptionService, "subscriptionTrustStoreLoaderManager", subscriptionTrustStoreLoaderManager);
    }

    @ParameterizedTest
    @MethodSource("provideApis")
    void should_not_be_able_to_call_api_with_mtls_plan_if_no_cert_in_unsecured_request(
        final String apiId,
        final boolean requireWiremock,
        final HttpClient client
    ) {
        if (requireWiremock) {
            wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));
        }
        client
            .rxRequest(GET, PlanHelper.getApiPath(apiId))
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
        if (requireWiremock) {
            wiremock.verify(0, getRequestedFor(urlPathEqualTo("/endpoint")));
        }
    }

    @ParameterizedTest
    @MethodSource("provideApis")
    void should_not_be_able_to_call_api_with_mtls_plan_if_certificate_not_registered_from_subscription(
        final String apiId,
        final boolean requireWiremock,
        final HttpClient client
    ) {
        if (requireWiremock) {
            wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));
        }
        client
            .rxRequest(GET, PlanHelper.getApiPath(apiId))
            .flatMap(HttpClientRequest::rxSend)
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED_401);
                return response.rxBody();
            })
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(t -> {
                assertThat(t.toString()).contains(MtlsPolicy.FAILURE_MESSAGE);
                return true;
            });
        if (requireWiremock) {
            wiremock.verify(0, getRequestedFor(urlPathEqualTo("/endpoint")));
        }
    }

    @ParameterizedTest
    @MethodSource("provideApis")
    void should_not_be_able_to_call_api_with_mtls_plan_if_certificate_not_registered_from_subscription_with_cert(
        final String apiId,
        final boolean requireWiremock,
        @WithCert HttpClient client
    ) {
        if (requireWiremock) {
            wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));
        }
        client
            .rxRequest(GET, PlanHelper.getApiPath(apiId))
            .flatMap(HttpClientRequest::rxSend)
            .test()
            .awaitDone(30, TimeUnit.SECONDS)
            .assertValue(t -> {
                assertThat(t.statusCode()).isEqualTo(UNAUTHORIZED_401);
                return true;
            });
        if (requireWiremock) {
            wiremock.verify(0, getRequestedFor(urlPathEqualTo("/endpoint")));
        }
    }

    @ParameterizedTest
    @MethodSource("provideApis")
    void should_be_able_to_call_api_with_mtls_plan(final String apiId, final boolean requireWiremock, @WithCert HttpClient client) {
        if (requireWiremock) {
            wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));
        }
        final Subscription subscription = aSubscription(apiId);
        // Directly use the SubscriptionTrustStoreLoaderManager to fake the sync process of a subscription and register the certificate
        subscriptionTrustStoreLoaderManager.registerSubscription(subscription, Set.of());
        client
            .rxRequest(GET, PlanHelper.getApiPath(apiId))
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

        subscriptionTrustStoreLoaderManager.unregisterSubscription(subscription);
        if (requireWiremock) {
            wiremock.verify(0, getRequestedFor(urlPathEqualTo("/endpoint")));
        }
    }

    @SneakyThrows
    Subscription aSubscription(String api) {
        final Subscription subscription = new Subscription();
        subscription.setApi(api);
        subscription.setApplication("application-id");
        subscription.setId("subscription-id");
        subscription.setPlan(PlanHelper.PLAN_MTLS_ID);
        final String clientCertificate = Files.readString(Paths.get(getUrl("plans/mtls/client.cer").getPath()));
        subscription.setClientCertificate(Base64.getEncoder().encodeToString(clientCertificate.getBytes()));
        return subscription;
    }
}
