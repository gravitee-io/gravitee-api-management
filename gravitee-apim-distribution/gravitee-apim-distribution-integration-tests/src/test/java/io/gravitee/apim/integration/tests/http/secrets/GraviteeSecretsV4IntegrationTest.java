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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.graviteesource.service.secrets.SecretsService;
import io.gravitee.apim.common.secret.DataKey;
import io.gravitee.apim.common.secret.SecretEnvelope;
import io.gravitee.apim.common.secret.SecretScope;
import io.gravitee.apim.common.secret.StaticKeyEncryptionKeyProvider;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.common.service.AbstractService;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationKeyRepository;
import io.gravitee.repository.management.api.SecretRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.OrganizationKey;
import io.gravitee.repository.management.model.Secret;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

@GatewayTest
@DeployApi(
    {
        "/apis/v4/http/secrets/gravitee/api-static-ref.json",
        "/apis/v4/http/secrets/gravitee/api-static-ref-no-environment.json",
        "/apis/v4/http/secrets/gravitee/api-static-ref-other-environment-key.json",
        "/apis/v4/http/secrets/gravitee/api-static-ref-other-environment.json",
    }
)
class GraviteeSecretsV4IntegrationTest extends AbstractGatewayTest {

    private static final String ORGANIZATION = "DEFAULT";
    private static final String ENVIRONMENT = "DEFAULT";
    private static final String OTHER_ENVIRONMENT = "other-environment";
    private static final String KEY = "llm-key";
    private static final String VALUE = "sk-gravitee-secret";
    private static final String OTHER_VALUE = "sk-other-environment-secret";

    private final String base64Kek = randomKek();
    private final StaticKeyEncryptionKeyProvider kek = StaticKeyEncryptionKeyProvider.fromBase64(base64Kek);
    private final DataKey dataKey = DataKey.generate();
    private final SecretRepository secrets = mock(SecretRepository.class);

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
        configurationBuilder.setYamlProperty("api.secrets.providers[0].id", "gravitee");
        configurationBuilder.setYamlProperty("api.secrets.providers[0].plugin", "gravitee");
        configurationBuilder.setYamlProperty("api.secrets.providers[0].configuration.encryption.key", base64Kek);
    }

    @Override
    public void configureServices(Set<Class<? extends AbstractService<?>>> services) {
        super.configureServices(services);
        services.add(SecretsService.class);
    }

    @Override
    public void configureBootContext(ConfigurableApplicationContext context) {
        try {
            context.getBeanFactory().registerSingleton("secretRepository", secretRepository());
            context.getBeanFactory().registerSingleton("organizationKeyRepository", organizationKeyRepository());
        } catch (TechnicalException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        super.setApplicationContext(applicationContext);
        stubEnvironmentsOnceForTheClass(applicationContext.getBean(EnvironmentRepository.class));
    }

    private static void stubEnvironmentsOnceForTheClass(EnvironmentRepository gatewayContextMock) {
        try {
            for (String environmentId : Set.of(ENVIRONMENT, OTHER_ENVIRONMENT)) {
                lenient()
                    .when(gatewayContextMock.findById(environmentId))
                    .thenReturn(Optional.of(Environment.builder().id(environmentId).organizationId(ORGANIZATION).build()));
            }
        } catch (TechnicalException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void should_call_the_upstream_with_the_value_of_the_gravitee_secret(HttpClient httpClient) {
        wiremock.stubFor(get("/endpoint").withHeader("Authorization", equalTo("ApiKey " + VALUE)).willReturn(ok("from backend")));

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

        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/endpoint")).withHeader("Authorization", equalTo("ApiKey " + VALUE)));
    }

    @ParameterizedTest
    @ValueSource(strings = { "/no-environment", "/other-environment", "/other-environment-key" })
    void should_never_hand_another_environment_s_secret_to_the_upstream(String path, HttpClient httpClient) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("from backend")));

        httpClient
            .rxRequest(HttpMethod.GET, path)
            .flatMap(HttpClientRequest::rxSend)
            .flatMap(response -> {
                assertThat(response.statusCode()).isEqualTo(503);
                return response.body();
            })
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete();

        wiremock.verify(0, getRequestedFor(urlPathEqualTo("/endpoint")).withHeader("Authorization", equalTo("ApiKey " + OTHER_VALUE)));
    }

    private SecretRepository secretRepository() throws TechnicalException {
        var secret = Secret.builder().id("secret-1").organizationId(ORGANIZATION).environmentId(ENVIRONMENT).key(KEY).build();
        secret.setEncryptedValue(SecretEnvelope.seal(dataKey, new SecretScope(ORGANIZATION, ENVIRONMENT, secret.getId()), VALUE));
        lenient().when(secrets.findByKey(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        lenient().when(secrets.findByKey(ORGANIZATION, ENVIRONMENT, secret.getKey())).thenReturn(Optional.of(secret));
        var other = Secret.builder()
            .id("secret-2")
            .organizationId(ORGANIZATION)
            .environmentId(OTHER_ENVIRONMENT)
            .key("other-llm-key")
            .build();
        other.setEncryptedValue(SecretEnvelope.seal(dataKey, new SecretScope(ORGANIZATION, OTHER_ENVIRONMENT, other.getId()), OTHER_VALUE));
        lenient().when(secrets.findByKey(ORGANIZATION, OTHER_ENVIRONMENT, other.getKey())).thenReturn(Optional.of(other));
        return secrets;
    }

    private OrganizationKeyRepository organizationKeyRepository() throws TechnicalException {
        var organizationKeys = mock(OrganizationKeyRepository.class);
        lenient().when(organizationKeys.findKekIds()).thenReturn(Set.of(kek.id()));
        lenient()
            .when(organizationKeys.findByOrganizationId(ORGANIZATION))
            .thenReturn(
                Optional.of(
                    OrganizationKey.builder()
                        .id(dataKey.id())
                        .organizationId(ORGANIZATION)
                        .kekId(kek.id())
                        .wrappedKey(Base64.getEncoder().encodeToString(kek.wrap(ORGANIZATION, dataKey.material())))
                        .build()
                )
            );
        return organizationKeys;
    }

    private static String randomKek() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
