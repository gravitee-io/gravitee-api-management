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
package io.gravitee.apim.infra.query_service.secret;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.secret.model.Secret;
import io.gravitee.apim.infra.adapter.SecretAdapterImpl;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SecretRepository;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

class SecretQueryServiceImplTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-06T10:00:00Z");

    private final SecretRepository secretRepository = mock(SecretRepository.class);
    private final SecretQueryServiceImpl service = new SecretQueryServiceImpl(secretRepository, new SecretAdapterImpl());

    @Test
    void should_ask_for_the_zero_based_page_and_return_the_one_based_page() throws TechnicalException {
        when(secretRepository.search(eq("org-1"), eq("env-1"), any())).thenReturn(new Page<>(List.of(aStoredSecret()), 1, 1, 11));

        var page = service.search("org-1", "env-1", new PageableImpl(2, 10));

        verify(secretRepository).search(eq("org-1"), eq("env-1"), argThat(p -> p.pageNumber() == 1 && p.pageSize() == 10));
        assertThat(page.getContent()).containsExactly(aSecret());
        assertThat(page.getPageNumber()).isEqualTo(2);
        assertThat(page.getTotalElements()).isEqualTo(11);
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
