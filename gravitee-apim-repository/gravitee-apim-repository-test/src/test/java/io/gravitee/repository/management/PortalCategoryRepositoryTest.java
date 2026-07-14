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

import io.gravitee.repository.management.model.PortalCategory;
import java.util.List;
import org.junit.jupiter.api.Test;

public class PortalCategoryRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/portal-category-tests/";
    }

    @Test
    public void shouldFindAllByEnvironmentIdOrderedByTitle() throws Exception {
        List<PortalCategory> categories = portalCategoryRepository.findAllByEnvironmentId("DEFAULT");

        assertThat(categories).extracting(PortalCategory::getTitle).containsExactly("Analytics", "Banking", "Weather");
    }

    @Test
    public void shouldFindAllByEnvironmentId_otherEnv() throws Exception {
        List<PortalCategory> categories = portalCategoryRepository.findAllByEnvironmentId("OTHER_ENV");

        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).getTitle()).isEqualTo("Other Env Category");
    }

    @Test
    public void shouldReturnEmptyListForUnknownEnvironmentId() throws Exception {
        assertThat(portalCategoryRepository.findAllByEnvironmentId("UNKNOWN")).isEmpty();
    }

    @Test
    public void shouldFindById() throws Exception {
        var portalCategory = portalCategoryRepository.findById("pc-2");

        assertThat(portalCategory).hasValueSatisfying(result -> {
            assertThat(result.getEnvironmentId()).isEqualTo("DEFAULT");
            assertThat(result.getTitle()).isEqualTo("Analytics");
            assertThat(result.getDescription()).isEqualTo("Analytics related APIs");
            assertThat(result.isVisible()).isFalse();
        });
    }

    @Test
    public void shouldReturnEmptyForUnknownId() throws Exception {
        assertThat(portalCategoryRepository.findById("unknown")).isEmpty();
    }

    @Test
    public void shouldCreate() throws Exception {
        var portalCategory = PortalCategory.builder()
            .id("new-pc")
            .environmentId("DEFAULT")
            .title("New Category")
            .description("A new category")
            .visible(true)
            .build();

        portalCategoryRepository.create(portalCategory);

        var saved = portalCategoryRepository.findById("new-pc");
        assertThat(saved).hasValueSatisfying(result -> {
            assertThat(result.getEnvironmentId()).isEqualTo("DEFAULT");
            assertThat(result.getTitle()).isEqualTo("New Category");
            assertThat(result.getDescription()).isEqualTo("A new category");
            assertThat(result.isVisible()).isTrue();
        });
    }

    @Test
    public void shouldCreateWithNullDescription() throws Exception {
        var portalCategory = PortalCategory.builder()
            .id("new-pc-null-desc")
            .environmentId("DEFAULT")
            .title("No Description")
            .description(null)
            .visible(true)
            .build();

        portalCategoryRepository.create(portalCategory);

        var saved = portalCategoryRepository.findById("new-pc-null-desc");
        assertThat(saved).hasValueSatisfying(result -> assertThat(result.getDescription()).isNull());
    }

    @Test
    public void shouldUpdate() throws Exception {
        var optional = portalCategoryRepository.findById("pc-3");
        assertThat(optional).as("Portal category to update not found").isPresent();

        var portalCategory = optional.get();
        portalCategory.setTitle("Updated Banking");
        portalCategory.setDescription("Updated description");
        portalCategory.setVisible(false);

        portalCategoryRepository.update(portalCategory);

        var updated = portalCategoryRepository.findById("pc-3");
        assertThat(updated).hasValueSatisfying(result -> {
            assertThat(result.getTitle()).isEqualTo("Updated Banking");
            assertThat(result.getDescription()).isEqualTo("Updated description");
            assertThat(result.isVisible()).isFalse();
        });
    }

    @Test
    public void shouldDelete() throws Exception {
        var nbBefore = portalCategoryRepository.findAllByEnvironmentId("DEFAULT").size();

        portalCategoryRepository.delete("pc-1");

        var nbAfter = portalCategoryRepository.findAllByEnvironmentId("DEFAULT").size();
        assertThat(nbAfter).isEqualTo(nbBefore - 1);
        assertThat(portalCategoryRepository.findById("pc-1")).isEmpty();
    }
}
