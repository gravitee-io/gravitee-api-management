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

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
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
import io.gravitee.apim.gateway.tests.sdk.utils.ResourceUtils;
import io.gravitee.common.service.AbstractService;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.reactor.ReactableApi;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.vault.VaultContainer;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@Testcontainers
@GatewayTest
public class SecretsV4IntegrationTest extends AbstractGatewayTest {

    private static final String VAULT_TOKEN = UUID.randomUUID().toString();

    @Container
    private static final VaultContainer vaultContainer = new VaultContainer<>("hashicorp/vault:1.13.3")
        .withVaultToken(VAULT_TOKEN)
        .withInitCommand(
            "kv put secret/test private-key='-----BEGIN PRIVATE KEY-----\nMIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCMOMvaM1XZzR1H\nPp127syCvuxnljnxsMLCvfM+8QsdyeuYVURei3z502zxWVTwNbxUbTxOkgIuNYRT\nyypEMpYajSEv0sMt4d7KBchE0eeQlcMDQ2J6kzfVjHyxLMMu8JscBQ5KGvF1vW3Q\np16w6C6JebF21Y0LumL9cMEToN6OrDKx9BrUkbTXHfSf+5rvkrnGfIPMulNJS2On\nl4zmT0bHs8a/zSIGmcwNIG243LmXTFu67TKbknJahICrRz0uZ8h1HMFbe7yY12s0\nd0xGJDJcPIBcdWHhB8z6iduTxCYZ2vY3EdFcC2RMO99zJPlWeDGY4lTM8TRFEAvE\nj4a21CltAgMBAAECggEADu4VNnx0zaX7UhSmq30tpVYy0ay7KrLJafbTqYX8ywUu\n4p9hkjeD7Q3H8cKzOoheLxcabrs5JDZqiol9TJmeReF1ASSNx5rfH9+RvVIkN87a\nXsST/b0jGsfElxDPD3Zq7YbUSKupvgGXaboIaQmvus+MR7zhMbh8xcN1q2NbjxE6\nCLXV+uu8kVPLsUbGFGK53vK7+MHnEKJIbjHR+LRNMxZRzoX7h57UO6uoXwWes2Kr\nopMzIJdv2ZRvXS62NNCTK9BoPb4OreXSoEJZkXS26ispOLLnM36D4UpNeDl6hITN\noCSHKiRGFqY5QOir/MP8uqNmGEtprMDmUQt/K5Z8OQKBgQDVvEi7SmHFJLNSsY2l\nvvxlwIa9rr0d/TFtTXcGMUdLYg7rQtaZ3e0geJv4wU58KahmpgYqpX4s0/Gc4L37\nyG1kCtPUjHvxxauLRr0OcXz35pTqlGhVGyls9onCxM/nDpoiYDvueuse69rVVGDE\nTKg2pxqS0Rxh7FARKY1RpJprjwKBgQCn8xsrRSc5QGF/L7b87MK1sf+lQ7Updubt\nv/XRFq5lhHp888MrIsqcqsDqRChoY2Qoi0vuNv52pgpha6lXkxL5Djd1ypX457KR\ntaJmx7qoolYW7INU4mvcryyIiW9ELiFrGb8XrwhXwmUBWOGGEDJkgYOj6W7Co38T\n2RbkBvBNQwKBgGTniv7A0v+bn/0+Tb0eOVJgXjxWrnnl+tu7YqHNyfbQyHJRD7d8\nimJ2DkyWFlOP5yzu3KJtlu/a74o8n/SqXtqIMhF6cVlnFOGf98lF0tXGSi+k+MyV\nEi2bBtaoy+4tep8YB7NC3JWwi5ODTlveRNvocCc4CcpBIlu33jvZFf4JAoGATJ0R\nn8OECRHdZ++UQfyfNdNlEza3xZp/7aTLtf3qwFSWq7lnJp5QXvdl2XgOFtCAOB6T\nHK/plKZZxece8Nweo45grlMj5s+LHf0FgG1MMPEc5IgvwOEo4xrl7cMEBs4kYH72\nNQ+bdq0u9lZdSpLI6iBKtNMfu5pptdwqHQstQ5ECgYBQ+XWJHuKLadcMAoAdG+gW\n5KxlSlin7PqgmzobXggp2aUGVLHs4pvIGA+7Sf/kAPfJ6bimv4dIODqEe8TZnmWp\n4ZG0jYjFXVnbwbh+hU5EF03ntO9WYqL6rYU3zujz/ZuKBEByIFowTX9uaVKEgML6\n5oHAF4Sb7zxa2jSEehGQ5Q==\n-----END PRIVATE KEY-----'"
        );

    private static String token;

    private final int backendPort = getAvailablePort();

    @Override
    protected void configureWireMock(WireMockConfiguration configuration) {
        configuration
            .httpsPort(backendPort)
            .needClientAuth(true)
            .keystorePath(ResourceUtils.toPath("certs/keystore01.jks"))
            .keystorePassword("password")
            .trustStorePath(ResourceUtils.toPath("certs/truststore01.jks"))
            .trustStorePassword("password");
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
    public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
        super.configureGateway(configurationBuilder);

        // create a renewable token so the plugin does not start panicking
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

    @Override
    public void configureApi(ReactableApi<?> reactableApi, Class<?> definitionClass) {
        if (reactableApi.getDefinition() instanceof Api apiDefinition) {
            updateEndpointsPort(apiDefinition, backendPort);
        } else {
            throw new AssertionError("Api should only be v4 api");
        }
    }

    @Test
    @DeployApi({ "/apis/v4/http/secrets/api-secured-jks-password-secret-static-ref.json" })
    void should_retrieve_password_from_secret_provider(HttpClient httpClient, Vertx vertx) {
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
    }
}
