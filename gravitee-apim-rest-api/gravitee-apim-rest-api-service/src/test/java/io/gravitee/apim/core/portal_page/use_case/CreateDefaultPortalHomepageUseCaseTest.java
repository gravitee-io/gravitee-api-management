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
package io.gravitee.apim.core.portal_page.use_case;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.PortalPageContextCrudServiceInMemory;
import inmemory.PortalPageCrudServiceInMemory;
import io.gravitee.apim.core.portal_page.domain_service.PortalPagesDomainService;
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.repository.management.model.PortalPageContext;
import io.gravitee.repository.management.model.PortalPageContextType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateDefaultPortalHomepageUseCaseTest {

    private final PortalPageContextCrudServiceInMemory portalPageContextCrudService = new PortalPageContextCrudServiceInMemory();
    private final PortalPageCrudServiceInMemory portalPageCrudService = new PortalPageCrudServiceInMemory();
    private final PortalPagesDomainService portalPagesDomainService = new PortalPagesDomainService(
        portalPageCrudService,
        portalPageContextCrudService
    );
    private final CreateDefaultPortalHomepageUseCase useCase = new CreateDefaultPortalHomepageUseCase(
        portalPageContextCrudService,
        portalPagesDomainService
    );

    @BeforeEach
    void setUp() {
        portalPageCrudService.reset();
        portalPageContextCrudService.initWith(new ArrayList<>());
    }

    @Test
    void should_create_default_homepage_when_none_exists() {
        // Arrange
        String environmentId = "test-environment";

        // Act
        useCase.execute(environmentId);

        // Assert
        assertThat(portalPageCrudService.storage()).hasSize(1);
        assertThat(portalPageCrudService.storage().getFirst().getPageContent()).isNotNull();
        assertThat(portalPageCrudService.storage().getFirst().getPageContent().content()).isNotEmpty();

        assertThat(portalPageContextCrudService.storage()).hasSize(1);
        PortalPageContext context = portalPageContextCrudService.storage().getFirst();
        assertThat(context.getContextType()).isEqualTo(PortalPageContextType.HOMEPAGE);
        assertThat(context.isPublished()).isTrue();
        assertThat(context.getEnvironmentId()).isEqualTo(environmentId);
    }

    @Test
    void should_not_create_default_homepage_when_already_exists() {
        // Arrange
        String environmentId = "test-environment";
        PageId existingPageId = PageId.random();
        PortalPageContext existingContext = PortalPageContext.builder()
            .pageId(existingPageId.toString())
            .contextType(PortalPageContextType.HOMEPAGE)
            .environmentId(environmentId)
            .published(true)
            .build();
        portalPageContextCrudService.initWith(new ArrayList<>(List.of(existingContext)));

        // Act
        useCase.execute(environmentId);

        // Assert
        assertThat(portalPageCrudService.storage()).isEmpty();
        assertThat(portalPageContextCrudService.storage()).hasSize(1);
    }

    @Test
    void should_throw_exception_when_default_template_cannot_be_loaded() {
        // Arrange
        String environmentId = "test-environment";

        CreateDefaultPortalHomepageUseCase spiedUseCase = new CreateDefaultPortalHomepageUseCase(
            portalPageContextCrudService,
            portalPagesDomainService
        ) {
            @Override
            protected String loadDefaultPortalPageContent() {
                throw new IllegalStateException("Could not load default portal page template");
            }
        };

        try {
            spiedUseCase.execute(environmentId);
            assertThat(false).as("Should have thrown IllegalStateException").isTrue();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).contains("Could not load default portal page template");
        }

        assertThat(portalPageCrudService.storage()).isEmpty();
        assertThat(portalPageContextCrudService.storage()).isEmpty();
    }

    @Test
    void should_load_default_portal_page_content_from_template() {
        // Act
        String content = useCase.loadDefaultPortalPageContent();

        // Assert
        assertThat(content).isNotNull();
        assertThat(content).isNotEmpty();
    }
}
