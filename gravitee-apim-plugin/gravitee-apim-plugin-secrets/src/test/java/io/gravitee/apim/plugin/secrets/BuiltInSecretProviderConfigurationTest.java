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
import static org.mockito.Mockito.mock;

import io.gravitee.node.secrets.plugins.SecretProviderPluginManager;
import io.gravitee.node.secrets.plugins.internal.DefaultSecretProviderClassLoaderFactory;
import io.gravitee.node.secrets.plugins.internal.DefaultSecretProviderPluginManager;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationKeyRepository;
import io.gravitee.repository.management.api.SecretRepository;
import io.gravitee.secrets.api.core.SecretURL;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

class BuiltInSecretProviderConfigurationTest {

    private final SecretProviderPluginManager pluginManager = new DefaultSecretProviderPluginManager(
        new DefaultSecretProviderClassLoaderFactory()
    );

    @Test
    void should_register_the_plugin_before_the_management_repository_is_loaded() {
        try (var context = gatewayContext()) {
            context.refresh();

            assertThat((Object) pluginManager.getFactoryById(BuiltInSecretProvider.ID)).isInstanceOf(BuiltInSecretProviderFactory.class);
        }
    }

    @Test
    void should_connect_the_plugin_to_the_repositories_the_management_repository_registers_later() {
        try (var context = gatewayContext()) {
            context.refresh();
            context.getBeanFactory().registerSingleton("secretRepository", mock(SecretRepository.class));
            context.getBeanFactory().registerSingleton("organizationKeyRepository", mock(OrganizationKeyRepository.class));
            context.getBeanFactory().registerSingleton("environmentRepository", mock(EnvironmentRepository.class));

            BuiltInSecretProviderFactory factory = pluginManager.getFactoryById(BuiltInSecretProvider.ID);
            assertThat(factory.create(new BuiltInSecretManagerConfiguration(Map.of("encryption.key", "a-key")))).isInstanceOf(
                BuiltInSecretProvider.class
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

            BuiltInSecretProviderFactory factory = pluginManager.getFactoryById(BuiltInSecretProvider.ID);
            factory
                .create(new BuiltInSecretManagerConfiguration(Map.of()))
                .start()
                .resolve(SecretURL.from("/gravitee/env-1/llm-key:value", true))
                .test()
                .assertError(err -> err.getMessage().contains("is not valid base64"));
        }
    }

    private AnnotationConfigApplicationContext gatewayContext() {
        var context = new AnnotationConfigApplicationContext();
        context.registerBean(SecretProviderPluginManager.class, () -> pluginManager);
        context.register(BuiltInSecretProviderConfiguration.class);
        return context;
    }
}
