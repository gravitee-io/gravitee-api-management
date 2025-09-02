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
import io.gravitee.repository.management.model.PortalPageContext;
import io.gravitee.repository.management.model.PortalPageContextType;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class PortalPageContextRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/portalpagecontext-tests/";
    }

    @Test
    public void should_create_portal_page_context() throws Exception {
        // Given
        PortalPageContext portalPageContext = new PortalPageContext();
        portalPageContext.setId("new-portal-page-context-id");
        portalPageContext.setEnvironmentId("test-environment");
        portalPageContext.setPageId("new-portal-page");
        portalPageContext.setContextType(PortalPageContextType.HOMEPAGE);
        portalPageContext.setPublished(true);

        // When
        PortalPageContext created = portalPageContextRepository.create(portalPageContext);

        // Then
        assertNotNull("Created PortalPageContext should not be null", created);
        assertEquals("PortalPageContext id should match", portalPageContext.getId(), created.getId());
        assertEquals("PortalPageContext pageId should match", portalPageContext.getPageId(), created.getPageId());
        assertEquals("PortalPageContext environmentId should match", portalPageContext.getEnvironmentId(), created.getEnvironmentId());
        assertEquals("PortalPageContext contextType should match", portalPageContext.getContextType(), created.getContextType());
        assertEquals("PortalPageContext published should match", portalPageContext.isPublished(), created.isPublished());
    }

    @Test
    public void should_find_all_by_context_type_and_environment_id() throws Exception {
        // Given
        PortalPageContextType contextType = PortalPageContextType.HOMEPAGE;

        // When
        List<PortalPageContext> foundContexts = portalPageContextRepository.findAllByContextTypeAndEnvironmentId(
            contextType,
            "test-environment"
        );

        // Then
        assertNotNull("Found contexts should not be null", foundContexts);
        assertEquals("Should find exactly 2 contexts for HOMEPAGE", 2, foundContexts.size());

        // Verify all contexts have the correct context type
        assertTrue(
            "All contexts should have HOMEPAGE context",
            foundContexts.stream().allMatch(c -> c.getContextType() == PortalPageContextType.HOMEPAGE)
        );

        // Verify specific contexts exist
        assertTrue(
            "Should contain test portal page context",
            foundContexts.stream().anyMatch(c -> c.getPageId().equals("test-portal-page-id"))
        );
        assertTrue(
            "Should contain update portal page context",
            foundContexts.stream().anyMatch(c -> c.getPageId().equals("update-portal-page"))
        );
    }

    @Test
    public void should_throw_error_when_updating_non_existing_portal_page_context() throws TechnicalException {
        // Given
        PortalPageContext nonExisting = new PortalPageContext();
        nonExisting.setId("non-existing-id");
        nonExisting.setEnvironmentId("test-environment");
        nonExisting.setPageId("non-existing-page");
        nonExisting.setContextType(PortalPageContextType.HOMEPAGE);
        nonExisting.setPublished(true);

        try {
            // When
            portalPageContextRepository.update(nonExisting);
            fail("Updating a non-existing PortalPageContext should throw an exception");
        } catch (IllegalStateException | TechnicalException ise) {
            // Then
            success("Test passed with one of expected exceptions thrown");
        } catch (Exception e) {
            fail("Unexpected exception type thrown: " + e.getClass().getName());
        }
    }

    @Test
    public void should_not_be_able_to_create_portal_page_with_same_page_id_and_context_and_environment_id() throws Exception {
        // Given
        PortalPageContext existing = portalPageContextRepository.findById("test-portal-page-context-id").orElse(null);
        assertNotNull("Existing PortalPageContext should be found", existing);

        PortalPageContext duplicate = PortalPageContext
            .builder()
            .id("new-id-for-duplicate")
            .pageId(existing.getPageId())
            .contextType(existing.getContextType())
            .environmentId(existing.getEnvironmentId())
            .published(true)
            .build();

        // When & Then
        try {
            portalPageContextRepository.create(duplicate);
            fail("Creating a PortalPageContext with same pageId, context and environmentId should fail due to unique constraint violation");
        } catch (Exception e) {
            // Verify it's a constraint violation exception
            String errorMessage = e.getMessage().toLowerCase();
            assertTrue(
                "Exception should indicate constraint violation",
                errorMessage.contains("constraint") ||
                errorMessage.contains("duplicate") ||
                errorMessage.contains("unique") ||
                errorMessage.contains("duplicate key") ||
                errorMessage.contains("already exists") ||
                errorMessage.contains("failed to create")
            );

            // Verify the duplicate was not actually created
            Optional<PortalPageContext> createdDuplicate = portalPageContextRepository.findById("new-id-for-duplicate");
            assertTrue("Duplicate should not be created", createdDuplicate.isEmpty());
        }
    }
}
