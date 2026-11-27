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
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.graviteesource.service.secrets.SecretsService;
import io.gravitee.apim.common.secret.DataKey;
import io.gravitee.apim.common.secret.SecretChangedEvent;
import io.gravitee.apim.common.secret.SecretEnvelope;
import io.gravitee.apim.common.secret.SecretScope;
import io.gravitee.apim.common.secret.StaticKeyEncryptionKeyProvider;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.converters.V4ApiDeploymentPreparer;
import io.gravitee.common.service.AbstractService;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.dictionary.DictionaryProperty;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.gateway.dictionary.model.Dictionary;
import io.gravitee.gateway.reactor.ReactableApi;
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
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

@GatewayTest
class GraviteeSecretRotationV4IntegrationTest extends AbstractGatewayTest {

    private static final String ORGANIZATION = "DEFAULT";
    private static final String ENVIRONMENT = "DEFAULT";
    private static final String UPDATE_KEY = "rotation-update-key";
    private static final String REDEPLOY_KEY = "rotation-redeploy-key";
    private static final String REDEPLOY_API = "/apis/v4/http/secrets/gravitee/api-rotation-redeploy.json";
    private static final String DELETE_KEY = "rotation-delete-key";
    private static final String BEFORE_ROTATION = "sk-before-rotation";
    private static final String AFTER_ROTATION = "sk-after-rotation";

    private final String base64Kek = randomKek();
    private final StaticKeyEncryptionKeyProvider kek = StaticKeyEncryptionKeyProvider.fromBase64(base64Kek);
    private final DataKey dataKey = DataKey.generate();
    private final SecretRepository secrets = mock(SecretRepository.class);
    private final OrganizationKeyRepository organizationKeys = mock(OrganizationKeyRepository.class);
    private final Map<String, String> storedValues = new ConcurrentHashMap<>(
        Map.of(UPDATE_KEY, BEFORE_ROTATION, REDEPLOY_KEY, BEFORE_ROTATION, DELETE_KEY, BEFORE_ROTATION)
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
    public void configureDictionaries(List<Dictionary> dictionaries) {
        Dictionary rotation = new Dictionary();
        rotation.setId("rotation");
        rotation.setKey("rotation");
        rotation.setEnvironmentId(ENVIRONMENT);
        rotation.setProperties(
            Map.of("update", new DictionaryProperty("/gravitee/" + UPDATE_KEY + ":value?envId=" + ENVIRONMENT + "&renewable=true", false))
        );
        dictionaries.add(rotation);
    }

    @Override
    public void configureBootContext(ConfigurableApplicationContext context) {
        context.getBeanFactory().registerSingleton("secretRepository", secrets);
        context.getBeanFactory().registerSingleton("organizationKeyRepository", organizationKeys);
    }

    // The SDK deploys a test's API before any @BeforeEach runs, then resets the Mockito beans of the gateway context.
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        super.setApplicationContext(applicationContext);
        stubRepositories();
    }

    @BeforeEach
    void restoreTheStubsTheSdkReset() {
        stubRepositories();
    }

