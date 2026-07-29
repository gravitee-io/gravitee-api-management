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

import com.dajudge.kindcontainer.KindContainer;
import com.dajudge.kindcontainer.KindContainerVersion;
import com.graviteesource.service.secrets.SecretsService;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.secrets.SecretProviderBuilder;
import io.gravitee.apim.integration.tests.secrets.KubernetesHelper;
import io.gravitee.common.service.AbstractService;
import io.gravitee.node.secrets.plugins.SecretProviderPlugin;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.secretprovider.kubernetes.KubernetesSecretProvider;
import io.gravitee.secretprovider.kubernetes.KubernetesSecretProviderFactory;
import io.gravitee.secretprovider.kubernetes.config.K8sConfig;
import io.gravitee.secrets.api.plugin.SecretManagerConfiguration;
import io.gravitee.secrets.api.plugin.SecretProviderFactory;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@GatewayTest
class KubernetesHttpProxyHeaderSecretTest extends AbstractGatewayTest {

    static Path kubeConfigFile;
    static KindContainer<?> kubeContainer;
    final String apiKey = UUID.randomUUID().toString();

    @AfterAll
    static void cleanup() throws IOException {
        kubeContainer.close();
        Files.deleteIfExists(kubeConfigFile);
    }

    // not call by JUnit, as needs to be started before API is deployed
    static void startK8s() throws IOException {
        if (kubeContainer == null) {
            kubeContainer = new KindContainer<>(KindContainerVersion.VERSION_1_29_1);
            kubeContainer.start();
            // write config so the secret provider can pick it up
            Files.writeString(kubeConfigFile, kubeContainer.getKubeconfig());
        }
    }

    @Override
    public void configureSecretProviders(
        Set<SecretProviderPlugin<? extends SecretProviderFactory<?>, ? extends SecretManagerConfiguration>> secretProviderPlugins
    ) throws Exception {
        secretProviderPlugins.add(
            SecretProviderBuilder.build(KubernetesSecretProvider.PLUGIN_ID, KubernetesSecretProviderFactory.class, K8sConfig.class)
        );
        startK8s();
        createSecrets();
    }

    @Override
    public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
        try {
            kubeConfigFile = Files.createTempDirectory(KubernetesHttpProxyHeaderSecretTest.class.getSimpleName()).resolve(
                "kube_config.yml"
            );
            configurationBuilder.setYamlProperty("api.secrets.providers[0].plugin", "kubernetes");
            configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.enabled", true);
            configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.kubeConfigFile", kubeConfigFile.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
    public void configureServices(Set<Class<? extends AbstractService<?>>> services) {
        super.configureServices(services);
        services.add(SecretsService.class);
    }

    // @Override
    void createSecrets() throws IOException, InterruptedException {
        KubernetesHelper.createSecret(kubeContainer, "default", "test", Map.of("api-key", this.apiKey));
    }

    protected void callAndAssert(HttpClient httpClient) {
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

    @Test
    @DeployApi("/apis/v4/http/secrets/k8s/api-static-ref.json")
    void should_call_api_with_k8s_api_key_from_static_ref(HttpClient httpClient) {
        callAndAssert(httpClient);
    }

    @Test
    @DeployApi("/apis/v4/http/secrets/k8s/api-el-key-ref.json")
    void should_call_api_with_k8s_api_key_from_static_ref_and_el_key(HttpClient httpClient) {
        callAndAssert(httpClient);
    }

    @Test
    @DeployApi("/apis/v4/http/secrets/k8s/api-el-ref.json")
    void should_call_api_with_k8s_api_key_el_ref(HttpClient httpClient) {
        callAndAssert(httpClient);
    }
}
