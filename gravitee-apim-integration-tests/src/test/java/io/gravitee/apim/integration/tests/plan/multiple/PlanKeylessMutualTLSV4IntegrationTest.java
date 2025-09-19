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
package io.gravitee.apim.integration.tests.plan.multiple;

import static io.gravitee.apim.integration.tests.plan.PlanHelper.configurePlans;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.configureTrustedHttpClient;
import static io.gravitee.common.http.HttpStatusCode.OK_200;

import com.graviteesource.entrypoint.http.get.HttpGetEntrypointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.parameters.GatewayDynamicConfig;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.integration.tests.plan.keyless.PlanKeylessV4IntegrationTest;
import io.gravitee.apim.integration.tests.plan.mtls.PlanMutualTLSClientAuthRequestIntegrationTest;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.endpoint.mock.MockEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.keyless.KeylessPolicy;
import io.gravitee.policy.mtls.MtlsPolicy;
import io.gravitee.policy.mtls.configuration.MtlsPolicyConfiguration;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author GraviteeSource Team
 */
public class PlanKeylessMutualTLSV4IntegrationTest {

    public static void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
        final Api apiDefinition = (Api) api.getDefinition();
        configurePlans(apiDefinition, Set.of("key-less", "mtls"));
    }

    public static void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
        entrypoints.putIfAbsent("http-get", EntrypointBuilder.build("http-get", HttpGetEntrypointConnectorFactory.class));
    }

    public static void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
        endpoints.putIfAbsent("mock", EndpointBuilder.build("mock", MockEndpointConnectorFactory.class));
    }

    public static void configurePolicies(final Map<String, PolicyPlugin> policies) {
        policies.put("key-less", PolicyBuilder.build("key-less", KeylessPolicy.class));
        policies.put("mtls", PolicyBuilder.build("mtls", MtlsPolicy.class, MtlsPolicyConfiguration.class));
    }

    public static void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
        reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
    }

    public static Stream<Arguments> provideApis() {
        return Stream.of(Arguments.of("v4-proxy-api", true), Arguments.of("v4-message-api", false));
    }

    public static void configureGateway(GatewayConfigurationBuilder config) {
        config
            .httpSecured(true)
            .set("http.ssl.clientAuth", "request")
            .set("http.ssl.keystore.type", KeyStoreLoader.CERTIFICATE_FORMAT_SELF_SIGNED);
    }

    @Nested
    @GatewayTest
    @DeployApi(value = { "/apis/plan/v4-proxy-api.json", "/apis/plan/v4-message-api.json" })
    public class SelectKeylessTest extends PlanKeylessV4IntegrationTest {

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            if (isV4Api(definitionClass)) {
                PlanKeylessMutualTLSV4IntegrationTest.configureApi(api, definitionClass);
            }
        }

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            PlanKeylessMutualTLSV4IntegrationTest.configureEntrypoints(entrypoints);
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            PlanKeylessMutualTLSV4IntegrationTest.configureEndpoints(endpoints);
        }

        @Override
        public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
            PlanKeylessMutualTLSV4IntegrationTest.configureReactors(reactors);
        }

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            PlanKeylessMutualTLSV4IntegrationTest.configurePolicies(policies);
        }

        @Override
        protected Stream<Arguments> provideApis() {
            return PlanKeylessMutualTLSV4IntegrationTest.provideApis();
        }

        @SneakyThrows
        @Override
        protected void configureGateway(GatewayConfigurationBuilder config) {
            PlanKeylessMutualTLSV4IntegrationTest.configureGateway(config);
        }

        @Override
        protected void configureHttpClient(
            HttpClientOptions options,
            GatewayDynamicConfig.Config gatewayConfig,
            ParameterContext parameterContext
        ) {
            configureTrustedHttpClient(options, gatewayConfig.httpPort(), false);
        }

        protected Stream<Arguments> provideSecurityHeaders() {
            return provideApis().flatMap(arguments -> {
                String path = (String) arguments.get()[0];
                boolean requireWiremock = (boolean) arguments.get()[1];
                return Stream.of(
                    Arguments.of(path, requireWiremock, "Authorization", ""),
                    Arguments.of(path, requireWiremock, "Authorization", "Basic 1231456789"),
                    Arguments.of(path, requireWiremock, "Authorization", "Bearer"),
                    Arguments.of(path, requireWiremock, "Authorization", "Bearer "),
                    Arguments.of(path, requireWiremock, "Authorization", "Bearer a-jwt-token")
                );
            });
        }

        @Override
        @ParameterizedTest
        @MethodSource("provideSecurityHeaders")
        protected void should_access_api_and_ignore_security(
            String apiId,
            boolean requireWiremock,
            String headerName,
            String headerValue,
            HttpClient client
        ) {
            super.should_access_api_and_ignore_security(apiId, requireWiremock, headerName, headerValue, client);
        }

        @Override
        @ParameterizedTest
        @MethodSource("provideApis")
        protected void should_return_200_success_without_any_security(String apiId, boolean requireWiremock, HttpClient client) {
            super.should_return_200_success_without_any_security(apiId, requireWiremock, client);
        }
    }

    @Nested
    @GatewayTest
    @DeployApi(value = { "/apis/plan/v4-proxy-api.json", "/apis/plan/v4-message-api.json" })
    public class SelectMtlsTest extends PlanMutualTLSClientAuthRequestIntegrationTest {

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            if (isV4Api(definitionClass)) {
                PlanKeylessMutualTLSV4IntegrationTest.configureApi(api, definitionClass);
            }
        }

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            PlanKeylessMutualTLSV4IntegrationTest.configureEntrypoints(entrypoints);
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            PlanKeylessMutualTLSV4IntegrationTest.configureEndpoints(endpoints);
        }

        @Override
        public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
            PlanKeylessMutualTLSV4IntegrationTest.configureReactors(reactors);
        }

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            PlanKeylessMutualTLSV4IntegrationTest.configurePolicies(policies);
        }

        @Override
        public Stream<Arguments> provideApis() {
            return PlanKeylessMutualTLSV4IntegrationTest.provideApis();
        }

        @SneakyThrows
        @Override
        protected void configureGateway(GatewayConfigurationBuilder config) {
            PlanKeylessMutualTLSV4IntegrationTest.configureGateway(config);
        }

        @ParameterizedTest
        @MethodSource("provideApis")
        @Override
        protected void should_not_be_able_to_call_api_with_mtls_plan_if_no_cert_in_request(
            final String apiId,
            final boolean requireWiremock,
            final HttpClient client
        ) {
            // In this case, the mtls plan will simply be skipped and pass to the Keyless one, letting the request success
            should_not_be_able_to_call_api_with_mtls_plan_if_no_cert_in_request(apiId, requireWiremock, client, OK_200, ENDPOINT_RESPONSE);
        }
    }
}
