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
package io.gravitee.apim.infra.crud_service.secret;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.secret.exception.SecretKeyAlreadyExistsException;
import io.gravitee.apim.core.secret.exception.SecretNotFoundException;
import io.gravitee.apim.core.secret.model.Secret;
import io.gravitee.apim.infra.adapter.SecretAdapterImpl;
import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SecretRepository;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SecretCrudServiceImplTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-06T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-11-06T10:05:00Z");

    private final SecretRepository secretRepository = mock(SecretRepository.class);
    private final SecretCrudServiceImpl service = new SecretCrudServiceImpl(secretRepository, new SecretAdapterImpl());

    @Test
    void should_store_the_metadata_and_the_encrypted_value() throws TechnicalException {
        when(secretRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.create(aSecret(), "ciphertext");

        var captor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.Secret.class);
        verify(secretRepository).create(captor.capture());
        assertThat(captor.getValue()).isEqualTo(aStoredSecret());
        assertThat(created).isEqualTo(aSecret());
    }

    @Test
    void should_report_a_duplicate_key_as_a_conflict() throws TechnicalException {
        when(secretRepository.create(any())).thenThrow(new DuplicateKeyException("duplicate"));

        assertThatThrownBy(() -> service.create(aSecret(), "ciphertext")).isInstanceOf(SecretKeyAlreadyExistsException.class);
    }

    @Test
    void should_report_a_storage_failure_without_the_encrypted_value() throws TechnicalException {
        when(secretRepository.create(any())).thenThrow(new TechnicalException("db down"));

        assertThatThrownBy(() -> service.create(aSecret(), "ciphertext"))
            .isInstanceOf(TechnicalDomainException.class)
            .hasMessageNotContaining("ciphertext");
    }

    @Test
    void should_find_a_secret_by_id() throws TechnicalException {
        when(secretRepository.findById("org-1", "env-1", "secret-1")).thenReturn(Optional.of(aStoredSecret()));

        assertThat(service.findById("org-1", "env-1", "secret-1")).contains(aSecret());
    }

    @Test
    void should_find_a_secret_by_key() throws TechnicalException {
        when(secretRepository.findByKey("org-1", "env-1", "anthropic-key")).thenReturn(Optional.empty());

        assertThat(service.findByKey("org-1", "env-1", "anthropic-key")).isEmpty();
    }

    @Test
    void should_update_the_value_and_the_update_date() throws TechnicalException {
        var updated = aStoredSecret().toBuilder().encryptedValue("new-ciphertext").updatedAt(Date.from(UPDATED_AT)).build();
        when(secretRepository.update(any())).thenReturn(updated);

        var result = service.update(aSecret().withUpdatedAt(UPDATED_AT), "new-ciphertext");

        verify(secretRepository).update(updated);
        assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void should_report_the_update_of_a_deleted_secret_as_not_found() throws TechnicalException {
        when(secretRepository.update(any())).thenThrow(new TechnicalException("no row"));
        when(secretRepository.findById("org-1", "env-1", "secret-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(aSecret().withUpdatedAt(UPDATED_AT), "new-ciphertext")).isInstanceOf(
            SecretNotFoundException.class
        );
    }

    @Test
    void should_report_a_failed_update_of_an_existing_secret_as_technical() throws TechnicalException {
        when(secretRepository.update(any())).thenThrow(new TechnicalException("down"));
        when(secretRepository.findById("org-1", "env-1", "secret-1")).thenReturn(Optional.of(aStoredSecret()));

        assertThatThrownBy(() -> service.update(aSecret().withUpdatedAt(UPDATED_AT), "new-ciphertext")).isInstanceOf(
            TechnicalDomainException.class
        );
    }

    @Test
    void should_delete_the_secret_in_its_scope() throws TechnicalException {
        service.delete(aSecret());

        verify(secretRepository).delete("org-1", "env-1", "secret-1");
    }

    private static Secret aSecret() {
        return new Secret("secret-1", "org-1", "env-1", "anthropic-key", CREATED_AT, CREATED_AT);
    }

    private static io.gravitee.repository.management.model.Secret aStoredSecret() {
        return io.gravitee.repository.management.model.Secret.builder()
            .id("secret-1")
            .organizationId("org-1")
            .environmentId("env-1")
            .key("anthropic-key")
            .encryptedValue("ciphertext")
            .createdAt(Date.from(CREATED_AT))
            .updatedAt(Date.from(CREATED_AT))
            .build();
    }
}
