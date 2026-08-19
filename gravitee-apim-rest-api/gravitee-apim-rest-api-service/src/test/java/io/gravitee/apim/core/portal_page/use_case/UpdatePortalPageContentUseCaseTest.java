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
import static fixtures.core.model.SwaggerUiConfigurationFixtures.aSwaggerUiConfiguration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalPageContentFixtures;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.api.service_provider.ApiTemplateModelProvider;
import io.gravitee.apim.core.environment.service_provider.EnvironmentTemplateModelProvider;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdownValidator;
import io.gravitee.apim.core.gravitee_markdown.exception.GraviteeMarkdownContentEmptyException;
import io.gravitee.apim.core.portal_page.domain_service.GraviteePortalPageContentValidatorService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationEnclosingApiDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationSourcedItemsDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalPageContentValidatorService;
import io.gravitee.apim.core.portal_page.exception.PageContentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.PortalPageContentTooLargeException;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.OpenApiPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.model.SwaggerUiConfiguration;
import io.gravitee.apim.core.portal_page.model.UpdatePortalPageContent;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.service_provider.PortalNavigationTemplatingService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UpdatePortalPageContentUseCaseTest {

    private UpdatePortalPageContentUseCase useCase;
    private PortalPageContentQueryServiceInMemory queryService;
    private PortalPageContentCrudServiceInMemory crudService;
    private PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService;

    @BeforeEach
    void setUp() {
        queryService = new PortalPageContentQueryServiceInMemory();
        crudService = new PortalPageContentCrudServiceInMemory();
        navigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory(new java.util.ArrayList<>());

        GraviteeMarkdownValidator gmdValidator = new GraviteeMarkdownValidator();
        PortalNavigationItemsQueryService portalNavigationItemsQueryService = mock(PortalNavigationItemsQueryService.class);
        when(portalNavigationItemsQueryService.search(any())).thenReturn(java.util.List.of());
        GraviteePortalPageContentValidatorService gmdContentValidator = new GraviteePortalPageContentValidatorService(
            gmdValidator,
            portalNavigationItemsQueryService,
            mock(PortalNavigationEnclosingApiDomainService.class),
            mock(PortalNavigationTemplatingService.class),
            mock(ApiTemplateModelProvider.class),
            mock(EnvironmentTemplateModelProvider.class)
        );
        PortalPageContentValidatorService validatorService = new PortalPageContentValidatorService(java.util.List.of(gmdContentValidator));

        useCase = new UpdatePortalPageContentUseCase(
            queryService,
            crudService,
            validatorService,
            navigationItemsQueryService,
            new PortalNavigationSourcedItemsDomainService(navigationItemsQueryService)
        );

        queryService.initWith(PortalPageContentFixtures.samplePortalPageContents());
        crudService.initWith(PortalPageContentFixtures.samplePortalPageContents());
    }

    @Test
    void should_reject_content_edit_when_page_is_managed_by_a_source() {
        var sourcedPage = io.gravitee.apim.core.portal_page.model.PortalNavigationPage.builder()
            .id(io.gravitee.apim.core.portal_page.model.PortalNavigationItemId.of("00000000-0000-0000-0000-00000000ff01"))
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .title("Sourced Page")
            .segment("sourced-page")
            .area(io.gravitee.apim.core.portal.model.PortalArea.TOP_NAVBAR)
            .order(0)
            .portalPageContentId(PortalPageContentId.of(CONTENT_ID))
            .published(true)
            .visibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PUBLIC)
            .source(
                io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource.builder()
                    .sourceType("http-fetcher")
                    .sourceConfiguration("{}")
                    .build()
            )
            .build();
        sourcedPage.markAsRoot();
        navigationItemsQueryService.storage().add(sourcedPage);

        final var updateContent = UpdatePortalPageContent.builder().content("Updated content").build();
        final var input = UpdatePortalPageContentUseCase.Input.builder()
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .portalPageContentId(CONTENT_ID)
            .updatePortalPageContent(updateContent)
            .build();

        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException.class)
            .hasMessageContaining("cannot be edited");
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
        assertThat(updatedContent.getContent().value()).isEqualTo("Updated content");
        assertThat(updatedContent.getId()).isEqualTo(PortalPageContentId.of(CONTENT_ID));
    }

    @Test
    void should_reject_update_when_content_exceeds_the_max_size() {
        // Given
        final var updateContent = UpdatePortalPageContent.builder().content("a".repeat(10 * 1024 * 1024 + 1)).build();
        final var input = UpdatePortalPageContentUseCase.Input.builder()
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .portalPageContentId(CONTENT_ID)
            .updatePortalPageContent(updateContent)
            .build();

        // When & Then
        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(PortalPageContentTooLargeException.class);
    }

    @Test
    void should_preserve_openapi_viewer_configuration_when_updating_content_only() {
        // Given
        final var contentId = PortalPageContentId.of("00000000-0000-0000-0000-000000000003");
        final var openApiContent = PortalPageContentFixtures.anOpenApiPageContent(
            contentId,
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            "openapi: 3.0.3\ninfo:\n  title: Initial",
            aSwaggerUiConfiguration()
        );
        queryService.initWith(List.of(openApiContent));
        crudService.initWith(List.of(openApiContent));

        final var updateContent = UpdatePortalPageContent.builder().content("openapi: 3.0.3\ninfo:\n  title: Updated").build();
        final var input = UpdatePortalPageContentUseCase.Input.builder()
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .portalPageContentId(contentId.toString())
            .updatePortalPageContent(updateContent)
            .build();

        // When
        final var output = useCase.execute(input);

        // Then
        assertThat(output.portalPageContent()).isInstanceOf(OpenApiPageContent.class);
        final var updatedContent = (OpenApiPageContent) output.portalPageContent();
        assertThat(updatedContent.getContent().value()).contains("title: Updated");
        assertThat(updatedContent.getViewerSettings()).isInstanceOf(SwaggerUiConfiguration.class);
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
    void should_replace_content_type_when_requested_type_differs() {
        // Given: CONTENT_ID holds a GRAVITEE_MARKDOWN content
        final var updateContent = UpdatePortalPageContent.builder()
            .content("openapi: 3.0.3\ninfo:\n  title: Imported")
            .type(PortalPageContentType.OPENAPI)
            .build();
        final var input = UpdatePortalPageContentUseCase.Input.builder()
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .portalPageContentId(CONTENT_ID)
            .updatePortalPageContent(updateContent)
            .build();

        // When
        final var output = useCase.execute(input);

        // Then
        assertThat(output.portalPageContent()).isInstanceOf(OpenApiPageContent.class);
        assertThat(output.portalPageContent().getId()).isEqualTo(PortalPageContentId.of(CONTENT_ID));
        final var updatedContent = (OpenApiPageContent) output.portalPageContent();
        assertThat(updatedContent.getContent().value()).contains("title: Imported");
    }

    @Test
    void should_keep_existing_type_when_requested_type_matches() {
        // Given
        final var updateContent = UpdatePortalPageContent.builder()
            .content("Updated content")
            .type(PortalPageContentType.GRAVITEE_MARKDOWN)
            .build();
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
        assertThat(((GraviteeMarkdownPageContent) output.portalPageContent()).getContent().value()).isEqualTo("Updated content");
    }

    @Test
    void should_validate_against_the_requested_type_when_type_changes() {
        // Given: an OPENAPI content updated towards GRAVITEE_MARKDOWN with empty content
        final var contentId = PortalPageContentId.of("00000000-0000-0000-0000-000000000003");
        final var openApiContent = PortalPageContentFixtures.anOpenApiPageContent(
            contentId,
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            "openapi: 3.0.3\ninfo:\n  title: Initial",
            aSwaggerUiConfiguration()
        );
        queryService.initWith(List.of(openApiContent));
        crudService.initWith(List.of(openApiContent));

        final var updateContent = UpdatePortalPageContent.builder().content("").type(PortalPageContentType.GRAVITEE_MARKDOWN).build();
        final var input = UpdatePortalPageContentUseCase.Input.builder()
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .portalPageContentId(contentId.toString())
            .updatePortalPageContent(updateContent)
            .build();

        // When & Then
        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(GraviteeMarkdownContentEmptyException.class)
            .hasMessage("Content must not be null or empty");
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
