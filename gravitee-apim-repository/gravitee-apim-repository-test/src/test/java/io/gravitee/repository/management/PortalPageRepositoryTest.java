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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.PortalPage;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class PortalPageRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/portalpage-tests/";
    }

    @Test
    public void should_find_by_id() throws Exception {
        // Given
        String portalPageId = "test-portal-page-id";

        // When
        Optional<PortalPage> found = portalPageRepository.findById(portalPageId);

        // Then
        assertThat(found).isNotNull();
        assertThat(found).isPresent();

        var portalPage = found.get();
        assertThat(portalPage.getId()).isEqualTo("test-portal-page-id");
        assertThat(portalPage.getName()).isEqualTo("Test Portal Page");
        assertThat(portalPage.getContent())
            .isEqualTo("This is a test portal page content with some sample text to verify the repository functionality.");
        assertThat(portalPage.getCreatedAt()).isEqualTo(new Date(1486771200000L));
        assertThat(portalPage.getUpdatedAt()).isEqualTo(new Date(1486771200000L));
    }

    @Test
    public void should_find_all_portal_pages() throws Exception {
        // When
        Set<PortalPage> allPages = portalPageRepository.findAll();

        // Then
        assertThat(allPages).isNotNull();
        assertThat(allPages.size()).isEqualTo(3);

        // Verify specific pages exist
        assertThat(allPages.stream().anyMatch(p -> p.getId().equals("test-portal-page-id"))).isTrue();
        assertThat(allPages.stream().anyMatch(p -> p.getId().equals("update-portal-page"))).isTrue();
        assertThat(allPages.stream().anyMatch(p -> p.getId().equals("delete-portal-page"))).isTrue();
    }

    @Test
    public void should_create_portal_page() throws Exception {
        // Given
        PortalPage portalPage = new PortalPage();
        portalPage.setId("new-portal-page");
        portalPage.setEnvironmentId("test-environment");
        portalPage.setName("Test Portal Page");
        portalPage.setContent("Test content for portal page");
        portalPage.setCreatedAt(new Date());
        portalPage.setUpdatedAt(new Date());

        // When
        PortalPage created = portalPageRepository.create(portalPage);

        // Then
        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(portalPage.getId());
        assertThat(created.getEnvironmentId()).isEqualTo(portalPage.getEnvironmentId());
        assertThat(created.getName()).isEqualTo(portalPage.getName());
        assertThat(created.getContent()).isEqualTo(portalPage.getContent());
    }

    @Test
    public void should_update_portal_page() throws Exception {
        // Given
        String portalPageId = "update-portal-page";
        PortalPage portalPage = new PortalPage();
        portalPage.setId(portalPageId);
        portalPage.setEnvironmentId("test-environment");
        portalPage.setName("Updated Portal Page Name");
        portalPage.setContent("Updated content for testing update operations");
        portalPage.setCreatedAt(new Date(1439022010883L));
        portalPage.setUpdatedAt(new Date());

        // When
        PortalPage updated = portalPageRepository.update(portalPage);

        // Then
        assertThat(updated).isNotNull();
        assertThat(updated.getId()).isEqualTo(portalPageId);
        assertThat(updated.getName()).isEqualTo("Updated Portal Page Name");
        assertThat(updated.getContent()).isEqualTo("Updated content for testing update operations");

        // Verify the update was persisted
        Optional<PortalPage> foundAfterUpdate = portalPageRepository.findById(portalPageId);
        assertThat(foundAfterUpdate).isPresent();
        assertThat(foundAfterUpdate.get().getName()).isEqualTo("Updated Portal Page Name");
        assertThat(foundAfterUpdate.get().getContent()).isEqualTo("Updated content for testing update operations");
    }

    @Test
    public void should_throw_error_when_updating_non_existing_portal_page() {
        // Given
        PortalPage nonExisting = new PortalPage();
        nonExisting.setId("non-existing-id");
        nonExisting.setEnvironmentId("test-environment");
        nonExisting.setName("Non Existing Page");
        nonExisting.setContent("Content for non-existing page");
        nonExisting.setCreatedAt(new Date());
        nonExisting.setUpdatedAt(new Date());

        assertThatThrownBy(() -> portalPageRepository.update(nonExisting))
            .matches(th -> th instanceof IllegalStateException || th instanceof TechnicalException);
    }

    @Test
    public void should_delete_portal_page() throws Exception {
        // Given
        String portalPageId = "delete-portal-page";

        // Verify the page exists before deletion
        Optional<PortalPage> beforeDelete = portalPageRepository.findById(portalPageId);
        assertThat(beforeDelete).isPresent();
        assertThat(beforeDelete.get().getName()).isEqualTo("Delete Portal Page");

        // When
        portalPageRepository.delete(portalPageId);

        // Then
        Optional<PortalPage> deleted = portalPageRepository.findById(portalPageId);
        assertThat(deleted).isNotPresent();
    }

    @Test
    public void should_find_by_ids() {
        // Given
        var ids = java.util.List.of("test-portal-page-id", "update-portal-page", "delete-portal-page");

        // When
        var pages = portalPageRepository.findByIds(ids);

        // Then
        assertThat(pages).isNotNull();
        assertThat(pages.size()).isEqualTo(3);
        assertThat(pages.stream().anyMatch(p -> p.getId().equals("test-portal-page-id"))).isTrue();
        assertThat(pages.stream().anyMatch(p -> p.getId().equals("update-portal-page"))).isTrue();
        assertThat(pages.stream().anyMatch(p -> p.getId().equals("delete-portal-page"))).isTrue();
    }

    @Test
    public void should_return_empty_list_when_no_ids_provided() {
        // When
        var pages = portalPageRepository.findByIds(List.of());

        // Then
        assertThat(pages).isNotNull();
        assertThat(pages).isEmpty();
    }
}
