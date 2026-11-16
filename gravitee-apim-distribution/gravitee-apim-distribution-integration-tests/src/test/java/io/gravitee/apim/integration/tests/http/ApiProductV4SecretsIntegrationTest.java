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

import static io.gravitee.policy.jwt.alg.Signature.HMAC_HS256;
import static io.gravitee.policy.v3.jwt.resolver.KeyResolver.GIVEN_KEY;
import static io.vertx.core.http.HttpMethod.GET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graviteesource.secretprovider.hcvault.HCVaultSecretProvider;
import com.graviteesource.secretprovider.hcvault.HCVaultSecretProviderFactory;
import com.graviteesource.secretprovider.hcvault.config.manager.VaultConfig;
import com.graviteesource.service.secrets.SecretsService;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApiProducts;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.resource.ResourceBuilder;
import io.gravitee.apim.gateway.tests.sdk.secrets.SecretProviderBuilder;
import io.gravitee.common.service.AbstractService;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.manager.ApiProductManager;
import io.gravitee.gateway.reactive.handlers.api.v4.product.ApiProductRuntime;
import io.gravitee.gateway.reactive.handlers.api.v4.product.ApiProductRuntimeRegistry;
import io.gravitee.node.secrets.plugins.SecretProviderPlugin;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.resource.ResourcePlugin;
import io.gravitee.policy.basicauth.BasicAuthenticationPolicy;
import io.gravitee.policy.basicauth.configuration.BasicAuthenticationPolicyConfiguration;
import io.gravitee.policy.jwt.configuration.JWTPolicyConfiguration;
import io.gravitee.resource.authprovider.inline.InlineAuthenticationProviderResource;
import io.gravitee.resource.authprovider.inline.configuration.InlineAuthenticationProviderResourceConfiguration;
import io.gravitee.secrets.api.plugin.SecretManagerConfiguration;
import io.gravitee.secrets.api.plugin.SecretProviderFactory;
import io.vertx.rxjava3.core.http.HttpClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.vault.VaultContainer;

/**
 * Secret references in an API Product are discovered with the product version its runtime is built from, and keep
 * resolving across updates.
 */
@GatewayTest
@DeployApiProducts({})
@DeployApi({ "/apis/v4/http/api-product/api-1.json" })
class ApiProductV4SecretsIntegrationTest extends ApiProductV4RuntimeIntegrationTest.RuntimeTestPreparer {

    private static final String VAULT_TOKEN = UUID.randomUUID().toString();
    private static final String SECRET_VALUE = "value-from-vault";
    private static final String SECRET_REF = "{#secrets.get('/vault/secret/product:value')}";
    private static final String PRODUCT_ID = "runtime-product";
    private static final String PRODUCT_PLAN_ID = "runtime-product-plan";
    private static final String API_1_ID = "my-api-v4-1";
    private static final String API_1_PATH = "/test-1";
    private static final String KEY = "runtime-product-key";
    private static final String RESPONSE_VERSION_HEADER = "X-Product-Response-Version";
    private static final String BASIC_AUTH_POLICY = "policy-basic-authentication";
    private static final String BASIC_AUTH_USER = "product-user";
    private static final String INLINE_USERS_RESOURCE = "product-users";
    private static final String INLINE_USERS_RESOURCE_TYPE = "auth-provider-inline-resource";

    private static final VaultContainer<?> vaultContainer = new VaultContainer<>("hashicorp/vault:1.13.3")
        .withVaultToken(VAULT_TOKEN)
        .withInitCommand("kv put secret/product value=" + SECRET_VALUE);

    // The gateway extension inherited from the parent class configures the gateway before any container extension runs.
    static {
        vaultContainer.start();
    }

    private long deployments;

    @AfterAll
    static void cleanup() {
        vaultContainer.close();
    }