    private void stubRepositories() {
        try {
            lenient()
                .when(secrets.findByKey(eq(ORGANIZATION), eq(ENVIRONMENT), anyString()))
                .thenAnswer(invocation -> storedSecret(invocation.getArgument(2)));
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
            lenient()
                .when(applicationContext.getBean(EnvironmentRepository.class).findById(ENVIRONMENT))
                .thenReturn(Optional.of(Environment.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build()));
        } catch (TechnicalException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DeployApi("/apis/v4/http/secrets/gravitee/api-rotation-update.json")
    void should_send_the_rotated_value_to_every_reference_of_a_secret_that_one_reference_reloads_on_change(HttpClient httpClient) {
        wiremock.stubFor(get("/endpoint").willReturn(ok()));
        assertThat(upstreamHeaders(httpClient, "/rotation-update")).containsAllEntriesOf(
            Map.of("X-Plain", BEFORE_ROTATION, "X-Reload", BEFORE_ROTATION, "X-Dictionary", BEFORE_ROTATION, "X-Property", BEFORE_ROTATION)
        );

        storedValues.put(UPDATE_KEY, AFTER_ROTATION);
        deployGenericEvent(secretChanged(UPDATE_KEY));

        await()
            .atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() ->
                assertThat(upstreamHeaders(httpClient, "/rotation-update")).containsAllEntriesOf(
                    Map.of(
                        "X-Plain",
                        AFTER_ROTATION,
                        "X-Reload",
                        AFTER_ROTATION,
                        "X-Dictionary",
                        AFTER_ROTATION,
                        "X-Property",
                        AFTER_ROTATION
                    )
                )
            );
    }

    @Test
    @DeployApi({ REDEPLOY_API, "/apis/v4/http/secrets/gravitee/api-rotation-redeploy-witness.json" })
    void should_keep_the_old_value_in_an_api_with_no_reference_that_reloads_on_change_until_the_api_is_redeployed(HttpClient httpClient)
        throws IOException {
        wiremock.stubFor(get("/endpoint").willReturn(ok()));
        storedValues.put(REDEPLOY_KEY, AFTER_ROTATION);
        deployGenericEvent(secretChanged(REDEPLOY_KEY));

        // The witness API reloads on change, so its new value shows the gateway has handled the event.
        await()
            .atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() ->
                assertThat(upstreamHeaders(httpClient, "/rotation-redeploy-witness")).containsEntry("X-Reload", AFTER_ROTATION)
            );
        assertThat(upstreamHeaders(httpClient, "/rotation-redeploy")).containsEntry("X-Plain", BEFORE_ROTATION);

        // The first deployment rewrote the definition's secret references into grants that the undeployment revokes.
        redeploy(definitionOf(REDEPLOY_API));

        assertThat(upstreamHeaders(httpClient, "/rotation-redeploy")).containsEntry("X-Plain", AFTER_ROTATION);
    }

    @Test
    @DeployApi("/apis/v4/http/secrets/gravitee/api-rotation-delete.json")
    void should_answer_503_once_a_secret_that_reloads_on_change_is_deleted(HttpClient httpClient) {
        wiremock.stubFor(get("/endpoint").willReturn(ok()));
        assertThat(status(httpClient, "/rotation-delete")).isEqualTo(200);

        storedValues.remove(DELETE_KEY);
        deployGenericEvent(secretChanged(DELETE_KEY));

        await()
            .atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> assertThat(status(httpClient, "/rotation-delete")).isEqualTo(503));
    }

    private Map<String, String> upstreamHeaders(HttpClient httpClient, String path) {
        wiremock.resetRequests();
        assertThat(status(httpClient, path)).isEqualTo(200);
        return wiremock
            .getAllServeEvents()
            .getFirst()
            .getRequest()
            .getHeaders()
            .all()
            .stream()
            .filter(header -> header.key().startsWith("X-"))
            .collect(Collectors.toMap(HttpHeader::key, HttpHeader::firstValue));
    }

    private static int status(HttpClient httpClient, String path) {
        return httpClient
            .rxRequest(HttpMethod.GET, path)
            .flatMap(request -> request.rxSend().flatMap(response -> response.body().map(body -> response.statusCode())))
            .timeout(10, TimeUnit.SECONDS)
            .blockingGet();
    }

    private ReactableApi<Api> definitionOf(String resource) throws IOException {
        try (InputStream json = GraviteeSecretRotationV4IntegrationTest.class.getResourceAsStream(resource)) {
            Api definition = new GraviteeMapper().readValue(json, Api.class);
            updateEndpointsPort(definition, wiremock.port());
            return new V4ApiDeploymentPreparer().toReactable(definition, ENVIRONMENT);
        }
    }

    private Optional<Secret> storedSecret(String key) {
        String value = storedValues.get(key);
        if (value == null) {
            return Optional.empty();
        }
        var secret = Secret.builder().id(key + "-id").organizationId(ORGANIZATION).environmentId(ENVIRONMENT).key(key).build();
        secret.setEncryptedValue(SecretEnvelope.seal(dataKey, new SecretScope(ORGANIZATION, ENVIRONMENT, secret.getId()), value));
        return Optional.of(secret);
    }

    private static GenericEvent secretChanged(String key) {
        return new GenericEvent(SecretChangedEvent.KIND, key, ENVIRONMENT, ORGANIZATION, null, Instant.now(), "{}");
    }

    private static String randomKek() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
