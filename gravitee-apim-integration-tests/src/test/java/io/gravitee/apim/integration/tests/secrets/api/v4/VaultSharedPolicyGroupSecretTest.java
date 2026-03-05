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
package io.gravitee.apim.integration.tests.secrets.api.v4;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.secretprovider.hcvault.HCVaultSecretProvider;
import com.graviteesource.secretprovider.hcvault.HCVaultSecretProviderFactory;
import com.graviteesource.secretprovider.hcvault.config.manager.VaultConfig;
import com.graviteesource.service.secrets.SecretsService;
import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.response.LogicalResponse;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeploySharedPolicyGroups;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.secrets.SecretProviderBuilder;
import io.gravitee.apim.integration.tests.secrets.SecuredVaultContainer;
import io.gravitee.apim.integration.tests.secrets.conf.SSLUtils;
import io.gravitee.common.service.AbstractService;
import io.gravitee.node.secrets.plugins.SecretProviderPlugin;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.policy.transformheaders.TransformHeadersPolicy;
import io.gravitee.policy.transformheaders.configuration.TransformHeadersPolicyConfiguration;
import io.gravitee.secrets.api.plugin.SecretManagerConfiguration;
import io.gravitee.secrets.api.plugin.SecretProviderFactory;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration test verifying that secrets ({@code {#secrets.get(...)}}) are correctly resolved
 * inside SharedPolicyGroup policy configurations.
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class VaultSharedPolicyGroupSecretTest {

    static SecuredVaultContainer vaultContainer;
    static Vault rootVault;

    @AfterAll
    static void cleanup() {
        vaultContainer.close();
    }

    @BeforeAll
    static void createVaultContainer() throws IOException, InterruptedException, VaultException {
        vaultContainer = new SecuredVaultContainer();
        vaultContainer.start();
        vaultContainer.initAndUnsealVault();
        vaultContainer.loginAndLoadTestPolicy();
        vaultContainer.setEngineVersions();
        vaultContainer.setupUserPassAuth();
        SSLUtils.SSLPairs clientCertAndKey = SSLUtils.createPairs();
        vaultContainer.setupCertAuth(clientCertAndKey.cert());
        rootVault = vaultContainer.getRootVault();
    }

    @Nested
    @GatewayTest
    @DeploySharedPolicyGroups("/sharedpolicygroups/spg-secret-header-on-request.json")
    class SharedPolicyGroupWithStaticSecretRef extends AbstractGatewayTest {

        final String apiKey = UUID.randomUUID().toString();

        @Override
        public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder.setYamlProperty("api.secrets.providers[0].plugin", "vault");
            configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.enabled", true);
            configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.host", vaultContainer.getHost());
            configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.port", vaultContainer.getPort());
            configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.ssl.enabled", true);
            configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.ssl.format", "pemfile");
            configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.ssl.file", SecuredVaultContainer.CERT_PEMFILE);
            configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.auth.method", "userpass");
            configurationBuilder.setYamlProperty(
                "api.secrets.providers[0].configuration.auth.config.username",
                SecuredVaultContainer.USER_ID
            );
            configurationBuilder.setYamlProperty(
                "api.secrets.providers[0].configuration.auth.config.password",
                SecuredVaultContainer.PASSWORD
            );
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
            policies.putIfAbsent(
                "transform-headers",
                PolicyBuilder.build("transform-headers", TransformHeadersPolicy.class, TransformHeadersPolicyConfiguration.class)
            );
        }

        @Override
        public void configureSecretProviders(
            Set<SecretProviderPlugin<? extends SecretProviderFactory<?>, ? extends SecretManagerConfiguration>> secretProviderPlugins
        ) throws Exception {
            secretProviderPlugins.add(
                SecretProviderBuilder.build(HCVaultSecretProvider.PLUGIN_ID, HCVaultSecretProviderFactory.class, VaultConfig.class)
            );
            LogicalResponse write = rootVault.logical().write("secret/test", Map.of("api-key", apiKey));
            assertThat(write.getRestResponse().getStatus()).isLessThan(300);
        }

        @Override
        public void configureServices(Set<Class<? extends AbstractService<?>>> services) {
            super.configureServices(services);
            services.add(SecretsService.class);
        }

        @Test
        @DeployApi("/apis/v4/http/secrets/vault/api-spg-secret.json")
        void should_resolve_secret_in_shared_policy_group(HttpClient httpClient) {
            wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

            httpClient
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    return response.body();
                })
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertComplete();

            wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")).withHeader("Authorization", equalTo("ApiKey ".concat(apiKey))));
        }
    }
}
