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
import static org.mockito.Mockito.mock;

import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationKeyRepository;
import io.gravitee.repository.management.api.SecretRepository;
import io.gravitee.secrets.api.core.SecretURL;
import io.gravitee.secrets.api.errors.SecretManagerException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;

class GraviteeSecretProviderFactoryTest {

    private static final SecretURL LLM_KEY = SecretURL.from("/gravitee/s-1:value?envId=env-1", true);

    private final GraviteeSecretProviderFactory factory = new GraviteeSecretProviderFactory();
    private final StaticApplicationContext gateway = new StaticApplicationContext();
    private String sharedKey;
    private Optional<String> misconfiguration = Optional.empty();

    @Test
    void should_refuse_to_create_a_provider_before_it_is_connected_to_the_gateway() {
        assertThatThrownBy(() -> factory.create(configuration("a-key")))
            .isInstanceOf(SecretManagerException.class)
            .hasMessageContaining("not connected");
    }

    @Test
    void should_create_the_gravitee_provider_with_the_repositories_the_gateway_has_by_then() {
        connect();
        givenSecretRepositories();

        assertThat(factory.create(configuration("a-key"))).isInstanceOf(GraviteeSecretProvider.class);
    }

    @Test
    void should_create_a_provider_that_refuses_every_secret_when_the_repository_does_not_store_secrets() {
        connect();
        gateway.getBeanFactory().registerSingleton("environmentRepository", mock(EnvironmentRepository.class));

        var provider = factory.create(configuration("a-key")).start();

        provider
            .resolve(LLM_KEY)
            .test()
            .assertError(err -> err instanceof SecretManagerException && err.getMessage().contains("does not store secrets"));
    }

    @Test
    void should_create_a_provider_that_refuses_every_secret_when_a_gravitee_entry_is_misconfigured() {
        misconfiguration = Optional.of("Secret provider entry api.secrets.providers[0] must have the id gravitee");
        connect();
        givenSecretRepositories();
        sharedKey = randomKek();

        var provider = factory.create(new GraviteeSecretManagerConfiguration(Map.of())).start();

        provider
            .resolve(LLM_KEY)
            .test()
            .assertError(err -> err instanceof SecretManagerException && err.getMessage().contains("api.secrets.providers[0]"));
    }

    @Test
    void should_create_a_provider_that_uses_the_configured_key_encryption_key() {
        connect();
        givenSecretRepositories();

        var provider = factory.create(configuration("not-a-key")).start();

        provider
            .resolve(LLM_KEY)
            .test()
            .assertError(err -> err instanceof SecretManagerException && err.getMessage().contains("is not valid base64"));
    }

    @Test
    void should_prefer_the_key_of_its_provider_entry_to_the_shared_key() {
        connect();
        givenSecretRepositories();
        sharedKey = randomKek();

        var provider = factory.create(configuration("not-a-key")).start();

        provider
            .resolve(LLM_KEY)
            .test()
            .assertError(err -> err.getMessage().contains("is not valid base64"));
    }

    @Test
    void should_fall_back_to_the_shared_key_when_its_provider_entry_has_none() {
        connect();
        givenSecretRepositories();
        sharedKey = randomKek();

        var provider = factory.create(new GraviteeSecretManagerConfiguration(Map.of())).start();

        provider.resolve(LLM_KEY).test().assertComplete().assertNoValues();
    }

    @Test
    void should_fall_back_to_the_shared_key_when_the_key_of_its_provider_entry_is_blank() {
        connect();
        givenSecretRepositories();
        sharedKey = "not-a-key";

        var provider = factory.create(configuration(" ")).start();

        provider
            .resolve(LLM_KEY)
            .test()
            .assertError(err -> err.getMessage().contains("is not valid base64"));
    }

    private void connect() {
        factory.connect(
            gateway.getBeanProvider(SecretRepository.class),
            gateway.getBeanProvider(OrganizationKeyRepository.class),
            gateway.getBeanProvider(EnvironmentRepository.class),
            () -> sharedKey,
            misconfiguration
        );
    }

    private static String randomKek() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    private void givenSecretRepositories() {
        gateway.getBeanFactory().registerSingleton("secretRepository", mock(SecretRepository.class));
        gateway.getBeanFactory().registerSingleton("organizationKeyRepository", mock(OrganizationKeyRepository.class));
        gateway.getBeanFactory().registerSingleton("environmentRepository", mock(EnvironmentRepository.class));
    }

    private static GraviteeSecretManagerConfiguration configuration(String key) {
        return new GraviteeSecretManagerConfiguration(Map.of("encryption.key", key));
    }
}
