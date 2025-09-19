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
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.dajudge.kindcontainer.KindContainer;
import com.dajudge.kindcontainer.KindContainerVersion;
import com.graviteesource.service.secrets.SecretsService;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
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
import io.gravitee.policy.basicauth.BasicAuthenticationPolicy;
import io.gravitee.policy.basicauth.configuration.BasicAuthenticationPolicyConfiguration;
import io.gravitee.policy.cache.CachePolicy;
import io.gravitee.policy.cache.configuration.CachePolicyConfiguration;
import io.gravitee.policy.oauth2.configuration.OAuth2PolicyConfiguration;
import io.gravitee.policy.v3.oauth2.Oauth2PolicyV3;
import io.gravitee.resource.authprovider.ldap.LdapAuthenticationProviderResource;
import io.gravitee.resource.authprovider.ldap.configuration.LdapAuthenticationProviderResourceConfiguration;
import io.gravitee.resource.cache.redis.RedisCacheResource;
import io.gravitee.resource.cache.redis.configuration.RedisCacheResourceConfiguration;
import io.gravitee.resource.oauth2.generic.OAuth2GenericResource;
import io.gravitee.resource.oauth2.generic.configuration.OAuth2ResourceConfiguration;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@GatewayTest
class ResourceSecretTest extends AbstractGatewayTest {

    private static final String REDIS_PASSWORD = "thisIsTheRedisPassword";
    private static final String CLIENT_SECRET = "adminPassword";
    private static final String LDAP_PASSWORD = "GoodNewsEveryone";
    static final int LDAP_PORT = 10389;
    static Path kubeConfigFile;
    static KindContainer<?> kubeContainer;

    static RedisContainer redisContainer;
    private static GenericContainer<?> ldapServer;

    @AfterAll
    static void cleanup() throws IOException {
        kubeContainer.close();
        Files.deleteIfExists(kubeConfigFile);
        redisContainer.close();
        ldapServer.close();
    }

    // not call by JUnit, as needs to be started before API is deployed
    static void startContainers() throws IOException {
        if (redisContainer == null) {
            redisContainer = new RedisContainer(DockerImageName.parse("redis:7.4.2")).withCommand(
                "redis-server",
                "--appendonly",
                "yes",
                "--requirepass",
                REDIS_PASSWORD
            );
            redisContainer.start();
        }
        if (kubeContainer == null) {
            kubeContainer = new KindContainer<>(KindContainerVersion.VERSION_1_29_1);
            kubeContainer.start();
            // write config so the secret provider can pick it up
            Files.writeString(kubeConfigFile, kubeContainer.getKubeconfig());
        }
        if (ldapServer == null) {
            ldapServer = new GenericContainer<>("ghcr.io/rroemhild/docker-test-openldap:master")
                .withExposedPorts(LDAP_PORT)
                .waitingFor(new LogMessageWaitStrategy().withRegEx(".*slapd starting.*"));
            ldapServer.start();
        }
    }

    @Override
    public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
        try {
            kubeConfigFile = Files.createTempDirectory(KubernetesHttpProxyHeaderSecretTest.class.getSimpleName()).resolve(
                "kube_config.yml"
            );

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
        resources.putIfAbsent("oauth2", ResourceBuilder.build("oauth2", OAuth2GenericResource.class, OAuth2ResourceConfiguration.class));
        resources.putIfAbsent(
            "auth-provider-ldap-resource",
            ResourceBuilder.build(
                "auth-provider-ldap-resource",
                LdapAuthenticationProviderResource.class,
                LdapAuthenticationProviderResourceConfiguration.class
            )
        );
    }

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.putIfAbsent("cache", PolicyBuilder.build("cache", CachePolicy.class, CachePolicyConfiguration.class));
        policies.putIfAbsent("oauth2", PolicyBuilder.build("oauth2", Oauth2PolicyV3.class, OAuth2PolicyConfiguration.class));
        policies.putIfAbsent(
            "policy-basic-authentication",
            PolicyBuilder.build(
                "policy-basic-authentication",
                BasicAuthenticationPolicy.class,
                BasicAuthenticationPolicyConfiguration.class
            )
        );
    }

    @Override
    public void configureServices(Set<Class<? extends AbstractService<?>>> services) {
        services.add(SecretsService.class);
    }

    void createSecrets() throws IOException, InterruptedException {
        KubernetesHelper.createSecret(kubeContainer, "default", "redis", Map.of("password", REDIS_PASSWORD));
        KubernetesHelper.createSecret(kubeContainer, "default", "oauth", Map.of("clientSecret", CLIENT_SECRET));
        KubernetesHelper.createSecret(
            kubeContainer,
            "default",
            "ldap",
            Map.of("username", "cn=admin,dc=planetexpress,dc=com", "password", LDAP_PASSWORD)
        );
    }

    @Override
    public void configurePlaceHolderVariables(Map<String, String> variables) {
        variables.put("REDIS_PORT", String.valueOf(redisContainer.getRedisPort()));
        variables.put("LDAP_PORT", String.valueOf(ldapServer.getMappedPort(LDAP_PORT)));
    }

    @Test
    @DeployApi("/apis/v4/http/secrets/resources/api-with-redis.json")
    void should_cache_the_response_in_redis(HttpClient httpClient) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

        callApi(httpClient);
        callApi(httpClient);

        // response has been cached, hence should only be called once
        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));
    }

    @Test
    @DeployApi("/apis/v4/http/secrets/resources/api-with-gen-oauth2.json")
    void should_call_oauth2_introspect__with_client_secret_and_then_the_backend(HttpClient httpClient) {
        String jwt = new PlainJWT(
            new JWTClaimsSet.Builder()
                .subject("test")
                .issuer("ITTest")
                .audience("OAuth")
                .expirationTime(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .issueTime(new Date())
                .claim("test", "hello")
                .build()
        ).serialize();

        // stub for introspect endpoint (called by oauth policy via oauth2 resource)
        wiremock.stubFor(
            get("/oauth/check_token")
                .withBasicAuth("admin", CLIENT_SECRET)
                .withHeader("token", equalTo(jwt))
                .willReturn(
                    jsonResponse(
                        """
                        {
                            "client_id": "admin",
                            "sub": "the_user",
                            "scope": []
                        }
                        """,
                        200
                    )
                )
        );

        // the backend
        wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));

        callApi(httpClient, Map.of("Authorization", "Bearer " + jwt));

        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/oauth/check_token")));
        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));
    }

    @Test
    @DeployApi("/apis/v4/http/secrets/resources/api-with-ldap.json")
    void should_call_and_authenticate_user_using_ldap(HttpClient httpClient) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend")));
        callApi(httpClient, Map.of("Authorization", "Basic " + Base64.getEncoder().encodeToString("fry:fry".getBytes())));
        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")));
    }

    private static void callApi(HttpClient httpClient) {
        callApi(httpClient, Map.of());
    }

    private static void callApi(HttpClient httpClient, Map<String, String> map) {
        httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .doOnSuccess(req -> map.forEach(req::putHeader))
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