    @Override
    protected void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
        super.configureGateway(configurationBuilder);
        String token;
        try {
            token = vaultContainer.execInContainer("vault", "token", "create", "-period=10m", "-field", "token").getStdout();
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Unable to create a Vault token", e);
        }
        configurationBuilder.setYamlProperty("api.secrets.providers[0].plugin", "vault");
        configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.enabled", true);
        configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.host", vaultContainer.getHost());
        configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.port", vaultContainer.getMappedPort(8200));
        configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.ssl.enabled", "false");
        configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.auth.method", "token");
        configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.auth.config.token", token);
    }

    @Override
    public void configureSecretProviders(
        Set<SecretProviderPlugin<? extends SecretProviderFactory<?>, ? extends SecretManagerConfiguration>> secretProviderPlugins
    ) {
        secretProviderPlugins.add(
            SecretProviderBuilder.build(HCVaultSecretProvider.PLUGIN_ID, HCVaultSecretProviderFactory.class, VaultConfig.class)
        );
    }

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        super.configurePolicies(policies);
        policies.put(
            BASIC_AUTH_POLICY,
            PolicyBuilder.build(BASIC_AUTH_POLICY, BasicAuthenticationPolicy.class, BasicAuthenticationPolicyConfiguration.class)
        );
    }

    @Override
    public void configureResources(Map<String, ResourcePlugin> resources) {
        super.configureResources(resources);
        resources.put(
            INLINE_USERS_RESOURCE_TYPE,
            ResourceBuilder.build(
                INLINE_USERS_RESOURCE_TYPE,
                InlineAuthenticationProviderResource.class,
                InlineAuthenticationProviderResourceConfiguration.class
            )
        );
    }

    @Override
    public void configureServices(Set<Class<? extends AbstractService<?>>> services) {
        super.configureServices(services);
        services.add(SecretsService.class);
    }

    @Test
    void should_resolve_a_secret_reference_in_a_product_plan_flow(HttpClient client) {
        allowProductKey();
        deployApiProduct(productWithSecretInPlanFlow("v1"));

        assertThat(call(client).responseVersion()).isEqualTo(SECRET_VALUE);
    }

    @Test
    void should_keep_resolving_the_secret_after_updates_of_the_product_once_the_replaced_versions_are_revoked(HttpClient client) {
        allowProductKey();
        deployApiProduct(productWithSecretInPlanFlow("v1"));
        ApiProductRuntime v1Runtime = publishedRuntime(PRODUCT_ID);
        ReactableApiProduct planRenamed = productWithSecretInPlanFlow("v2");
        planRenamed.getPlans().getFirst().setName("Renamed plan");
        deployApiProduct(planRenamed);
        ReactableApiProduct described = productWithSecretInPlanFlow("v3");
        described.getPlans().getFirst().setName("Renamed plan");
        described.setDescription("Described");
        deployApiProduct(described);
        await().atMost(10, TimeUnit.SECONDS).until(v1Runtime::isStopped);

        assertThat(call(client).responseVersion()).isEqualTo(SECRET_VALUE);
    }

    @Test
    void should_resolve_a_secret_reference_in_a_product_resource_used_by_a_product_plan_flow(HttpClient client) {
        allowProductKey();
        deployApiProduct(productWithSecretInResource());

        assertThat(callWithBasicAuth(client, SECRET_VALUE)).isEqualTo(200);
        assertThat(callWithBasicAuth(client, "not-the-secret")).isEqualTo(401);
    }

    private ApiProductRuntime publishedRuntime(String productId) {
        ApiProductRuntime runtime = getBean(ApiProductRuntimeRegistry.class).acquire(productId, "DEFAULT");
        runtime.release();
        return runtime;
    }

    private int callWithBasicAuth(HttpClient client, String password) {
        String credentials = Base64.getEncoder().encodeToString((BASIC_AUTH_USER + ":" + password).getBytes(StandardCharsets.UTF_8));
        return client
            .rxRequest(GET, API_1_PATH)
            .flatMap(request -> request.putHeader("X-Gravitee-Api-Key", KEY).putHeader("Authorization", "Basic " + credentials).rxSend())
            .flatMap(response -> response.rxBody().map(body -> response.statusCode()))
            .blockingGet();
    }

    private ReactableApiProduct productWithSecretInResource() {
        HttpSelector selector = new HttpSelector();
        selector.setPath("/");
        Plan plan = productPlan(PRODUCT_PLAN_ID, "api-key", PlanStatus.PUBLISHED);
        plan.setFlows(
            List.of(
                Flow.builder()
                    .name("basic authentication")
                    .enabled(true)
                    .selectors(List.of(selector))
                    .request(
                        List.of(
                            Step.builder()
                                .name("basic authentication")
                                .enabled(true)
                                .policy(BASIC_AUTH_POLICY)
                                .configuration(
                                    "{\"authenticationProviders\":[\"" + INLINE_USERS_RESOURCE + "\"],\"realm\":\"gravitee.io\"}"
                                )
                                .build()
                        )
                    )
                    .build()
            )
        );
        ReactableApiProduct product = product(PRODUCT_ID, Set.of(API_1_ID));
        product.setDeployedAt(new Date(++deployments));
        product.setPlans(List.of(plan));
        product.setResources(
            List.of(
                Resource.builder()
                    .name(INLINE_USERS_RESOURCE)
                    .type(INLINE_USERS_RESOURCE_TYPE)
                    .enabled(true)
                    .configuration("{\"users\":[{\"username\":\"" + BASIC_AUTH_USER + "\",\"password\":\"" + SECRET_REF + "\"}]}")
                    .build()
            )
        );
        return product;
    }

    @Test
    void should_discover_a_secret_reference_in_the_plan_security_of_the_runtime_and_keep_the_deployed_version_as_sent() throws Exception {
        JWTPolicyConfiguration configuration = new JWTPolicyConfiguration();
        configuration.setSignature(HMAC_HS256);
        configuration.setPublicKeyResolver(GIVEN_KEY);
        configuration.setResolverParameter(SECRET_REF);
        ReactableApiProduct product = product("jwt-secret-product", Set.of(API_1_ID));
        product.setDeployedAt(new Date(++deployments));
        Plan plan = productPlan("jwt-secret-plan", "jwt", PlanStatus.PUBLISHED);
        plan.setSecurity(PlanSecurity.builder().type("jwt").configuration(new ObjectMapper().writeValueAsString(configuration)).build());
        product.setPlans(List.of(plan));

        deployApiProduct(product);

        ApiProductRuntime runtime = getBean(ApiProductRuntimeRegistry.class).acquire("jwt-secret-product", "DEFAULT");
        String runtimeSecurity;
        try {
            runtimeSecurity = runtime.product().getPlans().getFirst().getSecurity().getConfiguration();
        } finally {
            runtime.release();
        }
        String deployedSecurity = getBean(ApiProductManager.class)
            .get("jwt-secret-product")
            .getPlans()
            .getFirst()
            .getSecurity()
            .getConfiguration();
        assertThat(runtimeSecurity).contains("#secrets.fromGrant(").doesNotContain("#secrets.get(");
        assertThat(deployedSecurity).contains("#secrets.get(");
    }

    private ReactableApiProduct productWithSecretInPlanFlow(String version) {
        HttpSelector selector = new HttpSelector();
        selector.setPath("/");
        Plan plan = productPlan(PRODUCT_PLAN_ID, "api-key", PlanStatus.PUBLISHED);
        plan.setFlows(
            List.of(
                Flow.builder()
                    .name("secret header")
                    .enabled(true)
                    .selectors(List.of(selector))
                    .response(
                        List.of(
                            Step.builder()
                                .name("add secret header")
                                .enabled(true)
                                .policy("transform-headers")
                                .configuration(
                                    "{\"addHeaders\":[{\"name\":\"" + RESPONSE_VERSION_HEADER + "\",\"value\":\"" + SECRET_REF + "\"}]}"
                                )
                                .build()
                        )
                    )
                    .build()
            )
        );
        ReactableApiProduct product = product(PRODUCT_ID, Set.of(API_1_ID));
        product.setVersion(version);
        product.setDeployedAt(new Date(++deployments));
        product.setPlans(List.of(plan));
        return product;
    }
}
