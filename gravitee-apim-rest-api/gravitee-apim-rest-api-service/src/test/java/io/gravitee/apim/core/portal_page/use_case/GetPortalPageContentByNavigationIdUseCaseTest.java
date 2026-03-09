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
import static fixtures.core.model.PortalNavigationItemFixtures.PAGE11_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.PortalNavigationItemFixtures;
import fixtures.core.model.PortalPageContentFixtures;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.PageContentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetPortalPageContentByNavigationIdUseCaseTest {

    private static final String ENVIRONMENT_ID = ENV_ID;
    private static final String UNPUBLISHED_ID = PortalNavigationItemFixtures.UNPUBLISHED_ID;
    private static final String PRIVATE_ID = PortalNavigationItemFixtures.PRIVATE_ID;

    private GetPortalPageContentByNavigationIdUseCase useCase;
    private PortalPageContentQueryServiceInMemory pageContentQueryService;

    @BeforeEach
    void setUp() {
        PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory();
        pageContentQueryService = new PortalPageContentQueryServiceInMemory();
        useCase = new GetPortalPageContentByNavigationIdUseCase(navigationItemsQueryService, pageContentQueryService);

        // Create page contents first with known IDs
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
