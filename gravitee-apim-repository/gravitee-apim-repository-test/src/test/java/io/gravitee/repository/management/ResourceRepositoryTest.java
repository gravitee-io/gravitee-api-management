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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.utils.UUID;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Resource;
import org.junit.jupiter.api.Test;
import java.util.Date;
import java.util.Optional;

public class ResourceRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/resource-tests/";
    }

    @Test
    public void shouldCreate() throws TechnicalException {
        var date = new Date();
        var id = UUID.random().toString();
        Resource resource = buildResource(id, "my-env", "fresh-resource", "cache", date);

        Resource created = resourceRepository.create(resource);

        assertThat(created.getId()).isEqualTo(id);
        assertThat(created.getName()).isEqualTo("fresh-resource");
        assertThat(created.getType()).isEqualTo("cache");
        assertThat(created.isEnabled()).isTrue();
    }

    @Test
    public void shouldFailOnDuplicateNameSameReference() throws TechnicalException {
        var date = new Date();
        Resource duplicate = buildResource(UUID.random().toString(), "my-env", "alpha-resource", "cache", date);

        assertThatThrownBy(() -> resourceRepository.create(duplicate)).isInstanceOf(Exception.class);
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        var existing = resourceRepository.findById("11111111-1111-1111-1111-111111111111").orElseThrow();
        var newUpdatedAt = new Date();

        existing.setName("alpha-resource-updated");
        existing.setType("cache");
        existing.setConfiguration("{\"ttl\":120}");
        existing.setEnabled(false);
        existing.setUpdatedAt(newUpdatedAt);

        Resource updated = resourceRepository.update(existing);

        assertThat(updated.getName()).isEqualTo("alpha-resource-updated");
        assertThat(updated.getConfiguration()).isEqualTo("{\"ttl\":120}");
        assertThat(updated.isEnabled()).isFalse();
        assertThat(updated.getUpdatedAt()).isEqualTo(newUpdatedAt);
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        Optional<Resource> found = resourceRepository.findById("22222222-2222-2222-2222-222222222222");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("beta-resource");
        assertThat(found.get().getType()).isEqualTo("oauth2");
    }

    @Test
    public void shouldNotFindWhenMissing() throws TechnicalException {
        assertThat(resourceRepository.findById("does-not-exist")).isEmpty();
    }

    @Test
    public void shouldFindByReferencePaginated() throws TechnicalException {
        Page<Resource> page = resourceRepository.findByReference(
            Resource.ReferenceType.ENVIRONMENT,
            "my-env",
            new PageableBuilder().pageNumber(0).pageSize(10).build(),
            null
        );

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(3L);
    }

    @Test
    public void shouldFindByReferenceFilterByQueryOnName() throws TechnicalException {
        Page<Resource> page = resourceRepository.findByReference(
            Resource.ReferenceType.ENVIRONMENT,
            "my-env",
            new PageableBuilder().pageNumber(0).pageSize(10).build(),
            "BETA"
        );

        assertThat(page.getContent()).extracting(Resource::getName).containsExactly("beta-resource");
    }

    @Test
    public void shouldFindByReferenceFilterByQueryOnType() throws TechnicalException {
        Page<Resource> page = resourceRepository.findByReference(
            Resource.ReferenceType.ENVIRONMENT,
            "my-env",
            new PageableBuilder().pageNumber(0).pageSize(10).build(),
            "OAuth2"
        );

        assertThat(page.getContent()).extracting(Resource::getName).containsExactly("beta-resource");
    }

    @Test
    public void shouldNotFindByReferenceForOtherEnvironment() throws TechnicalException {
        Page<Resource> page = resourceRepository.findByReference(
            Resource.ReferenceType.ENVIRONMENT,
            "other-env",
            new PageableBuilder().pageNumber(0).pageSize(10).build(),
            null
        );

        assertThat(page.getContent()).extracting(Resource::getName).containsExactly("foreign-resource");
    }

    @Test
    public void shouldExistsByNameAndReference() throws TechnicalException {
        assertThat(resourceRepository.existsByNameAndReference("alpha-resource", Resource.ReferenceType.ENVIRONMENT, "my-env")).isTrue();
        assertThat(
            resourceRepository.existsByNameAndReference("alpha-resource", Resource.ReferenceType.ENVIRONMENT, "other-env")
        ).isFalse();
        assertThat(resourceRepository.existsByNameAndReference("missing", Resource.ReferenceType.ENVIRONMENT, "my-env")).isFalse();
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        resourceRepository.delete("33333333-3333-3333-3333-333333333333");

        assertThat(resourceRepository.findById("33333333-3333-3333-3333-333333333333")).isEmpty();
    }

    private static Resource buildResource(String id, String referenceId, String name, String type, Date date) {
        return Resource.builder()
            .id(id)
            .referenceId(referenceId)
            .referenceType(Resource.ReferenceType.ENVIRONMENT)
            .name(name)
            .type(type)
            .configuration("{}")
            .enabled(true)
            .createdAt(date)
            .updatedAt(date)
            .build();
    }
}
