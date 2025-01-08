package io.gravitee.apim.integration.tests.secrets.api.v4;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.dajudge.kindcontainer.KindContainer;
import com.dajudge.kindcontainer.KindContainerVersion;
import com.graviteesource.service.secrets.SecretsService;
import com.redis.testcontainers.RedisContainer;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.resource.ResourceBuilder;
import io.gravitee.apim.gateway.tests.sdk.secrets.SecretProviderBuilder;
import io.gravitee.apim.integration.tests.secrets.KubernetesHelper;
import io.gravitee.common.service.AbstractService;
import io.gravitee.node.secrets.plugins.SecretProviderPlugin;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.resource.ResourcePlugin;
import io.gravitee.policy.cache.CachePolicy;
import io.gravitee.policy.cache.configuration.CachePolicyConfiguration;
import io.gravitee.resource.cache.redis.RedisCacheResource;
import io.gravitee.resource.cache.redis.configuration.RedisCacheResourceConfiguration;
import io.gravitee.secretprovider.kubernetes.KubernetesSecretProvider;
import io.gravitee.secretprovider.kubernetes.KubernetesSecretProviderFactory;
import io.gravitee.secretprovider.kubernetes.config.K8sConfig;
import io.gravitee.secrets.api.plugin.SecretManagerConfiguration;
import io.gravitee.secrets.api.plugin.SecretProviderFactory;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@GatewayTest
class RedisCacheSecretTest extends AbstractGatewayTest {

    private static final String REDIS_PASSWORD = "thisIsTheRedisPassword";
    static Path kubeConfigFile;
    static KindContainer<?> kubeContainer;

    static RedisContainer redisContainer;

    @AfterAll
    static void cleanup() throws IOException {
        kubeContainer.close();
        Files.deleteIfExists(kubeConfigFile);
        redisContainer.close();
    }

    // not call by JUnit, as needs to be started before API is deployed
    static void startContainers() throws IOException {
        if (redisContainer == null) {
            redisContainer =
                new RedisContainer(DockerImageName.parse("redis:7.4.2"))
                        .withCommand("redis-server", "--appendonly", "yes", "--requirepass", REDIS_PASSWORD)
                    .withLogConsumer(f -> System.out.println(f.getUtf8String()));
            redisContainer.start();
        }
        if (kubeContainer == null) {
            kubeContainer = new KindContainer<>(KindContainerVersion.VERSION_1_29_1);
            kubeContainer.start();
            // write config so the secret provider can pick it up
            Files.writeString(kubeConfigFile, kubeContainer.getKubeconfig());
        }
    }

    @Override
    public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
        try {
            kubeConfigFile =
                Files.createTempDirectory(KubernetesHttpProxyHeaderSecretTest.class.getSimpleName()).resolve("kube_config.yml");

            startContainers();
            createSecrets();

            configurationBuilder.setYamlProperty("api.secrets.providers[0].plugin", "kubernetes");
            configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.enabled", true);
            configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.kubeConfigFile", kubeConfigFile.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void configureSecretProviders(
        Set<SecretProviderPlugin<? extends SecretProviderFactory<?>, ? extends SecretManagerConfiguration>> secretProviderPlugins
    ) {
        secretProviderPlugins.add(
            SecretProviderBuilder.build(KubernetesSecretProvider.PLUGIN_ID, KubernetesSecretProviderFactory.class, K8sConfig.class)
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
    public void configureResources(Map<String, ResourcePlugin> resources) {
        resources.putIfAbsent(
            "cache-redis",
            ResourceBuilder.build("cache-redis", RedisCacheResource.class, RedisCacheResourceConfiguration.class)
        );
    }

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.putIfAbsent("cache", PolicyBuilder.build("cache", CachePolicy.class, CachePolicyConfiguration.class));
    }

    @Override
    public void configureServices(Set<Class<? extends AbstractService<?>>> services) {
        services.add(SecretsService.class);
    }

    void createSecrets() throws IOException, InterruptedException {
        KubernetesHelper.createSecret(kubeContainer, "default", "redis", Map.of("password", REDIS_PASSWORD));
    }

    @Override
    public void configurePlaceHolderVariables(Map<String, String> variables) {
        variables.put("REDIS_PORT", String.valueOf(redisContainer.getRedisPort()));
    }

    @Test
    @DeployApi("/apis/v4/http/secrets/redis/api-static-ref.json")
    void should_call_twice_but_one_time_the_backend(HttpClient httpClient) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

        callEndpoint(httpClient);
        callEndpoint(httpClient);

        // response has been cached, hence should only be called once
        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));
    }

    private static void callEndpoint(HttpClient httpClient) {
        httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(HttpClientRequest::rxSend)
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.body();
            })
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertValue(Buffer.buffer("response from backend"))
            .assertComplete();
    }
}
