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

package io.gravitee.apim.integration.tests.http.secrets;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.graviteesource.secretprovider.hcvault.HCVaultSecretProvider;
import com.graviteesource.secretprovider.hcvault.HCVaultSecretProviderFactory;
import com.graviteesource.secretprovider.hcvault.config.manager.VaultConfig;
import com.graviteesource.service.secrets.SecretsService;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.secrets.SecretProviderBuilder;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.dictionary.model.Dictionary;
import io.gravitee.node.secrets.plugins.SecretProviderPlugin;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.secrets.api.plugin.SecretManagerConfiguration;
import io.gravitee.secrets.api.plugin.SecretProviderFactory;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.vault.VaultContainer;

/**
 * @author GraviteeSource Team
 */
@Testcontainers
@GatewayTest
public class SecretsRenewalV4IntegrationTest extends AbstractGatewayTest {

    private static final String VAULT_TOKEN = UUID.randomUUID().toString();

    @Container
    private static final VaultContainer<?> vaultContainer = new VaultContainer<>("hashicorp/vault:1.13.3")
        .withVaultToken(VAULT_TOKEN)
        .withInitCommand(
            "kv put secret/test" +
                " value1='initial value1'" +
                " value2='initial value2'" +
                " value3='initial value3'" +
                " value4='initial value4'" +
                " value5='initial value5'",
            "kv put secret/test2 value1='initial testValue1'"
        );

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
    }

    @Override
    public void configureDictionaries(List<Dictionary> dictionaries) {
        Dictionary testDictionary = new Dictionary();
        testDictionary.setId("test");
        testDictionary.setKey("test");
        testDictionary.setEnvironmentId("DEFAULT");
        testDictionary.setProperties(Map.of("value4", "/vault/secret/test:value4?renewable=true"));
        dictionaries.add(testDictionary);
    }

    @Override
    public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
        super.configureGateway(configurationBuilder);

        // create a renewable token so the plugin does not start panicking
        String token;
        try {
            token = vaultContainer.execInContainer("vault", "token", "create", "-period=10m", "-field", "token").getStdout();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        configurationBuilder.setYamlProperty("api.secrets.providers[0].plugin", "vault");
        configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.enabled", true);
        configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.host", vaultContainer.getHost());
        configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.port", vaultContainer.getMappedPort(8200));
        configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.ssl.enabled", "false");
        configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.auth.method", "token");
        configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.auth.config.token", token);
        configurationBuilder.setYamlProperty("api.secrets.renewal.enabled", true);
        configurationBuilder.setYamlProperty("api.secrets.renewal.check.delay", "2");
        configurationBuilder.setYamlProperty("api.secrets.renewal.check.unit", "SECONDS");
        configurationBuilder.setYamlProperty("api.secrets.renewal.defaultSecretTtl.delay", "2");
        configurationBuilder.setYamlProperty("api.secrets.renewal.defaultSecretTtl.unit", "SECONDS");
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
    public void configureServices(Set<Class<? extends AbstractService<?>>> services) {
        super.configureServices(services);
        services.add(SecretsService.class);
    }

    @AfterAll
    static void cleanup() {
        vaultContainer.close();
    }

    @SneakyThrows
    @Test
    @DeployApi({ "/apis/v4/http/secrets/api-with-secrets.json" })
    void should_retrieve_password_from_secret_provider_when_it_changes(HttpClient httpClient, Vertx vertx) {
        wiremock.stubFor(get("/echo").willReturn(ok("response from backend")));

        // Call the API and check the secret is retrieved correctly
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
        wiremock.verify(
            1,
            getRequestedFor(urlPathEqualTo("/echo"))
                .withHeader("X-Secret-URI", equalTo("initial value1 initial testValue1"))
                .withHeader("X-Secret-URI-renewable", equalTo("initial value2"))
                .withHeader("X-Secret-URI-reloadOnChange", equalTo("initial value3"))
                .withHeader("X-Secret-Dictionary", equalTo("initial value4"))
                .withHeader("X-Secret-Properties", equalTo("initial value5"))
        );

        // Update the secret in the provider
        vaultContainer.execInContainer(
            "vault",
            "kv",
            "put",
            "secret/test",
            "value1=updated value1",
            "value2=updated value2",
            "value3=updated value3",
            "value4=updated value4",
            "value5=updated value5"
        );

        // Wait and check that the API retrieves the updated secret
        TimeUnit.SECONDS.sleep(5);
        await()
            .pollInterval(1, TimeUnit.SECONDS)
            .atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
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

                wiremock.verify(
                    1,
                    getRequestedFor(urlPathEqualTo("/echo"))
                        .withHeader("X-Secret-URI", equalTo("updated value1 initial testValue1")) // As the full secret is refreshed, all values are updated and benefit to all specs.
                        .withHeader("X-Secret-URI-renewable", equalTo("updated value2"))
                        .withHeader("X-Secret-URI-reloadOnChange", equalTo("updated value3"))
                        .withHeader("X-Secret-Dictionary", equalTo("updated value4"))
                        .withHeader("X-Secret-Properties", equalTo("updated value5"))
                );
            });
    }
}
