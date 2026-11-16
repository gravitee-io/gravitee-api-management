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

import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationKeyRepository;
import io.gravitee.repository.management.api.SecretRepository;
import io.gravitee.secrets.api.plugin.EnvironmentAwareSecretProvider;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class BuiltInSecretProviderConfigurationTest {

    @Test
    void should_declare_the_built_in_provider_once_the_repository_registers_the_secret_repositories() {
        try (var context = new AnnotationConfigApplicationContext(BuiltInSecretProviderConfiguration.class)) {
            context.registerBean(SecretRepository.class, () -> mock(SecretRepository.class));
            context.registerBean(OrganizationKeyRepository.class, () -> mock(OrganizationKeyRepository.class));
            context.registerBean(EnvironmentRepository.class, () -> mock(EnvironmentRepository.class));

            assertThat(context.getBeansOfType(EnvironmentAwareSecretProvider.class).values())
                .singleElement()
                .isInstanceOf(BuiltInSecretProvider.class);
        }
    }

    @Test
    void should_declare_no_provider_when_the_repository_does_not_store_secrets() {
        try (var context = new AnnotationConfigApplicationContext(BuiltInSecretProviderConfiguration.class)) {
            context.registerBean(EnvironmentRepository.class, () -> mock(EnvironmentRepository.class));

            assertThat(context.getBeansOfType(EnvironmentAwareSecretProvider.class)).isEmpty();
        }
    }
}
