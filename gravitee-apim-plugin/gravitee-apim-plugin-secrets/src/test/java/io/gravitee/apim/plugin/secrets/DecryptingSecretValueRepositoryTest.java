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
import static org.mockito.Mockito.when;

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
import io.gravitee.secrets.api.errors.SecretManagerException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DecryptingSecretValueRepositoryTest {

    private static final String ORG = "org-1";
    private static final String ENV = "env-1";
    private static final String KEY = "llm-key";

    private final String base64Kek = randomKek();
    private final StaticKeyEncryptionKeyProvider kek = StaticKeyEncryptionKeyProvider.fromBase64(base64Kek);
    private final DataKey dataKey = DataKey.generate();
    private final SecretRepository secretRepository = mock(SecretRepository.class);
    private final OrganizationKeyRepository organizationKeyRepository = mock(OrganizationKeyRepository.class);
    private final EnvironmentRepository environmentRepository = mock(EnvironmentRepository.class);

    @BeforeEach
    void setUp() throws TechnicalException {
        when(environmentRepository.findById(ENV)).thenReturn(Optional.of(Environment.builder().id(ENV).organizationId(ORG).build()));
        when(organizationKeyRepository.findByOrganizationId(ORG)).thenReturn(
            Optional.of(
                OrganizationKey.builder()
                    .id(dataKey.id())
                    .organizationId(ORG)
                    .kekId(kek.id())
                    .wrappedKey(Base64.getEncoder().encodeToString(kek.wrap(ORG, dataKey.material())))
                    .build()
            )
        );
        when(organizationKeyRepository.findKekIds()).thenReturn(Set.of(kek.id()));
    }

    @Test
    void should_find_the_decrypted_value_without_being_started() throws TechnicalException {
        givenSecret("sk-live-123");

        assertThat(repository(base64Kek).findValue(ENV, KEY)).contains("sk-live-123");
    }

    @Test
    void should_find_nothing_when_the_environment_has_no_secret_with_that_key() throws TechnicalException {
        when(secretRepository.findByKey(ORG, ENV, KEY)).thenReturn(Optional.empty());

        assertThat(repository(base64Kek).findValue(ENV, KEY)).isEmpty();
    }

    @Test
    void should_refuse_every_secret_when_no_key_encryption_key_is_set() throws TechnicalException {
        givenSecret("sk-live-123");
        var repository = repository(null);

        assertThatThrownBy(() -> repository.findValue(ENV, KEY))
            .isInstanceOf(SecretManagerException.class)
            .hasMessageContaining("is not set");
        assertThatThrownBy(() -> repository.findValue(ENV, KEY))
            .isInstanceOf(SecretManagerException.class)
            .hasMessageContaining("is not set");
    }

    private void givenSecret(String plaintext) throws TechnicalException {
        var secret = Secret.builder().id("s-1").organizationId(ORG).environmentId(ENV).key(KEY).build();
        secret.setEncryptedValue(SecretEnvelope.seal(dataKey, new SecretScope(ORG, ENV, secret.getId()), plaintext));
        when(secretRepository.findByKey(ORG, ENV, KEY)).thenReturn(Optional.of(secret));
    }

    private DecryptingSecretValueRepository repository(String base64Kek) {
        return new DecryptingSecretValueRepository(base64Kek, secretRepository, organizationKeyRepository, environmentRepository);
    }

    private static String randomKek() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
