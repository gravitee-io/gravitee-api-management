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
package io.gravitee.apim.plugin.secrets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.api.genericevent.GenericEventRegistry;
import io.gravitee.node.secrets.plugins.SecretProviderPluginManager;
import io.gravitee.node.secrets.plugins.internal.DefaultSecretProviderClassLoaderFactory;
import io.gravitee.node.secrets.plugins.internal.DefaultSecretProviderPluginManager;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationKeyRepository;
import io.gravitee.repository.management.api.SecretRepository;
import io.gravitee.repository.management.api.SecretValueRepository;
import io.gravitee.secrets.api.core.SecretURL;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

class GraviteeSecretProviderConfigurationTest {

    private final GenericEventRegistry genericEventRegistry = mock(GenericEventRegistry.class);
    private final SecretProviderPluginManager pluginManager = new DefaultSecretProviderPluginManager(
        new DefaultSecretProviderClassLoaderFactory()
    );

    @Test
    void should_register_the_plugin_before_the_management_repository_is_loaded() {
        try (var context = gatewayContext()) {
            context.refresh();

            assertThat((Object) pluginManager.getFactoryById(GraviteeSecretProvider.ID)).isInstanceOf(GraviteeSecretProviderFactory.class);
        }
    }

    @Test
    void should_connect_the_plugin_to_the_repositories_the_management_repository_registers_later() {
        try (var context = gatewayContext()) {
            context.refresh();
            context.getBeanFactory().registerSingleton("secretRepository", mock(SecretRepository.class));
            context.getBeanFactory().registerSingleton("organizationKeyRepository", mock(OrganizationKeyRepository.class));
            context.getBeanFactory().registerSingleton("environmentRepository", mock(EnvironmentRepository.class));

            GraviteeSecretProviderFactory factory = pluginManager.getFactoryById(GraviteeSecretProvider.ID);
            assertThat(factory.create(new GraviteeSecretManagerConfiguration(Map.of("encryption.key", "a-key")))).isInstanceOf(
                GraviteeSecretProvider.class
            );
        }
    }

    @Test
    void should_hand_the_shared_key_of_the_gateway_to_providers_whose_entry_has_none() {
        try (var context = gatewayContext()) {
            context
                .getEnvironment()
                .getPropertySources()
                .addFirst(new MapPropertySource("gateway", Map.of("api.secrets.encryption.key", "not-a-key")));
            context.refresh();
            context.getBeanFactory().registerSingleton("secretRepository", mock(SecretRepository.class));
            context.getBeanFactory().registerSingleton("organizationKeyRepository", mock(OrganizationKeyRepository.class));
            context.getBeanFactory().registerSingleton("environmentRepository", mock(EnvironmentRepository.class));

            GraviteeSecretProviderFactory factory = pluginManager.getFactoryById(GraviteeSecretProvider.ID);
            factory
                .create(new GraviteeSecretManagerConfiguration(Map.of()))
                .start()
                .resolve(SecretURL.from("/gravitee/s-1:value?envId=env-1", true))
                .test()
                .assertError(err -> err.getMessage().contains("is not valid base64"));
        }
    }

    @Test
    void should_hand_providers_the_misconfiguration_of_the_gravitee_entries() {
        try (var context = gatewayContext()) {
            context
                .getEnvironment()
                .getPropertySources()
                .addFirst(
                    new MapPropertySource(
                        "gateway",
                        Map.of("api.secrets.providers[0].plugin", "gravitee", "api.secrets.providers[0].id", "internal")
                    )
                );
            context.refresh();
            context.getBeanFactory().registerSingleton("secretRepository", mock(SecretRepository.class));
            context.getBeanFactory().registerSingleton("organizationKeyRepository", mock(OrganizationKeyRepository.class));
            context.getBeanFactory().registerSingleton("environmentRepository", mock(EnvironmentRepository.class));

            GraviteeSecretProviderFactory factory = pluginManager.getFactoryById(GraviteeSecretProvider.ID);
            factory
                .create(new GraviteeSecretManagerConfiguration(Map.of()))
                .start()
                .resolve(SecretURL.from("/gravitee/s-1:value?envId=env-1", true))
                .test()
                .assertError(err -> err.getMessage().contains("api.secrets.providers[0]"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "management.type", "repositories.management.type" })
    void should_refuse_every_secret_on_a_gateway_without_a_management_repository(String property) {
        try (var context = gatewayContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("gateway", Map.of(property, "none")));
            context.refresh();
            context.getBeanFactory().registerSingleton("secretRepository", mock(SecretRepository.class));
            context.getBeanFactory().registerSingleton("organizationKeyRepository", mock(OrganizationKeyRepository.class));
            context.getBeanFactory().registerSingleton("environmentRepository", mock(EnvironmentRepository.class));

            GraviteeSecretProviderFactory factory = pluginManager.getFactoryById(GraviteeSecretProvider.ID);
            factory
                .create(new GraviteeSecretManagerConfiguration(Map.of("encryption.key", "a-key")))
                .start()
                .resolve(SecretURL.from("/gravitee/s-1:value?envId=env-1", true))
                .test()
                .assertError(err -> err.getMessage().contains("does not store secrets"));
        }
    }

    @Test
    void should_read_secrets_through_the_bridge_client_on_a_hybrid_gateway() throws TechnicalException {
        try (var context = gatewayContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("gateway", Map.of("management.type", "http")));
            context.refresh();
            var bridge = mock(SecretValueRepository.class);
            when(bridge.findValue("env-1", "s-1")).thenReturn(Optional.of("sk-live-123"));
            context.getBeanFactory().registerSingleton("httpSecretValueRepository", bridge);

            GraviteeSecretProviderFactory factory = pluginManager.getFactoryById(GraviteeSecretProvider.ID);
            var secrets = factory
                .create(new GraviteeSecretManagerConfiguration(Map.of()))
                .start()
                .resolve(SecretURL.from("/gravitee/s-1:value?envId=env-1", true))
                .blockingGet();

            assertThat(secrets.asMap().get("value").asString()).isEqualTo("sk-live-123");
        }
    }

