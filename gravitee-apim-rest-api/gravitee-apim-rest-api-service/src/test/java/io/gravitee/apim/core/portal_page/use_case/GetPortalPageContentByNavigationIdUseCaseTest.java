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

import static fixtures.core.model.PortalNavigationItemFixtures.ENV_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.PAGE11_CONTENT_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.PAGE11_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalNavigationItemFixtures;
import fixtures.core.model.PortalPageContentFixtures;
import inmemory.ApiQueryServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import io.gravitee.apim.core.membership.domain_service.ApiPortalMembershipDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiVisibilityDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationEnclosingApiDomainService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.PageContentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.exception.PortalPageContentTemplateException;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.service_provider.PortalNavigationTemplatingService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetPortalPageContentByNavigationIdUseCaseTest {

    private static final String ENVIRONMENT_ID = ENV_ID;
    private static final String UNPUBLISHED_ID = PortalNavigationItemFixtures.UNPUBLISHED_ID;
    private static final String PRIVATE_ID = PortalNavigationItemFixtures.PRIVATE_ID;

    private GetPortalPageContentByNavigationIdUseCase useCase;
    private PortalPageContentQueryServiceInMemory pageContentQueryService;
    private PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService;

    @Mock
    private PortalNavigationTemplatingService portalNavigationTemplatingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        navigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory();
        pageContentQueryService = new PortalPageContentQueryServiceInMemory();
        var apiVisibilityDomainService = new PortalNavigationApiVisibilityDomainService(
            navigationItemsQueryService,
            new ApiPortalMembershipDomainService(
                new MembershipQueryServiceInMemory(),
                new SubscriptionQueryServiceInMemory(),
                new ApiQueryServiceInMemory()
            )
        );
        var enclosingApiDomainService = new PortalNavigationEnclosingApiDomainService(navigationItemsQueryService);
        when(portalNavigationTemplatingService.renderGraviteeMarkdown(any())).thenAnswer(invocation -> {
            var in = (PortalNavigationTemplatingService.RenderPortalNavigationMarkdownInput) invocation.getArgument(0);
            return in.rawMarkdown();
        });
        useCase = new GetPortalPageContentByNavigationIdUseCase(
            navigationItemsQueryService,
            pageContentQueryService,
            apiVisibilityDomainService,
            enclosingApiDomainService,
            portalNavigationTemplatingService
        );

        clearInvocations(portalNavigationTemplatingService);
        var supportContentId = PortalPageContentId.of(PortalNavigationItemFixtures.SUPPORT_CONTENT_ID);
        var page11ContentId = PortalPageContentId.of(PortalNavigationItemFixtures.PAGE11_CONTENT_ID);

        var supportContent = PortalPageContentFixtures.aGraviteeMarkdownPageContent(
            supportContentId,
            PortalPageContentFixtures.ORGANIZATION_ID,
            ENVIRONMENT_ID,
            "Support page content"
        );

        var page11Content = PortalPageContentFixtures.aGraviteeMarkdownPageContent(
            page11ContentId,
            PortalPageContentFixtures.ORGANIZATION_ID,
            ENVIRONMENT_ID,
            "Page 11 content"
        );

        pageContentQueryService.initWith(List.of(supportContent, page11Content));
        navigationItemsQueryService.initWith(PortalNavigationItemFixtures.navigationItemsForContentTest());
    }

    @Test
    void should_return_portal_page_content_when_navigation_page_found() {
        // Given
        var input = new GetPortalPageContentByNavigationIdUseCase.Input(
            PAGE11_ID,
            ENVIRONMENT_ID,
            PortalNavigationItemViewerContext.forConsole()
        );

        // When
        var output = useCase.execute(input);

        // Then
        assertThat(output.portalPageContent()).isNotNull();
        assertThat(output.portalPageContent()).isInstanceOf(GraviteeMarkdownPageContent.class);
        assertThat(output.portalNavigationItem()).isNotNull();
        assertThat(output.portalNavigationItem()).isInstanceOf(PortalNavigationPage.class);
        assertThat(output.portalNavigationItem().getId().toString()).isEqualTo(PAGE11_ID);
        assertThat(((GraviteeMarkdownPageContent) output.portalPageContent()).getContent().value()).isEqualTo("Page 11 content");
    }

    @Test
    void should_apply_portal_navigation_templating_to_gravitee_markdown() {
        doReturn("templated").when(portalNavigationTemplatingService).renderGraviteeMarkdown(any());

        var input = new GetPortalPageContentByNavigationIdUseCase.Input(
            PAGE11_ID,
            ENVIRONMENT_ID,
            PortalNavigationItemViewerContext.forConsole()
        );

        var output = useCase.execute(input);

        assertThat(((GraviteeMarkdownPageContent) output.portalPageContent()).getContent().value()).isEqualTo("templated");
        assertThat(
            ((GraviteeMarkdownPageContent) pageContentQueryService
                    .findById(PortalPageContentId.of(PAGE11_CONTENT_ID))
                    .orElseThrow()).getContent().value()
        ).isEqualTo("Page 11 content");
        var captor = ArgumentCaptor.forClass(PortalNavigationTemplatingService.RenderPortalNavigationMarkdownInput.class);
        verify(portalNavigationTemplatingService).renderGraviteeMarkdown(captor.capture());
        assertThat(captor.getValue().rawMarkdown()).isEqualTo("Page 11 content");
        assertThat(captor.getValue().templateKey()).isEqualTo(PAGE11_CONTENT_ID);
        assertThat(captor.getValue().organizationId()).isEqualTo(PortalNavigationItemFixtures.ORG_ID);
        assertThat(captor.getValue().environmentId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(captor.getValue().enclosingApiId()).isEmpty();
    }

    @Test
    void should_leave_original_markdown_when_template_rendering_fails() {
        doThrow(new PortalPageContentTemplateException("Invalid expression or value is missing"))
            .when(portalNavigationTemplatingService)
            .renderGraviteeMarkdown(any());

        var input = new GetPortalPageContentByNavigationIdUseCase.Input(
            PAGE11_ID,
            ENVIRONMENT_ID,
            PortalNavigationItemViewerContext.forConsole()
        );

        var output = useCase.execute(input);

        assertThat(((GraviteeMarkdownPageContent) output.portalPageContent()).getContent().value()).isEqualTo("Page 11 content");
    }

    @Test
    void should_pass_enclosing_api_id_when_page_is_under_api_branch() {
        var apiNav = PortalNavigationItemFixtures.anApi(PortalNavigationItemFixtures.API1_ID, "API One", null, "api-technical-id");
        apiNav.markAsRoot();
        var folderUnderApi = PortalNavigationItemFixtures.aFolder(PortalNavigationItemFixtures.API1_FOLDER_ID, "Docs", apiNav.getId());
        folderUnderApi.updateParent(apiNav);
        var contentId = PortalPageContentId.random();
        var pageId = "00000000-0000-0000-0000-000000000097";
        var pageUnderFolder = PortalNavigationItemFixtures.aPage(pageId, "API doc page", folderUnderApi.getId(), contentId);
        pageUnderFolder.updateParent(folderUnderApi);

        var md = PortalPageContentFixtures.aGraviteeMarkdownPageContent(
            contentId,
            PortalPageContentFixtures.ORGANIZATION_ID,
            ENVIRONMENT_ID,
            "Hello"
        );
        pageContentQueryService.initWith(List.of(md));
        navigationItemsQueryService.initWith(List.of(apiNav, folderUnderApi, pageUnderFolder));

        useCase.execute(
            new GetPortalPageContentByNavigationIdUseCase.Input(pageId, ENVIRONMENT_ID, PortalNavigationItemViewerContext.forConsole())
        );

        var captor = ArgumentCaptor.forClass(PortalNavigationTemplatingService.RenderPortalNavigationMarkdownInput.class);
        verify(portalNavigationTemplatingService).renderGraviteeMarkdown(captor.capture());
        assertThat(captor.getValue().enclosingApiId()).contains("api-technical-id");
    }

    @Test
    void should_throw_when_navigation_item_not_found() {
        // Given
        var unknownId = "00000000-0000-0000-0000-000000000099";
        var input = new GetPortalPageContentByNavigationIdUseCase.Input(
            unknownId,
            ENVIRONMENT_ID,
            PortalNavigationItemViewerContext.forConsole()
        );

        // When & Then
        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(PortalNavigationItemNotFoundException.class)
            .hasMessage("Portal navigation item not found");
    }

    @Test
    void should_throw_when_navigation_item_is_not_a_page() {
        // Given
        var input = new GetPortalPageContentByNavigationIdUseCase.Input(
            PortalNavigationItemFixtures.APIS_ID,
            ENVIRONMENT_ID,
            PortalNavigationItemViewerContext.forConsole()
        );

        // When & Then
        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(InvalidPortalNavigationItemDataException.class)
            .hasMessageContaining("Navigation item type cannot be changed or is mismatched");
    }

    @Test
    void should_throw_when_page_content_not_found() {
        // Given
        pageContentQueryService.initWith(List.of());

        var input = new GetPortalPageContentByNavigationIdUseCase.Input(
            PAGE11_ID,
            ENVIRONMENT_ID,
            PortalNavigationItemViewerContext.forConsole()
        );

        // When & Then
        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(PageContentNotFoundException.class)
            .hasMessage("Page content not found");
    }

    @Test
    void should_throw_when_navigation_item_exists_in_different_environment() {
        // Given
        var input = new GetPortalPageContentByNavigationIdUseCase.Input(
            PAGE11_ID,
            "different-env",
            PortalNavigationItemViewerContext.forConsole()
        );

        // When & Then
        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(PortalNavigationItemNotFoundException.class)
            .hasMessage("Portal navigation item not found");
    }

    @Test
    void should_throw_when_navigation_item_not_visible_in_portal() {
        // Given
        var input = new GetPortalPageContentByNavigationIdUseCase.Input(
            UNPUBLISHED_ID,
            ENVIRONMENT_ID,
            PortalNavigationItemViewerContext.forPortal(true)
        );

        // When & Then
        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(PortalNavigationItemNotFoundException.class)
            .hasMessage("Portal navigation item not found");
    }

    @Test
    void should_throw_when_navigation_item_private_and_user_not_authenticated() {
        // Given
        var input = new GetPortalPageContentByNavigationIdUseCase.Input(
            PRIVATE_ID,
            ENVIRONMENT_ID,
            PortalNavigationItemViewerContext.forPortal(false)
        );

        // When & Then
        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(PortalNavigationItemNotFoundException.class)
            .hasMessage("Portal navigation item not found");
    }
}
