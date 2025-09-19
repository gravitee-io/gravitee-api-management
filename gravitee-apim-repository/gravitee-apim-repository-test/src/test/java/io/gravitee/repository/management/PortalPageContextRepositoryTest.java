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
import static org.assertj.core.api.Assertions.catchThrowable;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.PortalPageContext;
import io.gravitee.repository.management.model.PortalPageContextType;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
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

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(portalPageContext.getId());
        assertThat(created.getPageId()).isEqualTo(portalPageContext.getPageId());
        assertThat(created.getEnvironmentId()).isEqualTo(portalPageContext.getEnvironmentId());
        assertThat(created.getContextType()).isEqualTo(portalPageContext.getContextType());
        assertThat(created.isPublished()).isEqualTo(portalPageContext.isPublished());
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
        assertThat(foundContexts).isNotNull();
        assertThat(foundContexts).hasSize(2);
        assertThat(foundContexts).allMatch(c -> c.getContextType() == PortalPageContextType.HOMEPAGE);
        assertThat(foundContexts).anyMatch(c -> c.getPageId().equals("test-portal-page-id"));
        assertThat(foundContexts).anyMatch(c -> c.getPageId().equals("update-portal-page"));
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

        Throwable thrown = catchThrowable(() -> portalPageContextRepository.update(nonExisting));
        assertThat(thrown).isInstanceOfAny(IllegalStateException.class, TechnicalException.class);
    }

    @Test
    public void should_not_be_able_to_create_portal_page_with_same_page_id_and_context_and_environment_id() throws Exception {
        // Given
        PortalPageContext existing = portalPageContextRepository.findById("test-portal-page-context-id").orElse(null);
        assertThat(existing).isNotNull();

        PortalPageContext duplicate = PortalPageContext.builder()
            .id("new-id-for-duplicate")
            .pageId(existing.getPageId())
            .contextType(existing.getContextType())
            .environmentId(existing.getEnvironmentId())
            .published(true)
            .build();

        Throwable thrown = catchThrowable(() -> portalPageContextRepository.create(duplicate));
        assertThat(thrown).isNotNull();
        String errorMessage = thrown.getMessage().toLowerCase();
        assertThat(errorMessage).containsAnyOf("constraint", "duplicate", "unique", "duplicate key", "already exists", "failed to create");

        Optional<PortalPageContext> createdDuplicate = portalPageContextRepository.findById("new-id-for-duplicate");
        assertThat(createdDuplicate).isEmpty();
    }

    @Test
    public void should_find_by_page_id() throws Exception {
        String pageId = "test-portal-page-id";

        PortalPageContext foundContext = portalPageContextRepository.findByPageId(pageId);

        assertThat(foundContext).isNotNull();
        assertThat(foundContext.getPageId()).isEqualTo(pageId);
        assertThat(foundContext.getId()).isEqualTo("test-portal-page-context-id");
    }
}
