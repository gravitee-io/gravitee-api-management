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

import static fixtures.core.model.PortalNavigationItemFixtures.PORTAL_REFERENCE;
import static fixtures.core.model.PortalPageContentFixtures.CONTENT_ID;
import static fixtures.core.model.PortalPageContentFixtures.ENVIRONMENT_ID;
import static fixtures.core.model.PortalPageContentFixtures.ORGANIZATION_ID;
import static fixtures.core.model.SwaggerUiConfigurationFixtures.aSwaggerUiConfiguration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalNavigationItemFixtures;
import fixtures.core.model.PortalPageContentFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.api.service_provider.ApiTemplateModelProvider;
import io.gravitee.apim.core.environment.service_provider.EnvironmentTemplateModelProvider;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdownValidator;
import io.gravitee.apim.core.gravitee_markdown.exception.GraviteeMarkdownContentEmptyException;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.domain_service.GraviteePortalPageContentValidatorService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationEnclosingApiDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationSourcedItemsDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalPageContentValidatorService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.PageContentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.PortalPageContentTooLargeException;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.OpenApiPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.model.SwaggerUiConfiguration;
import io.gravitee.apim.core.portal_page.model.UpdatePortalPageContent;
import io.gravitee.apim.core.portal_page.service_provider.PortalNavigationTemplatingService;
import java.util.List;
import java.util.Map;
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
        queryService = spy(new PortalPageContentQueryServiceInMemory());
        crudService = new PortalPageContentCrudServiceInMemory();
        navigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory();
        navigationItemsQueryService.initWith(List.of(pageForContent(CONTENT_ID), pageForContent("00000000-0000-0000-0000-000000000003")));

        GraviteeMarkdownValidator gmdValidator = new GraviteeMarkdownValidator();
        var environmentTemplateModelProvider = mock(EnvironmentTemplateModelProvider.class);
        when(environmentTemplateModelProvider.getEnvironmentMetadata(ENVIRONMENT_ID)).thenReturn(Map.of());
        GraviteePortalPageContentValidatorService gmdContentValidator = new GraviteePortalPageContentValidatorService(
            gmdValidator,
            navigationItemsQueryService,
            new PortalNavigationEnclosingApiDomainService(navigationItemsQueryService),
            mock(PortalNavigationTemplatingService.class),
            mock(ApiTemplateModelProvider.class),
            environmentTemplateModelProvider
        );
        PortalPageContentValidatorService validatorService = new PortalPageContentValidatorService(List.of(gmdContentValidator));

        useCase = new UpdatePortalPageContentUseCase(
            PortalNavigationUseCaseTestFixtures.access(
                ORGANIZATION_ID,
                ENVIRONMENT_ID,
                navigationItemsQueryService,
                queryService,
                new ApiCrudServiceInMemory()
            ),
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
        var sourcedPage = PortalNavigationPage.builder()
            .reference(PORTAL_REFERENCE)
            .id(PortalNavigationItemId.of("00000000-0000-0000-0000-00000000ff01"))
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .title("Sourced Page")
            .segment("sourced-page")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .portalPageContentId(PortalPageContentId.of(CONTENT_ID))
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .source(PortalNavigationItemSource.builder().sourceType("http-fetcher").sourceConfiguration("{}").build())
            .build();
        sourcedPage.markAsRoot();
        navigationItemsQueryService.initWith(List.of(sourcedPage));

        final var updateContent = UpdatePortalPageContent.builder().content("Updated content").build();
        final var input = UpdatePortalPageContentUseCase.Input.builder()
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .portalPageContentId(CONTENT_ID)
            .updatePortalPageContent(updateContent)
            .scope(PORTAL_REFERENCE)
            .build();

        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(InvalidPortalNavigationItemDataException.class)
            .hasMessageContaining("cannot be edited");
    }

    @Test
    void should_refuse_content_owned_by_another_portal_in_the_same_environment_without_writing() {
        var page = pageForContent(CONTENT_ID);
        page.changeOwner(new NavigationItemReference.PortalReference(PortalId.of("22222222-2222-2222-2222-222222222222")));
        navigationItemsQueryService.initWith(List.of(page));
        var previousContents = crudService
            .storage()
            .stream()
            .map(content -> content.getContent().toString())
            .toList();
        var input = UpdatePortalPageContentUseCase.Input.builder()
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .portalPageContentId(CONTENT_ID)
            .updatePortalPageContent(UpdatePortalPageContent.builder().content("Updated content").build())
            .scope(PORTAL_REFERENCE)
            .build();

        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(PageContentNotFoundException.class);

        assertThat(crudService.storage())
            .extracting(content -> content.getContent().toString())
            .containsExactlyElementsOf(previousContents);
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
            .scope(PORTAL_REFERENCE)
            .build();

        // When
        final var output = useCase.execute(input);

        // Then
        assertThat(output.portalPageContent()).isInstanceOf(GraviteeMarkdownPageContent.class);
        final var updatedContent = (GraviteeMarkdownPageContent) output.portalPageContent();
        assertThat(updatedContent.getContent().value()).isEqualTo("Updated content");
        assertThat(updatedContent.getId()).isEqualTo(PortalPageContentId.of(CONTENT_ID));
        verify(queryService).findById(PortalPageContentId.of(CONTENT_ID));
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
            .scope(PORTAL_REFERENCE)
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
            .scope(PORTAL_REFERENCE)
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
            .scope(PORTAL_REFERENCE)
            .build();

        // When & Then
        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(PageContentNotFoundException.class)
            .hasMessage("Page content not found");
    }

    @Test
    void should_throw_when_organization_id_mismatch() {
        initializeContentOwnership("different-org", ENVIRONMENT_ID);
        // Given
        final var updateContent = UpdatePortalPageContent.builder().content("Updated content").build();
        final var input = UpdatePortalPageContentUseCase.Input.builder()
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .portalPageContentId(CONTENT_ID)
            .updatePortalPageContent(updateContent)
            .scope(PORTAL_REFERENCE)
            .build();

        // When & Then
        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(PageContentNotFoundException.class)
            .hasMessage("Page content not found");
        assertStoredContentUnchanged();
    }

    @Test
    void should_throw_when_environment_id_mismatch() {
        initializeContentOwnership(ORGANIZATION_ID, "different-env");
        // Given
        final var updateContent = UpdatePortalPageContent.builder().content("Updated content").build();
        final var input = UpdatePortalPageContentUseCase.Input.builder()
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .portalPageContentId(CONTENT_ID)
            .updatePortalPageContent(updateContent)
            .scope(PORTAL_REFERENCE)
            .build();

        // When & Then
        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(PageContentNotFoundException.class)
            .hasMessage("Page content not found");
        assertStoredContentUnchanged();
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
            .scope(PORTAL_REFERENCE)
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
            .scope(PORTAL_REFERENCE)
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
            .scope(PORTAL_REFERENCE)
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
            .scope(PORTAL_REFERENCE)
            .build();

        // When & Then
        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(GraviteeMarkdownContentEmptyException.class)
            .hasMessage("Content must not be null or empty");
    }

    private PortalNavigationPage pageForContent(String contentId) {
        var page = PortalNavigationItemFixtures.aPage("Content page", null)
            .toBuilder()
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .portalPageContentId(PortalPageContentId.of(contentId))
            .build();
        page.markAsRoot();
        return page;
    }

    private void initializeContentOwnership(String organizationId, String environmentId) {
        var content = PortalPageContentFixtures.aGraviteeMarkdownPageContent(
            PortalPageContentId.of(CONTENT_ID),
            organizationId,
            environmentId,
            PortalPageContentFixtures.CONTENT
        );
        queryService.initWith(List.of(content));
        crudService.initWith(List.of(content));
    }

    private void assertStoredContentUnchanged() {
        var storedContent = (GraviteeMarkdownPageContent) crudService.storage().getFirst();
        assertThat(storedContent.getContent().value()).isEqualTo(PortalPageContentFixtures.CONTENT);
    }
}
