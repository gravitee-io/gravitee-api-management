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

import static fixtures.core.model.PortalPageContentFixtures.CONTENT_ID;
import static fixtures.core.model.PortalPageContentFixtures.ENVIRONMENT_ID;
import static fixtures.core.model.PortalPageContentFixtures.ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.PortalPageContentFixtures;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdownValidator;
import io.gravitee.apim.core.gravitee_markdown.exception.GraviteeMarkdownContentEmptyException;
import io.gravitee.apim.core.portal_page.domain_service.PortalPageContentValidatorService;
import io.gravitee.apim.core.portal_page.exception.PageContentNotFoundException;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.UpdatePortalPageContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UpdatePortalPageContentUseCaseTest {

    private UpdatePortalPageContentUseCase useCase;

    @BeforeEach
    void setUp() {
        PortalPageContentQueryServiceInMemory queryService = new PortalPageContentQueryServiceInMemory();
        PortalPageContentCrudServiceInMemory crudService = new PortalPageContentCrudServiceInMemory();
        GraviteeMarkdownValidator gmdValidator = new GraviteeMarkdownValidator();
        PortalPageContentValidatorService validatorService = new PortalPageContentValidatorService(gmdValidator);
        useCase = new UpdatePortalPageContentUseCase(queryService, validatorService, crudService);

        queryService.initWith(PortalPageContentFixtures.samplePortalPageContents());
        crudService.initWith(PortalPageContentFixtures.samplePortalPageContents());
    }

    @Test
    void should_update_content_when_valid() {
        // Given
        final var updateContent = UpdatePortalPageContent.builder().content("Updated content").build();
        final var input = UpdatePortalPageContentUseCase.Input.builder()
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .portalPageContentId(CONTENT_ID)
            .updatePortalPageContent(updateContent)
            .build();

        // When
        final var output = useCase.execute(input);

        // Then
        assertThat(output.portalPageContent()).isInstanceOf(GraviteeMarkdownPageContent.class);
        final var updatedContent = (GraviteeMarkdownPageContent) output.portalPageContent();
        assertThat(updatedContent.getContent()).isEqualTo("Updated content");
        assertThat(updatedContent.getId()).isEqualTo(PortalPageContentId.of(CONTENT_ID));
    }

    @Test
    void should_throw_when_content_not_found() {
        // Given
        final var updateContent = UpdatePortalPageContent.builder().content("Updated content").build();
        final var input = UpdatePortalPageContentUseCase.Input.builder()
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .portalPageContentId("00000000-0000-0000-0000-000000000002")
            .updatePortalPageContent(updateContent)
            .build();

        // When & Then
        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(PageContentNotFoundException.class)
            .hasMessage("Page content not found");
    }

    @Test
    void should_throw_when_organization_id_mismatch() {
        // Given
        final var updateContent = UpdatePortalPageContent.builder().content("Updated content").build();
        final var input = UpdatePortalPageContentUseCase.Input.builder()
            .organizationId("different-org")
            .environmentId(ENVIRONMENT_ID)
            .portalPageContentId(CONTENT_ID)
            .updatePortalPageContent(updateContent)
            .build();

        // When & Then
        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(PageContentNotFoundException.class)
            .hasMessage("Page content not found");
    }

    @Test
    void should_throw_when_environment_id_mismatch() {
        // Given
        final var updateContent = UpdatePortalPageContent.builder().content("Updated content").build();
        final var input = UpdatePortalPageContentUseCase.Input.builder()
            .organizationId(ORGANIZATION_ID)
            .environmentId("different-env")
            .portalPageContentId(CONTENT_ID)
            .updatePortalPageContent(updateContent)
            .build();

        // When & Then
        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(PageContentNotFoundException.class)
            .hasMessage("Page content not found");
    }

    @Test
    void should_throw_when_content_is_empty() {
        // Given
        final var updateContent = UpdatePortalPageContent.builder().content("").build();
        final var input = UpdatePortalPageContentUseCase.Input.builder()
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .portalPageContentId(CONTENT_ID)
            .updatePortalPageContent(updateContent)
            .build();

        // When & Then
        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(GraviteeMarkdownContentEmptyException.class)
            .hasMessage("Content must not be null or empty");
    }
}