    @Test
    void should_give_a_bridge_server_on_a_gateway_with_a_management_repository_the_secret_values_it_decrypts() {
        try (var context = gatewayContext()) {
            context.refresh();
            context.getBeanFactory().registerSingleton("secretRepository", mock(SecretRepository.class));
            context.getBeanFactory().registerSingleton("organizationKeyRepository", mock(OrganizationKeyRepository.class));
            context.getBeanFactory().registerSingleton("environmentRepository", mock(EnvironmentRepository.class));

            assertThat(context.getBean(SecretValueRepository.class)).isInstanceOf(DecryptingSecretValueRepository.class);
        }
    }

    @Test
    void should_give_a_bridge_server_the_key_of_the_gravitee_entry_when_the_gateway_has_no_shared_key() {
        try (var context = gatewayContext()) {
            context
                .getEnvironment()
                .getPropertySources()
                .addFirst(
                    new MapPropertySource(
                        "gateway",
                        Map.of(
                            "api.secrets.providers[0].plugin",
                            "gravitee",
                            "api.secrets.providers[0].configuration.encryption.key",
                            "not-a-key"
                        )
                    )
                );
            context.refresh();
            context.getBeanFactory().registerSingleton("secretRepository", mock(SecretRepository.class));
            context.getBeanFactory().registerSingleton("organizationKeyRepository", mock(OrganizationKeyRepository.class));
            context.getBeanFactory().registerSingleton("environmentRepository", mock(EnvironmentRepository.class));

            assertThatThrownBy(() -> context.getBean(SecretValueRepository.class).findValue("env-1", "s-1")).hasMessageContaining(
                "is not valid base64"
            );
        }
    }

    @Test
    void should_give_a_bridge_server_the_key_of_the_gravitee_entry_over_the_shared_key() {
        try (var context = gatewayContext()) {
            context
                .getEnvironment()
                .getPropertySources()
                .addFirst(
                    new MapPropertySource(
                        "gateway",
                        Map.of(
                            "api.secrets.encryption.key",
                            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                            "api.secrets.providers[0].plugin",
                            "gravitee",
                            "api.secrets.providers[0].configuration.encryption.key",
                            "not-a-key"
                        )
                    )
                );
            context.refresh();
            context.getBeanFactory().registerSingleton("secretRepository", mock(SecretRepository.class));
            context.getBeanFactory().registerSingleton("organizationKeyRepository", mock(OrganizationKeyRepository.class));
            context.getBeanFactory().registerSingleton("environmentRepository", mock(EnvironmentRepository.class));

            assertThatThrownBy(() -> context.getBean(SecretValueRepository.class).findValue("env-1", "s-1")).hasMessageContaining(
                "is not valid base64"
            );
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "http", "none" })
    void should_give_no_secret_values_of_its_own_on_a_gateway_without_a_management_database(String type) {
        try (var context = gatewayContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("gateway", Map.of("management.type", type)));
            context.refresh();

            assertThat(context.getBeanProvider(SecretValueRepository.class).getIfAvailable()).isNull();
        }
    }

    @Test
    void should_subscribe_to_the_changes_of_the_secret_store() {
        try (var context = gatewayContext()) {
            context.refresh();

            verify(genericEventRegistry).subscribe(eq("secret.changed"), any(GraviteeSecretChangeSubscriber.class));
        }
    }

    private AnnotationConfigApplicationContext gatewayContext() {
        var context = new AnnotationConfigApplicationContext();
        context.registerBean(SecretProviderPluginManager.class, () -> pluginManager);
        context.registerBean(GenericEventRegistry.class, () -> genericEventRegistry);
        context.registerBean(EventManager.class, () -> mock(EventManager.class));
        context.register(GraviteeSecretProviderConfiguration.class);
        return context;
    }
}
