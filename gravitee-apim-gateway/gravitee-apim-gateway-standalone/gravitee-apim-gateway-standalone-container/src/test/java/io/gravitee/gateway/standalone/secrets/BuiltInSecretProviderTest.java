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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.gravitee.apim.common.secret.DataKey;
import io.gravitee.apim.common.secret.SecretEnvelope;
import io.gravitee.apim.common.secret.SecretScope;
import io.gravitee.apim.common.secret.StaticKeyEncryptionKeyProvider;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationKeyRepository;
import io.gravitee.repository.management.api.SecretRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.OrganizationKey;
import io.gravitee.repository.management.model.Secret;
import io.gravitee.secrets.api.core.SecretURL;
import io.gravitee.secrets.api.errors.SecretManagerException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class BuiltInSecretProviderTest {

    private static final String ORG = "org-1";
    private static final String ENV = "env-1";
    private static final SecretURL LLM_KEY = SecretURL.from("/gravitee/llm-key:value", true);

    private final String base64Kek = randomKek();
    private final StaticKeyEncryptionKeyProvider kek = StaticKeyEncryptionKeyProvider.fromBase64(base64Kek);
    private final DataKey dataKey = DataKey.generate();

    private SecretRepository secretRepository;
    private OrganizationKeyRepository organizationKeyRepository;
    private EnvironmentRepository environmentRepository;

    @BeforeEach
    void setUp() throws TechnicalException {
        secretRepository = mock(SecretRepository.class);
        organizationKeyRepository = mock(OrganizationKeyRepository.class);
        environmentRepository = mock(EnvironmentRepository.class);
        lenient()
            .when(environmentRepository.findById(ENV))
            .thenReturn(Optional.of(Environment.builder().id(ENV).organizationId(ORG).build()));
        lenient()
            .when(organizationKeyRepository.findByOrganizationId(ORG))
            .thenReturn(
                Optional.of(
                    OrganizationKey.builder()
                        .id(dataKey.id())
                        .organizationId(ORG)
                        .kekId(kek.id())
                        .wrappedKey(Base64.getEncoder().encodeToString(kek.wrap(ORG, dataKey.material())))
                        .build()
                )
            );
        lenient().when(organizationKeyRepository.findKekIds()).thenReturn(Set.of(kek.id()));
    }

    @Test
    void should_answer_the_gravitee_provider_of_a_reference() {
        assertThat(provider(base64Kek).id()).isEqualTo("gravitee");
    }

    @Nested
    class Resolve {

        @Test
        void should_resolve_the_decrypted_value_of_a_secret_of_the_environment() throws TechnicalException {
            givenSecret(ENV, "llm-key", "sk-live-123");

            var secrets = started().resolve(ENV, LLM_KEY).blockingGet();

            assertThat(secrets.asMap().get("value").asString()).isEqualTo("sk-live-123");
        }

        @Test
        void should_resolve_nothing_when_the_environment_has_no_secret_with_that_key() throws TechnicalException {
            when(secretRepository.findByKey(ORG, ENV, "llm-key")).thenReturn(Optional.empty());

            started().resolve(ENV, LLM_KEY).test().assertComplete().assertNoValues();
        }

        @Test
        void should_resolve_nothing_for_an_environment_that_does_not_exist() throws TechnicalException {
            when(environmentRepository.findById("unknown")).thenReturn(Optional.empty());

            started().resolve("unknown", LLM_KEY).test().assertComplete().assertNoValues();
        }

        @Test
        void should_read_the_secret_only_in_the_environment_of_the_caller() throws TechnicalException {
            when(secretRepository.findByKey(ORG, ENV, "llm-key")).thenReturn(Optional.empty());

            started().resolve(ENV, LLM_KEY).test().assertNoValues();

            verify(secretRepository).findByKey(ORG, ENV, "llm-key");
            verifyNoMoreInteractions(secretRepository);
        }

        @Test
        void should_look_up_the_organization_of_an_environment_once() throws TechnicalException {
            givenSecret(ENV, "llm-key", "sk-live-123");
            var provider = started();

            provider.resolve(ENV, LLM_KEY).blockingGet();
            provider.resolve(ENV, LLM_KEY).blockingGet();

            verify(environmentRepository, times(1)).findById(ENV);
        }

        @Test
        void should_fail_when_the_repository_fails() throws TechnicalException {
            when(secretRepository.findByKey(anyString(), anyString(), anyString())).thenThrow(new TechnicalException("down"));

            started().resolve(ENV, LLM_KEY).test().assertError(SecretManagerException.class);
        }

        @Test
        void should_fail_without_the_value_when_the_secret_cannot_be_decrypted() throws TechnicalException {
            when(secretRepository.findByKey(ORG, ENV, "llm-key")).thenReturn(
                Optional.of(
                    Secret.builder().id("s-1").organizationId(ORG).environmentId(ENV).key("llm-key").encryptedValue("v1:x:AAAA").build()
                )
            );

            started()
                .resolve(ENV, LLM_KEY)
                .test()
                .assertError(err -> err instanceof SecretManagerException && !err.getMessage().contains("AAAA"));
        }

        @Test
        void should_fail_when_the_organization_has_no_data_key() throws TechnicalException {
            givenSecret(ENV, "llm-key", "sk-live-123");
            when(organizationKeyRepository.findByOrganizationId(ORG)).thenReturn(Optional.empty());

            started()
                .resolve(ENV, LLM_KEY)
                .test()
                .assertError(err -> err instanceof SecretManagerException && err.getMessage().contains("has no data key"));
        }

        @Test
        void should_fail_when_the_data_key_of_the_organization_cannot_be_read() throws TechnicalException {
            givenSecret(ENV, "llm-key", "sk-live-123");
            when(organizationKeyRepository.findByOrganizationId(ORG)).thenThrow(new TechnicalException("down"));

            started().resolve(ENV, LLM_KEY).test().assertError(SecretManagerException.class);
        }

        @Test
        void should_fail_when_another_key_encryption_key_wrapped_the_data_key_of_the_organization() throws TechnicalException {
            givenSecret(ENV, "llm-key", "sk-live-123");
            var otherKek = StaticKeyEncryptionKeyProvider.fromBase64(randomKek());
            when(organizationKeyRepository.findByOrganizationId(ORG)).thenReturn(
                Optional.of(
                    OrganizationKey.builder()
                        .id(dataKey.id())
                        .organizationId(ORG)
                        .kekId(otherKek.id())
                        .wrappedKey(Base64.getEncoder().encodeToString(otherKek.wrap(ORG, dataKey.material())))
                        .build()
                )
            );
            when(organizationKeyRepository.findKekIds()).thenReturn(Set.of(kek.id(), otherKek.id()));

            started()
                .resolve(ENV, LLM_KEY)
                .test()
                .assertError(err -> err instanceof SecretManagerException && !err.getMessage().contains("sk-live-123"));
        }
    }

    @Nested
    class Start {

        private final Logger logger = (Logger) LoggerFactory.getLogger(BuiltInSecretProvider.class);
        private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

        Start() {
            logs.start();
            logger.addAppender(logs);
        }

        @AfterEach
        void detachAppender() {
            logger.detachAppender(logs);
        }

        @Test
        void should_fail_with_the_property_and_the_reason_when_no_key_encryption_key_is_set() {
            assertThatThrownBy(() -> provider(null).start())
                .isInstanceOf(SecretManagerException.class)
                .hasMessageContaining("api.secrets.encryption.key")
                .hasMessageContaining("is not set");
        }

        @Test
        void should_fail_without_printing_the_key_when_the_key_encryption_key_is_malformed() {
            assertThatThrownBy(() -> provider("not-a-key").start())
                .isInstanceOf(SecretManagerException.class)
                .hasMessageContaining("api.secrets.encryption.key")
                .hasMessageNotContaining("not-a-key");
        }

        @Test
        void should_fail_when_the_key_encryption_key_wrapped_none_of_the_stored_data_keys() throws TechnicalException {
            when(organizationKeyRepository.findKekIds()).thenReturn(Set.of("another-kek"));

            assertThatThrownBy(() -> provider(base64Kek).start())
                .isInstanceOf(SecretManagerException.class)
                .hasMessageContaining("api.secrets.encryption.key")
                .hasMessageContaining("does not match")
                .hasMessageNotContaining(base64Kek);
        }

        @Test
        void should_start_when_no_data_key_is_stored_yet() throws TechnicalException {
            when(organizationKeyRepository.findKekIds()).thenReturn(Set.of());

            assertThat(provider(base64Kek).start()).isNotNull();
        }

        @Test
        void should_start_without_warning_when_the_key_encryption_key_wrapped_every_stored_data_key() {
            provider(base64Kek).start();

            assertThat(logs.list).isEmpty();
        }

        @Test
        void should_start_with_a_warning_when_the_key_encryption_key_wrapped_only_some_stored_data_keys() throws TechnicalException {
            when(organizationKeyRepository.findKekIds()).thenReturn(Set.of(kek.id(), "another-kek"));

            assertThat(provider(base64Kek).start()).isNotNull();

            assertThat(logs.list)
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.WARN);
                    assertThat(event.getFormattedMessage())
                        .contains(StaticKeyEncryptionKeyProvider.KEY_PROPERTY, "did not wrap")
                        .doesNotContain(base64Kek);
                });
        }
    }

    private void givenSecret(String environmentId, String key, String plaintext) throws TechnicalException {
        var secret = Secret.builder().id("s-1").organizationId(ORG).environmentId(environmentId).key(key).build();
        secret.setEncryptedValue(SecretEnvelope.seal(dataKey, new SecretScope(ORG, environmentId, secret.getId()), plaintext));
        when(secretRepository.findByKey(ORG, environmentId, key)).thenReturn(Optional.of(secret));
    }

    private BuiltInSecretProvider started() {
        return (BuiltInSecretProvider) provider(base64Kek).start();
    }

    private BuiltInSecretProvider provider(String base64Kek) {
        return new BuiltInSecretProvider(base64Kek, secretRepository, organizationKeyRepository, environmentRepository);
    }

    private static String randomKek() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
