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
package io.gravitee.apim.infra.secret;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.EnvironmentCrudServiceInMemory;
import inmemory.SecretCrudServiceInMemory;
import inmemory.SecretValueEncryptorInMemory;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.secret.model.Secret;
import io.gravitee.repository.exceptions.TechnicalException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SecretValueRepositoryImplTest {

    private static final String ORG = "org-1";
    private static final String ENV = "env-1";

    private final EnvironmentCrudServiceInMemory environmentCrudService = new EnvironmentCrudServiceInMemory();
    private final SecretCrudServiceInMemory secretCrudService = new SecretCrudServiceInMemory();
    private final SecretValueEncryptorInMemory encryptor = new SecretValueEncryptorInMemory();
    private final SecretValueRepositoryImpl repository = new SecretValueRepositoryImpl(
        environmentCrudService,
        secretCrudService,
        encryptor
    );

    @BeforeEach
    void setUp() {
        environmentCrudService.initWith(List.of(Environment.builder().id(ENV).organizationId(ORG).build()));
    }

    @Test
    void should_find_the_decrypted_value_of_a_secret_of_the_environment() throws TechnicalException {
        givenSecret("s-1", "llm-key", "sk-live-123");

        assertThat(repository.findValue(ENV, "llm-key")).contains("sk-live-123");
    }

    @Test
    void should_find_nothing_when_the_environment_has_no_secret_with_that_key() throws TechnicalException {
        assertThat(repository.findValue(ENV, "llm-key")).isEmpty();
    }

    @Test
    void should_find_nothing_for_an_environment_that_does_not_exist() throws TechnicalException {
        assertThat(repository.findValue("unknown", "llm-key")).isEmpty();
    }

    @Test
    void should_find_nothing_when_the_secret_is_deleted_after_it_was_found() throws TechnicalException {
        var deletingCrudService = new SecretCrudServiceInMemory() {
            @Override
            public Optional<Secret> findByKey(String organizationId, String environmentId, String key) {
                Optional<Secret> found = super.findByKey(organizationId, environmentId, key);
                found.ifPresent(this::delete);
                return found;
            }
        };
        var secret = Secret.create("s-1", ORG, ENV, "llm-key", Instant.now());
        deletingCrudService.create(secret, encryptor.encrypt(secret, "sk-live-123"));

        var racingRepository = new SecretValueRepositoryImpl(environmentCrudService, deletingCrudService, encryptor);

        assertThat(racingRepository.findValue(ENV, "llm-key")).isEmpty();
    }

    @Test
    void should_fail_when_the_value_cannot_be_decrypted() {
        var secret = Secret.create("s-1", ORG, ENV, "llm-key", Instant.now());
        secretCrudService.create(secret, "not-encrypted-for-this-secret");

        assertThatThrownBy(() -> repository.findValue(ENV, "llm-key")).isInstanceOf(TechnicalDomainException.class);
    }

    private void givenSecret(String id, String key, String plaintext) {
        var secret = Secret.create(id, ORG, ENV, key, Instant.now());
        secretCrudService.create(secret, encryptor.encrypt(secret, plaintext));
    }
}
