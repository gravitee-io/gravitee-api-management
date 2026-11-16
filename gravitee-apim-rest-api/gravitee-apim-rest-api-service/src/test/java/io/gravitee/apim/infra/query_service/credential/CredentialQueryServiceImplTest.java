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
package io.gravitee.apim.infra.query_service.credential;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.credential.model.Credential;
import io.gravitee.apim.core.credential.model.CredentialQuery;
import io.gravitee.apim.core.credential.model.CredentialStrategy;
import io.gravitee.apim.core.credential.model.SecretReference;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.adapter.CredentialAdapter;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CredentialRepository;
import io.gravitee.repository.management.api.search.CredentialCriteria;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CredentialQueryServiceImplTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-06T10:00:00Z");

    private final CredentialRepository credentialRepository = mock(CredentialRepository.class);
    private final CredentialQueryServiceImpl service = new CredentialQueryServiceImpl(credentialRepository);

    @Test
    void should_ask_for_the_zero_based_page_and_return_the_one_based_page() throws TechnicalException {
        when(credentialRepository.search(eq("org-1"), eq("env-1"), any(), any())).thenReturn(
            new Page<>(List.of(CredentialAdapter.INSTANCE.toRepository(aCredential())), 1, 1, 11)
        );

        var page = service.search("org-1", "env-1", CredentialQuery.all(), new PageableImpl(2, 10));

        verify(credentialRepository).search(eq("org-1"), eq("env-1"), any(), argThat(p -> p.pageNumber() == 1 && p.pageSize() == 10));
        assertThat(page.getContent()).containsExactly(aCredential());
        assertThat(page.getPageNumber()).isEqualTo(2);
        assertThat(page.getTotalElements()).isEqualTo(11);
    }

    @Test
    void should_pass_every_filter_to_the_repository() throws TechnicalException {
        when(credentialRepository.search(any(), any(), any(), any())).thenReturn(new Page<>(List.of(), 0, 0, 0));

        service.search(
            "org-1",
            "env-1",
            CredentialQuery.builder().name("openai").strategies(Set.of(CredentialStrategy.API_KEY)).serviceId("openai").build(),
            new PageableImpl(1, 10)
        );

        verify(credentialRepository).search(
            eq("org-1"),
            eq("env-1"),
            eq(CredentialCriteria.builder().query("openai").strategies(Set.of("API_KEY")).serviceId("openai").build()),
            any()
        );
    }

    @Test
    void should_leave_every_filter_empty_when_none_is_given() throws TechnicalException {
        when(credentialRepository.search(any(), any(), any(), any())).thenReturn(new Page<>(List.of(), 0, 0, 0));

        service.search("org-1", "env-1", CredentialQuery.all(), new PageableImpl(1, 10));

        verify(credentialRepository).search(eq("org-1"), eq("env-1"), eq(CredentialCriteria.builder().build()), any());
    }

    @Test
    void should_report_a_storage_failure_as_technical() throws TechnicalException {
        when(credentialRepository.search(any(), any(), any(), any())).thenThrow(new TechnicalException("db down"));

        assertThatThrownBy(() -> service.search("org-1", "env-1", CredentialQuery.all(), new PageableImpl(1, 10))).isInstanceOf(
            TechnicalDomainException.class
        );
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
