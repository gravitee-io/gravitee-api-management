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
package io.gravitee.gateway.standalone.secrets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.gravitee.apim.common.secret.StaticKeyEncryptionKeyProvider;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationKeyRepository;
import io.gravitee.repository.management.api.SecretRepository;
import io.gravitee.secrets.api.plugin.EnvironmentAwareSecretProvider;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

class BuiltInSecretProviderConfigurationTest {

    @Test
    void should_declare_the_built_in_provider_once_the_repository_registers_the_secret_repositories() {
        try (var context = new AnnotationConfigApplicationContext(BuiltInSecretProviderConfiguration.class)) {
            givenKey(context, "a-key");
            givenSecretRepositories(context);

            assertThat(context.getBeansOfType(EnvironmentAwareSecretProvider.class).values())
                .singleElement()
                .isInstanceOf(BuiltInSecretProvider.class);
        }
    }

    @Test
    void should_declare_no_provider_when_the_repository_does_not_store_secrets() {
        try (var context = new AnnotationConfigApplicationContext(BuiltInSecretProviderConfiguration.class)) {
            givenKey(context, "a-key");
            context.registerBean(EnvironmentRepository.class, () -> mock(EnvironmentRepository.class));

            assertThat(context.getBeansOfType(EnvironmentAwareSecretProvider.class)).isEmpty();
        }
    }

    @Test
    void should_declare_no_provider_when_the_key_encryption_key_is_not_set() {
        try (var context = new AnnotationConfigApplicationContext(BuiltInSecretProviderConfiguration.class)) {
            givenSecretRepositories(context);

            assertThat(context.getBeansOfType(EnvironmentAwareSecretProvider.class)).isEmpty();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "  " })
    void should_declare_no_provider_when_the_key_encryption_key_is_blank(String key) {
        try (var context = new AnnotationConfigApplicationContext(BuiltInSecretProviderConfiguration.class)) {
            givenKey(context, key);
            givenSecretRepositories(context);

            assertThat(context.getBeansOfType(EnvironmentAwareSecretProvider.class)).isEmpty();
        }
    }

    private static void givenKey(AnnotationConfigApplicationContext context, String key) {
        context
            .getEnvironment()
            .getPropertySources()
            .addFirst(new MapPropertySource("test", Map.of(StaticKeyEncryptionKeyProvider.KEY_PROPERTY, key)));
    }

    private static void givenSecretRepositories(AnnotationConfigApplicationContext context) {
        context.registerBean(SecretRepository.class, () -> mock(SecretRepository.class));
        context.registerBean(OrganizationKeyRepository.class, () -> mock(OrganizationKeyRepository.class));
        context.registerBean(EnvironmentRepository.class, () -> mock(EnvironmentRepository.class));
    }
}
