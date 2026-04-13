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
package io.gravitee.repository.management;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.catalog.LlmProvider;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.Test;

public class CatalogSourceRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @Test
    public void should_create_and_find_llm_provider_source() throws Exception {
        var id = UUID.randomUUID().toString();
        var now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        var source = new LlmProvider(id, now, "OpenAI", "sk-test-key");

        catalogSourceRepository.create(source);

        var found = catalogSourceRepository.findById(id);

        assertThat(found).isPresent();
        assertThat(found.get()).isInstanceOf(LlmProvider.class);

        var provider = (LlmProvider) found.get();
        assertThat(provider.id()).isEqualTo(id);
        assertThat(provider.name()).isEqualTo("OpenAI");
        assertThat(provider.apiKey()).isEqualTo("sk-test-key");
        assertThat(provider.createdAt()).isEqualTo(now);
    }

    @Test
    public void should_return_empty_when_source_not_found() throws Exception {
        var found = catalogSourceRepository.findById("non-existent-id");

        assertThat(found).isEmpty();
    }

    @Test
    public void should_delete_source() throws Exception {
        var id = UUID.randomUUID().toString();
        var source = new LlmProvider(id, Instant.now(), "ToDelete", "sk-delete");

        catalogSourceRepository.create(source);
        catalogSourceRepository.delete(id);

        var found = catalogSourceRepository.findById(id);
        assertThat(found).isEmpty();
    }

    @Test
    public void should_find_sources_by_type() throws Exception {
        var source1 = new LlmProvider(UUID.randomUUID().toString(), Instant.now(), "Provider1", "sk-1");
        var source2 = new LlmProvider(UUID.randomUUID().toString(), Instant.now(), "Provider2", "sk-2");

        catalogSourceRepository.create(source1);
        catalogSourceRepository.create(source2);

        var pageable = new PageableBuilder().pageNumber(0).pageSize(10).build();
        var page = catalogSourceRepository.findByType(LlmProvider.class, pageable);

        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(page.getContent()).allSatisfy(s -> assertThat(s).isInstanceOf(LlmProvider.class));
    }

    @Test
    public void should_find_all_sources_with_pagination() throws Exception {
        for (int i = 0; i < 5; i++) {
            catalogSourceRepository.create(new LlmProvider(UUID.randomUUID().toString(), Instant.now(), "Provider-" + i, "sk-" + i));
        }

        var page1 = catalogSourceRepository.findAll(new PageableBuilder().pageNumber(0).pageSize(2).build());
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page1.getTotalElements()).isGreaterThanOrEqualTo(5);

        var page2 = catalogSourceRepository.findAll(new PageableBuilder().pageNumber(1).pageSize(2).build());
        assertThat(page2.getContent()).hasSize(2);

        var page3 = catalogSourceRepository.findAll(new PageableBuilder().pageNumber(2).pageSize(2).build());
        assertThat(page3.getContent()).hasSizeGreaterThanOrEqualTo(1);
    }
}
