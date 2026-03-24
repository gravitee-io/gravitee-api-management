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
package io.gravitee.apim.integration.tests.http;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.JWT_CLIENT_ID;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.JWT_SECRET;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.PLAN_MTLS_ID;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.configurePlans;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.configureTrustedHttpClient;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.generateJWT;
import static io.gravitee.apim.integration.tests.plan.PlanHelper.getUrl;
import static io.gravitee.policy.jwt.alg.Signature.HMAC_HS256;
import static io.gravitee.policy.v3.jwt.resolver.KeyResolver.GIVEN_KEY;
import static io.vertx.core.http.HttpMethod.GET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApiProducts;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.parameters.GatewayDynamicConfig;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.integration.tests.plan.mtls.WithCert;
import io.gravitee.common.security.PKCS7Utils;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.gateway.api.service.ApiKeyService;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.services.SubscriptionCacheService;
import io.gravitee.gateway.reactive.api.policy.SecurityToken;
import io.gravitee.gateway.security.core.SubscriptionTrustStoreLoaderManager;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.apikey.ApiKeyPolicy;
import io.gravitee.policy.apikey.ApiKeyPolicyInitializer;
import io.gravitee.policy.apikey.configuration.ApiKeyPolicyConfiguration;
import io.gravitee.policy.jwt.JWTPolicy;
import io.gravitee.policy.jwt.configuration.JWTPolicyConfiguration;
import io.gravitee.policy.mtls.MtlsPolicy;
import io.gravitee.policy.mtls.configuration.MtlsPolicyConfiguration;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ParameterContext;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * API Product integration tests for security enforcement (JWT, mTLS).
 * Management/lifecycle tests remain in {@link ApiProductV4IntegrationTest}.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductV4SecurityIntegrationTest {

    private static final String ENV_ID = "DEFAULT";
    private static final String API_1_ID = "my-api-v4-1";
    private static final String API_2_ID = "my-api-v4-2";
    private static final String API_1_PATH = "/test-1";
    private static final String API_2_PATH = "/test-2";

    @Nested
    class JwtSecurityTypeScenarios extends SecurityTestPreparer {

        @Test
        @DeployApi(
            { "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json", "/apis/v4/http/api-product/api-3.json" }
        )
        void should_return_200_success_with_jwt_and_subscription_on_the_api_product_api(HttpClient client) throws Exception {
            final String productId = "jwt-product-success";
            final String planId = "jwt-product-plan-success";
            ReactableApiProduct p = product(productId, Set.of(API_1_ID));
            registerProductPlans(p, List.of(productJwtPlan(planId, PlanStatus.PUBLISHED)));
            deployApiProduct(p);
            allowJwtSubscriptionForApiAndPlan(API_1_ID, planId, JWT_CLIENT_ID);

            int status = getStatusWithHeader(client, API_1_PATH, "Authorization", "Bearer " + generateJWT(5000));
            assertThat(status).isEqualTo(200);
        }

        @Test
        @DeployApi(
            { "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json", "/apis/v4/http/api-product/api-3.json" }
        )
        void should_return_401_unauthorized_with_wrong_security_on_the_api_product_api(HttpClient client) {
            final String productId = "jwt-product-wrong-security";
            final String planId = "jwt-product-plan-wrong-security";
            ReactableApiProduct p = product(productId, Set.of(API_1_ID));
            registerProductPlans(p, List.of(productJwtPlan(planId, PlanStatus.PUBLISHED)));
            deployApiProduct(p);

            assertThat(getStatus(client, API_1_PATH)).isEqualTo(401);
            assertThat(getStatusWithHeader(client, API_1_PATH, "Authorization", "Bearer a-jwt-token")).isEqualTo(401);
            assertThat(getStatusWithHeader(client, API_1_PATH, "X-Gravitee-Api-Key", "an-api-key")).isEqualTo(401);
        }

        @Test
        @DeployApi(
            { "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json", "/apis/v4/http/api-product/api-3.json" }
        )
        void should_keep_sibling_api_accessible_via_jwt_after_one_api_is_removed_from_product(HttpClient client) throws Exception {
            final String productId = "jwt-product-deletion-sibling";
            final String planId = "jwt-product-plan-deletion-sibling";
            ReactableApiProduct p = product(productId, Set.of(API_1_ID, API_2_ID));
            registerProductPlans(p, List.of(productJwtPlan(planId, PlanStatus.PUBLISHED)));
            deployApiProduct(p);
            allowJwtSubscriptionForApiAndPlan(API_1_ID, planId, JWT_CLIENT_ID);
            allowJwtSubscriptionForApiAndPlan(API_2_ID, planId, JWT_CLIENT_ID);

            assertThat(getStatusWithHeader(client, API_1_PATH, "Authorization", "Bearer " + generateJWT(5000))).isEqualTo(200);
            assertThat(getStatusWithHeader(client, API_2_PATH, "Authorization", "Bearer " + generateJWT(5000))).isEqualTo(200);

            undeploy(API_1_ID);
            ReactableApiProduct updatedProduct = product(productId, Set.of(API_2_ID));
            registerProductPlans(updatedProduct, List.of(productJwtPlan(planId, PlanStatus.PUBLISHED)));
            redeployApiProduct(updatedProduct);

            assertThat(getStatusWithHeader(client, API_1_PATH, "Authorization", "Bearer " + generateJWT(5000))).isEqualTo(404);
            assertThat(getStatusWithHeader(client, API_2_PATH, "Authorization", "Bearer " + generateJWT(5000))).isEqualTo(200);
        }

        @Test
        @DeployApi(
            { "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json", "/apis/v4/http/api-product/api-3.json" }
        )
        void should_return_401_unauthorized_with_expired_jwt_and_subscription_on_the_api_product_api(HttpClient client) throws Exception {
            final String productId = "jwt-product-expired";
            final String planId = "jwt-product-plan-expired";
            ReactableApiProduct p = product(productId, Set.of(API_1_ID));
            registerProductPlans(p, List.of(productJwtPlan(planId, PlanStatus.PUBLISHED)));
            deployApiProduct(p);
            allowJwtSubscriptionForApiAndPlan(API_1_ID, planId, JWT_CLIENT_ID);

            int status = getStatusWithHeader(client, API_1_PATH, "Authorization", "Bearer " + generateJWT(-5000));
            assertThat(status).isEqualTo(401);
        }
    }

    @Nested
    class MtlsSecurityTypeScenarios extends SecurityTestPreparer {

        private SubscriptionTrustStoreLoaderManager subscriptionTrustStoreLoaderManager;

        @Override
        protected void configureGateway(GatewayConfigurationBuilder config) {
            config
                .httpSecured(true)
                .set("http.ssl.clientAuth", "request")
                .set("http.ssl.keystore.type", KeyStoreLoader.CERTIFICATE_FORMAT_SELF_SIGNED);
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

        @BeforeEach
        void setupMtlsSubscriptionTrustStore() {
            subscriptionTrustStoreLoaderManager = getBean(SubscriptionTrustStoreLoaderManager.class);
            final SubscriptionCacheService subscriptionService = (SubscriptionCacheService) getBean(SubscriptionService.class);
            when(subscriptionService.getByApiAndSecurityToken(any(), any(), any())).thenCallRealMethod();
            ReflectionTestUtils.setField(subscriptionService, "subscriptionTrustStoreLoaderManager", subscriptionTrustStoreLoaderManager);
        }

        @Test
        @DeployApi(
            { "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json", "/apis/v4/http/api-product/api-3.json" }
        )
        void should_be_able_to_call_api_product_with_mtls_plan(@WithCert HttpClient client) throws Exception {
            final String productId = "mtls-product-success";
            final String planId = PLAN_MTLS_ID;
            ReactableApiProduct p = product(productId, Set.of(API_1_ID));
            registerProductPlans(p, List.of(productPlan(planId, "mtls", PlanStatus.PUBLISHED)));
            deployApiProduct(p);

            Subscription subscription = mtlsSubscription(API_1_ID, planId, false);
            subscriptionTrustStoreLoaderManager.registerSubscription(subscription, Set.of());
            try {
                assertThat(getStatus(client, API_1_PATH)).isEqualTo(200);
            } finally {
                subscriptionTrustStoreLoaderManager.unregisterSubscription(subscription);
            }
        }

        @Test
        @DeployApi(
            { "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json", "/apis/v4/http/api-product/api-3.json" }
        )
        void should_be_able_to_call_api_product_with_mtls_plan_and_several_cert(@WithCert HttpClient client) throws Exception {
            final String productId = "mtls-product-multi-cert";
            final String planId = PLAN_MTLS_ID;
            ReactableApiProduct p = product(productId, Set.of(API_1_ID));
            registerProductPlans(p, List.of(productPlan(planId, "mtls", PlanStatus.PUBLISHED)));
            deployApiProduct(p);

            Subscription subscription = mtlsSubscription(API_1_ID, planId, true);
            subscriptionTrustStoreLoaderManager.registerSubscription(subscription, Set.of());
            try {
                assertThat(getStatus(client, API_1_PATH)).isEqualTo(200);
            } finally {
                subscriptionTrustStoreLoaderManager.unregisterSubscription(subscription);
            }
        }

        @Test
        @DeployApi(
            { "/apis/v4/http/api-product/api-1.json", "/apis/v4/http/api-product/api-2.json", "/apis/v4/http/api-product/api-3.json" }
        )
        void should_not_be_able_to_call_api_product_with_mtls_plan_if_no_cert_in_request(HttpClient client) {
            final String productId = "mtls-product-no-cert";
            final String planId = PLAN_MTLS_ID;
            ReactableApiProduct p = product(productId, Set.of(API_1_ID));
            registerProductPlans(p, List.of(productPlan(planId, "mtls", PlanStatus.PUBLISHED)));
            deployApiProduct(p);

            assertThat(getStatus(client, API_1_PATH)).isEqualTo(401);
        }

        private Subscription mtlsSubscription(String apiId, String planId, boolean withPkcs7Bundle) throws IOException {
            final Subscription subscription = new Subscription();
            subscription.setApi(apiId);
            subscription.setApplication("application-id");
            subscription.setApplicationName("Application");
            subscription.setId("subscription-mtls-" + planId);
            subscription.setPlan(planId);
            final String clientCertificate = Files.readString(Paths.get(getUrl("plans/mtls/client.cer").getPath()));
            if (withPkcs7Bundle) {
                final String clientCertificate2 = Files.readString(Paths.get(getUrl("plans/mtls/client2.cer").getPath()));
                subscription.setClientCertificate(
                    Base64.getEncoder().encodeToString(PKCS7Utils.createBundle(List.of(clientCertificate, clientCertificate2)))
                );
            } else {
                subscription.setClientCertificate(Base64.getEncoder().encodeToString(clientCertificate.getBytes()));
            }
            return subscription;
        }
    }

    @GatewayTest
    // Empty @DeployApiProducts({}) signals that this test class needs the "universe" license tier for API Product features.
    @DeployApiProducts({})
    static class SecurityTestPreparer extends AbstractGatewayTest {

        private final Set<String> manuallyDeployedApiProducts = new HashSet<>();

        @BeforeEach
        void setupSecurityDefaults() {
            wiremock.stubFor(get("/endpoint").willReturn(ok("endpoint response")));
            when(getBean(ApiKeyService.class).getByApiAndKey(anyString(), anyString())).thenReturn(Optional.empty());
            when(getBean(SubscriptionService.class).getByApiAndSecurityToken(any(), any(), any())).thenReturn(Optional.empty());
        }

        @AfterEach
        void cleanupManuallyDeployedApiProducts() {
            Set.copyOf(manuallyDeployedApiProducts).forEach(this::undeployApiProduct);
        }

        @Override
        public void deployApiProduct(ReactableApiProduct reactableApiProduct) {
            manuallyDeployedApiProducts.add(reactableApiProduct.getId());
            super.deployApiProduct(reactableApiProduct);
        }

        @Override
        public void undeployApiProduct(String apiProductId) {
            manuallyDeployedApiProducts.remove(apiProductId);
            super.undeployApiProduct(apiProductId);
        }

        @Override
        public void redeployApiProduct(ReactableApiProduct reactableApiProduct) {
            manuallyDeployedApiProducts.add(reactableApiProduct.getId());
            super.redeployApiProduct(reactableApiProduct);
        }

        @Override
        public void configureApi(io.gravitee.gateway.reactor.ReactableApi<?> api, Class<?> definitionClass) {
            if (isV4Api(definitionClass)) {
                final io.gravitee.definition.model.v4.Api apiDefinition = (io.gravitee.definition.model.v4.Api) api.getDefinition();
                configurePlans(apiDefinition, Set.of("api-key"));
            }
        }

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            policies.put(
                "api-key",
                PolicyBuilder.build("api-key", ApiKeyPolicy.class, ApiKeyPolicyConfiguration.class, ApiKeyPolicyInitializer.class)
            );
            policies.put("jwt", PolicyBuilder.build("jwt", JWTPolicy.class, JWTPolicyConfiguration.class));
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

        int getStatus(HttpClient client, String path) {
            return client
                .rxRequest(GET, path)
                .flatMap(HttpClientRequest::rxSend)
                .map(response -> response.statusCode())
                .blockingGet();
        }

        int getStatusWithHeader(HttpClient client, String path, String headerName, String headerValue) {
            return client
                .rxRequest(GET, path)
                .flatMap(request -> {
                    request.putHeader(headerName, headerValue);
                    return request.rxSend();
                })
                .map(response -> response.statusCode())
                .blockingGet();
        }

        void allowJwtSubscriptionForApiAndPlan(String apiId, String planId, String clientId) {
            Subscription subscription = new Subscription();
            subscription.setApplication("application-id");
            subscription.setApplicationName("Application");
            subscription.setId("subscription-jwt-" + planId);
            subscription.setApi(apiId);
            subscription.setPlan(planId);
            when(
                getBean(SubscriptionService.class).getByApiAndSecurityToken(
                    eq(apiId),
                    argThat(
                        securityToken ->
                            securityToken.getTokenType().equals(SecurityToken.TokenType.CLIENT_ID.name()) &&
                            securityToken.getTokenValue().equals(clientId)
                    ),
                    eq(planId)
                )
            ).thenReturn(Optional.of(subscription));
        }

        void registerProductPlans(ReactableApiProduct product, List<Plan> plans) {
            product.setPlans(plans);
        }

        ReactableApiProduct product(String productId, Set<String> apiIds) {
            return ReactableApiProduct.builder()
                .id(productId)
                .name("Product-" + productId)
                .description("Scenario product " + productId)
                .version("1.0.0")
                .apiIds(apiIds)
                .environmentId(ENV_ID)
                .build();
        }

        Plan productPlan(String planId, String securityType, PlanStatus status) {
            return Plan.builder()
                .id(planId)
                .name("ProductPlan-" + planId)
                .security(PlanSecurity.builder().type(securityType).build())
                .status(status)
                .mode(PlanMode.STANDARD)
                .build();
        }

        Plan productJwtPlan(String planId, PlanStatus status) {
            try {
                JWTPolicyConfiguration configuration = new JWTPolicyConfiguration();
                configuration.setSignature(HMAC_HS256);
                configuration.setResolverParameter(JWT_SECRET);
                configuration.setPublicKeyResolver(GIVEN_KEY);
                return Plan.builder()
                    .id(planId)
                    .name("ProductPlan-" + planId)
                    .security(
                        PlanSecurity.builder().type("jwt").configuration(new ObjectMapper().writeValueAsString(configuration)).build()
                    )
                    .status(status)
                    .mode(PlanMode.STANDARD)
                    .build();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create JWT product plan configuration", e);
            }
        }
    }
}
