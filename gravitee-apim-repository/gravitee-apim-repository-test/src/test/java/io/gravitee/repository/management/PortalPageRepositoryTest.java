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

import static org.junit.Assert.*;
import static org.junit.platform.commons.function.Try.success;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.PortalPage;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
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
        assertNotNull("PortalPage should be found", found);
        assertTrue("PortalPage should be present", found.isPresent());

        var portalPage = found.get();
        assertEquals("id", "test-portal-page-id", portalPage.getId());
        assertEquals("name", "Test Portal Page", portalPage.getName());
        assertEquals(
            "content",
            "This is a test portal page content with some sample text to verify the repository functionality.",
            portalPage.getContent()
        );
        assertEquals("createdAt", new Date(1486771200000L), portalPage.getCreatedAt());
        assertEquals("updatedAt", new Date(1486771200000L), portalPage.getUpdatedAt());
    }

    @Test
    public void should_find_all_portal_pages() throws Exception {
        // When
        Set<PortalPage> allPages = portalPageRepository.findAll();

        // Then
        assertNotNull("All PortalPages should not be null", allPages);
        assertEquals("Should find exactly 13 PortalPages", 3, allPages.size());

        // Verify specific pages exist
        assertTrue("Should contain test portal page", allPages.stream().anyMatch(p -> p.getId().equals("test-portal-page-id")));
        assertTrue("Should contain update portal page", allPages.stream().anyMatch(p -> p.getId().equals("update-portal-page")));
        assertTrue("Should contain delete portal page", allPages.stream().anyMatch(p -> p.getId().equals("delete-portal-page")));
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
        assertNotNull("Created PortalPage should not be null", created);
        assertEquals("PortalPage ID should match", portalPage.getId(), created.getId());
        assertEquals("PortalPage environmentId should match", portalPage.getEnvironmentId(), created.getEnvironmentId());
        assertEquals("PortalPage name should match", portalPage.getName(), created.getName());
        assertEquals("PortalPage content should match", portalPage.getContent(), created.getContent());
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
        assertNotNull("Updated PortalPage should not be null", updated);
        assertEquals("PortalPage ID should match", portalPageId, updated.getId());
        assertEquals("PortalPage name should match", "Updated Portal Page Name", updated.getName());
        assertEquals("PortalPage content should match", "Updated content for testing update operations", updated.getContent());

        // Verify the update was persisted
        Optional<PortalPage> foundAfterUpdate = portalPageRepository.findById(portalPageId);
        assertTrue("PortalPage should exist after update", foundAfterUpdate.isPresent());
        assertEquals("Updated name should be persisted", "Updated Portal Page Name", foundAfterUpdate.get().getName());
        assertEquals(
            "Updated content should be persisted",
            "Updated content for testing update operations",
            foundAfterUpdate.get().getContent()
        );
    }

    @Test
    public void should_throw_error_when_updating_non_existing_portal_page() throws TechnicalException {
        // Given
        PortalPage nonExisting = new PortalPage();
        nonExisting.setId("non-existing-id");
        nonExisting.setEnvironmentId("test-environment");
        nonExisting.setName("Non Existing Page");
        nonExisting.setContent("Content for non-existing page");
        nonExisting.setCreatedAt(new Date());
        nonExisting.setUpdatedAt(new Date());

        try {
            // When
            portalPageRepository.update(nonExisting);
            fail("Updating a non-existing PortalPage should throw an exception");
        } catch (IllegalStateException | TechnicalException ex) {
            // Then
            success("Test passed with one of expected exceptions thrown");
        } catch (Exception ex) {
            fail("Unexpected exception: " + ex);
        }
    }

    @Test
    public void should_delete_portal_page() throws Exception {
        // Given
        String portalPageId = "delete-portal-page";

        // Verify the page exists before deletion
        Optional<PortalPage> beforeDelete = portalPageRepository.findById(portalPageId);
        assertTrue("PortalPage should exist before deletion", beforeDelete.isPresent());
        assertEquals("PortalPage name should match", "Delete Portal Page", beforeDelete.get().getName());

        // When
        portalPageRepository.delete(portalPageId);

        // Then
        Optional<PortalPage> deleted = portalPageRepository.findById(portalPageId);
        assertFalse("PortalPage should not exist after deletion", deleted.isPresent());
    }
}
