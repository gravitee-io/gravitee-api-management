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
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.secrets.SecretProviderBuilder;
import io.gravitee.apim.integration.tests.secrets.SecuredVaultContainer;
import io.gravitee.apim.integration.tests.secrets.conf.SSLUtils;
import io.gravitee.common.service.AbstractService;
import io.gravitee.node.secrets.plugins.SecretProviderPlugin;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.secrets.api.plugin.SecretManagerConfiguration;
import io.gravitee.secrets.api.plugin.SecretProviderFactory;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class VaultHttpProxyHeaderSecretTest {

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

    static void addPlugin(
        Set<SecretProviderPlugin<? extends SecretProviderFactory<?>, ? extends SecretManagerConfiguration>> secretProviderPlugins
    ) {
        secretProviderPlugins.add(
            SecretProviderBuilder.build(HCVaultSecretProvider.PLUGIN_ID, HCVaultSecretProviderFactory.class, VaultConfig.class)
        );
    }

    abstract static class AbstractVaultApiTest extends AbstractGatewayTest {

        final Set<String> createSecrets = new HashSet<>();

        @Override
        public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
            configurationBuilder.setYamlProperty("api.secrets.providers[0].plugin", "vault");
            configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.enabled", true);
            vaultInstanceConfig(vaultContainer).forEach(configurationBuilder::setYamlProperty);
            authConfig().forEach(configurationBuilder::setYamlProperty);
        }

        Map<String, Object> vaultInstanceConfig(SecuredVaultContainer vaultContainer) {
            return Map.of(
                "api.secrets.providers[0].configuration.host",
                vaultContainer.getHost(),
                "api.secrets.providers[0].configuration.port",
                vaultContainer.getPort(),
                "api.secrets.providers[0].configuration.ssl.enabled",
                true,
                "api.secrets.providers[0].configuration.ssl.format",
                "pemfile",
                "api.secrets.providers[0].configuration.ssl.file",
                SecuredVaultContainer.CERT_PEMFILE
            );
        }

        protected Map<String, Object> authConfig() {
            return Map.of(
                "api.secrets.providers[0].configuration.auth.method",
                "userpass",
                "api.secrets.providers[0].configuration.auth.config.username",
                SecuredVaultContainer.USER_ID,
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
        public void configureSecretProviders(
            Set<SecretProviderPlugin<? extends SecretProviderFactory<?>, ? extends SecretManagerConfiguration>> secretProviderPlugins
        ) throws Exception {
            addPlugin(secretProviderPlugins);
            createSecrets();
        }

        @Override
        public void configureServices(Set<Class<? extends AbstractService<?>>> services) {
            super.configureServices(services);
            services.add(SecretsService.class);
        }

        abstract void createSecrets() throws IOException, InterruptedException, VaultException;

        final void writeSecret(String path, Map<String, Object> data) throws VaultException {
            LogicalResponse write = rootVault.logical().write(path, data);
            assertThat(write.getRestResponse().getStatus()).isLessThan(300);
            createSecrets.add(path);
        }

        @AfterEach
        final void cleanSecrets() throws VaultException {
            for (String path : createSecrets) {
                rootVault.logical().delete(path);
            }
        }
    }

    abstract static class AbstractApiKeyStaticSecretRefTest extends AbstractVaultApiTest {

        protected final String apiKey = UUID.randomUUID().toString();

        @Override
        void createSecrets() throws VaultException {
            writeSecret("secret/test", Map.of("api-key", apiKey));
        }

        protected void callAndAssert(HttpClient httpClient) {
            wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

            httpClient
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .flatMap(response -> {
                    // just asserting we get a response (hence no SSL errors), no need for an API.
                    assertThat(response.statusCode()).isEqualTo(200);
                    return response.body();
                })
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertComplete();

            wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")).withHeader("Authorization", equalTo("ApiKey ".concat(apiKey))));
        }
    }

    @Nested
    @GatewayTest
    class StaticSecretRef extends AbstractApiKeyStaticSecretRefTest {

        @Test
        @DeployApi("/apis/v4/http/secrets/vault/api-static-ref.json")
        void should_call_api_with_vault_api_key_from_static_ref(HttpClient httpClient) {
            callAndAssert(httpClient);
        }
    }

    @Nested
    @GatewayTest
    class StaticSecretRefELKey extends AbstractApiKeyStaticSecretRefTest {

        @Test
        @DeployApi("/apis/v4/http/secrets/vault/api-el-key-ref.json")
        void should_call_api_with_vault_api_key_from_static_ref_and_el_key(HttpClient httpClient) {
            callAndAssert(httpClient);
        }
    }

    @Nested
    @GatewayTest
    class StaticSecretRefELURI extends AbstractApiKeyStaticSecretRefTest {

        @Test
        @DeployApi("/apis/v4/http/secrets/vault/api-el-ref.json")
        void should_call_api_with_vault_api_key_el_ref(HttpClient httpClient) {
            callAndAssert(httpClient);
        }
    }
}
