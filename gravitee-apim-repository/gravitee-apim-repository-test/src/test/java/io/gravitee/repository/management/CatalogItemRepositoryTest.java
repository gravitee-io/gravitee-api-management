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
import io.gravitee.repository.management.model.catalog.Model;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.Test;

public class CatalogItemRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @Test
    public void should_create_and_find_model_item() throws Exception {
        var id = UUID.randomUUID().toString();
        var sourceId = UUID.randomUUID().toString();
        var now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        var item = new Model(id, sourceId, now, "gpt-4", "GPT-4 large language model");

        catalogItemRepository.create(item);

        var found = catalogItemRepository.findById(id);

        assertThat(found).isPresent();
        assertThat(found.get()).isInstanceOf(Model.class);

        var model = (Model) found.get();
        assertThat(model.id()).isEqualTo(id);
        assertThat(model.sourceId()).isEqualTo(sourceId);
        assertThat(model.name()).isEqualTo("gpt-4");
        assertThat(model.description()).isEqualTo("GPT-4 large language model");
        assertThat(model.createdAt()).isEqualTo(now);
    }

    @Test
    public void should_return_empty_when_item_not_found() throws Exception {
        var found = catalogItemRepository.findById("non-existent-id");

        assertThat(found).isEmpty();
    }

    @Test
    public void should_delete_item() throws Exception {
        var id = UUID.randomUUID().toString();
        var item = new Model(id, UUID.randomUUID().toString(), Instant.now(), "to-delete", "Will be deleted");

        catalogItemRepository.create(item);
        catalogItemRepository.delete(id);

        var found = catalogItemRepository.findById(id);
        assertThat(found).isEmpty();
    }

    @Test
    public void should_find_items_by_type() throws Exception {
        var sourceId = UUID.randomUUID().toString();
        var item1 = new Model(UUID.randomUUID().toString(), sourceId, Instant.now(), "gpt-4", "GPT-4");
        var item2 = new Model(UUID.randomUUID().toString(), sourceId, Instant.now(), "gpt-4o", "GPT-4o");

        catalogItemRepository.create(item1);
        catalogItemRepository.create(item2);

        var pageable = new PageableBuilder().pageNumber(0).pageSize(10).build();
        var page = catalogItemRepository.findByType(Model.class, pageable);

        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(page.getContent()).allSatisfy(i -> assertThat(i).isInstanceOf(Model.class));
    }

    @Test
    public void should_find_items_by_source_id() throws Exception {
        var sourceId1 = UUID.randomUUID().toString();
        var sourceId2 = UUID.randomUUID().toString();

        catalogItemRepository.create(new Model(UUID.randomUUID().toString(), sourceId1, Instant.now(), "gpt-4", "GPT-4"));
        catalogItemRepository.create(new Model(UUID.randomUUID().toString(), sourceId1, Instant.now(), "gpt-4o", "GPT-4o"));
        catalogItemRepository.create(new Model(UUID.randomUUID().toString(), sourceId2, Instant.now(), "claude-3", "Claude 3"));

        var itemsSource1 = catalogItemRepository.findBySourceId(sourceId1);
        assertThat(itemsSource1).hasSize(2);
        assertThat(itemsSource1).allSatisfy(item -> {
            assertThat(item).isInstanceOf(Model.class);
            assertThat(item.sourceId()).isEqualTo(sourceId1);
        });

        var itemsSource2 = catalogItemRepository.findBySourceId(sourceId2);
        assertThat(itemsSource2).hasSize(1);
        assertThat(itemsSource2.getFirst().sourceId()).isEqualTo(sourceId2);
    }

    @Test
    public void should_find_all_items_with_pagination() throws Exception {
        var sourceId = UUID.randomUUID().toString();
        for (int i = 0; i < 5; i++) {
            catalogItemRepository.create(new Model(UUID.randomUUID().toString(), sourceId, Instant.now(), "model-" + i, "Model " + i));
        }

        var page1 = catalogItemRepository.findAll(new PageableBuilder().pageNumber(0).pageSize(2).build());
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page1.getTotalElements()).isGreaterThanOrEqualTo(5);

        var page2 = catalogItemRepository.findAll(new PageableBuilder().pageNumber(1).pageSize(2).build());
        assertThat(page2.getContent()).hasSize(2);

        var page3 = catalogItemRepository.findAll(new PageableBuilder().pageNumber(2).pageSize(2).build());
        assertThat(page3.getContent()).hasSizeGreaterThanOrEqualTo(1);
    }
}
