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
package io.gravitee.apim.infra.crud_service.credential;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.credential.model.Credential;
import io.gravitee.apim.core.credential.model.CredentialStrategy;
import io.gravitee.apim.core.credential.model.SecretReference;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.adapter.CredentialAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CredentialRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CredentialCrudServiceImplTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-06T10:00:00Z");

    private final CredentialRepository credentialRepository = mock(CredentialRepository.class);
    private final CredentialCrudServiceImpl service = new CredentialCrudServiceImpl(credentialRepository);

    @Test
    void should_store_the_credential() throws TechnicalException {
        when(credentialRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.create(aCredential());

        verify(credentialRepository).create(CredentialAdapter.INSTANCE.toRepository(aCredential()));
        assertThat(created).isEqualTo(aCredential());
    }

    @Test
    void should_find_a_credential_in_its_scope() throws TechnicalException {
        when(credentialRepository.findById("org-1", "env-1", "credential-1")).thenReturn(
            Optional.of(CredentialAdapter.INSTANCE.toRepository(aCredential()))
        );

        assertThat(service.findById("org-1", "env-1", "credential-1")).contains(aCredential());
    }

    @Test
    void should_update_the_credential() throws TechnicalException {
        var renamed = aCredential().toBuilder().name("OpenAI production").build();
        when(credentialRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.update(renamed)).isEqualTo(renamed);
        verify(credentialRepository).update(CredentialAdapter.INSTANCE.toRepository(renamed));
    }

    @Test
    void should_delete_the_credential_in_its_scope() throws TechnicalException {
        service.delete(aCredential());

        verify(credentialRepository).delete("org-1", "env-1", "credential-1");
    }

    @Test
    void should_report_a_storage_failure_as_technical() throws TechnicalException {
        when(credentialRepository.create(any())).thenThrow(new TechnicalException("db down"));

        assertThatThrownBy(() -> service.create(aCredential()))
            .isInstanceOf(TechnicalDomainException.class)
            .hasMessageContaining("credential-1");
    }

    private static Credential aCredential() {
        return Credential.builder()
            .id("credential-1")
            .organizationId("org-1")
            .environmentId("env-1")
            .name("OpenAI")
            .strategy(CredentialStrategy.BEARER)
            .serviceId("openai")
            .ownerId("user-1")
            .configuration(Map.of())
            .secretReferences(Map.of("token", new SecretReference("/vault/openai:token", null)))
            .createdAt(CREATED_AT)
            .updatedAt(CREATED_AT)
            .build();
    }
}
