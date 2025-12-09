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
package io.gravitee.rest.api.portal.rest.resource;

import static fixtures.core.model.PortalPageContentFixtures.ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalPageContentFixtures;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.rest.api.portal.rest.fixture.PortalNavigationFixtures;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class PortalNavigationItemResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "DEFAULT";

    @Autowired
    private PortalNavigationItemsQueryServiceInMemory portalNavigationItemsQueryService;

    @Autowired
    private PortalPageContentQueryServiceInMemory portalPageContentQueryService;

    @Override
    protected String contextPath() {
        return "portal-navigation-items/";
    }

    @BeforeEach
    public void init() {
        GraviteeContext.setCurrentEnvironment(ENV_ID);
    }

    @AfterEach
    public void tearDown() {
        GraviteeContext.cleanContext();
        portalNavigationItemsQueryService.reset();
        portalPageContentQueryService.reset();
    }

    @Nested
    class GetPortalNavigationItemById {

        @Test
        void should_return_portal_navigation_item_when_found_and_published() {
            // Given
            var itemId = PortalNavigationFixtures.randomNavigationId();
            var publishedItem = PortalNavigationFixtures.page(
                itemId,
                "Published Page",
                PortalArea.TOP_NAVBAR,
                PortalNavigationFixtures.randomPageId()
            );
            publishedItem.setPublished(true);
            publishedItem.setEnvironmentId(ENV_ID);

            portalNavigationItemsQueryService.initWith(List.of(publishedItem));

            // When
            Response response = target(itemId.toString()).request().get();

            // Then
            assertThat(response.getStatus()).isEqualTo(200);
            var result = response.readEntity(io.gravitee.rest.api.portal.rest.model.PortalNavigationItem.class);
            assertThat(result).isNotNull();
            assertThat(result.getActualInstance()).isNotNull();
        }

        @Test
        void should_return_portal_navigation_item_when_user_is_authenticated() {
            var itemId = PortalNavigationFixtures.randomNavigationId();
            var publishedAndPublicItem = PortalNavigationFixtures.page(
                itemId,
                "Published Public Page",
                PortalArea.TOP_NAVBAR,
                PortalNavigationFixtures.randomPageId()
            );
            publishedAndPublicItem.setPublished(true);
            publishedAndPublicItem.setVisibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PUBLIC);
            publishedAndPublicItem.setEnvironmentId(ENV_ID);

            portalNavigationItemsQueryService.initWith(List.of(publishedAndPublicItem));

            // When
            Response response = target(itemId.toString()).request().get();

            // Then
            assertThat(response.getStatus()).isEqualTo(200);
            var result = response.readEntity(io.gravitee.rest.api.portal.rest.model.PortalNavigationItem.class);
            assertThat(result).isNotNull();
            assertThat(result.getActualInstance()).isNotNull();
        }

        @Test
        void should_return_404_when_item_not_found() {
            // Given
            var unknownId = PortalNavigationFixtures.randomNavigationId();
            portalNavigationItemsQueryService.initWith(List.of());

            // When
            Response response = target(unknownId.toString()).request().get();

            // Then
            assertThat(response.getStatus()).isEqualTo(404);
        }

        @Test
        void should_return_404_when_item_is_unpublished() {
            // Given
            var itemId = PortalNavigationFixtures.randomNavigationId();
            var unpublishedItem = PortalNavigationFixtures.page(
                itemId,
                "Unpublished Page",
                PortalArea.TOP_NAVBAR,
                PortalNavigationFixtures.randomPageId()
            );
            unpublishedItem.setPublished(false);
            unpublishedItem.setEnvironmentId(ENV_ID);

            portalNavigationItemsQueryService.initWith(List.of(unpublishedItem));

            // When
            Response response = target(itemId.toString()).request().get();

            // Then
            assertThat(response.getStatus()).isEqualTo(404);
        }
    }

    @Nested
    class GetPortalNavigationItemContentById {

        @Test
        void should_return_content_when_page_found_and_published() {
            // Given
            var itemId = PortalNavigationFixtures.randomNavigationId();
            var contentId = PortalNavigationFixtures.randomPageId();
            var pageContent = PortalPageContentFixtures.aGraviteeMarkdownPageContent(
                contentId,
                ORGANIZATION_ID,
                ENV_ID,
                "Page content text"
            );

            var publishedPage = PortalNavigationFixtures.page(itemId, "Published Page", PortalArea.TOP_NAVBAR, contentId);
            publishedPage.setPublished(true);
            publishedPage.setEnvironmentId(ENV_ID);

            portalNavigationItemsQueryService.initWith(List.of(publishedPage));
            portalPageContentQueryService.initWith(List.of(pageContent));

            // When
            Response response = target(itemId.toString()).path("content").request().get();

            // Then
            assertThat(response.getStatus()).isEqualTo(200);
            var content = response.readEntity(String.class);
            assertThat(content).isEqualTo("Page content text");
        }

        @Test
        void should_return_content_when_user_is_authenticated() {
            // Given
            var itemId = PortalNavigationFixtures.randomNavigationId();
            var contentId = PortalNavigationFixtures.randomPageId();
            var pageContent = PortalPageContentFixtures.aGraviteeMarkdownPageContent(
                contentId,
                ORGANIZATION_ID,
                ENV_ID,
                "Page content text"
            );

            var publishedAndPublicPage = PortalNavigationFixtures.page(itemId, "Published Public Page", PortalArea.TOP_NAVBAR, contentId);
            publishedAndPublicPage.setPublished(true);
            publishedAndPublicPage.setVisibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PUBLIC);
            publishedAndPublicPage.setEnvironmentId(ENV_ID);

            portalNavigationItemsQueryService.initWith(List.of(publishedAndPublicPage));
            portalPageContentQueryService.initWith(List.of(pageContent));

            // When
            Response response = target(itemId.toString()).path("content").request().get();

            // Then
            assertThat(response.getStatus()).isEqualTo(200);
            var content = response.readEntity(String.class);
            assertThat(content).isEqualTo("Page content text");
        }

        @Test
        void should_return_404_when_item_not_found() {
            // Given
            var unknownId = PortalNavigationFixtures.randomNavigationId();
            portalNavigationItemsQueryService.initWith(List.of());

            // When
            Response response = target(unknownId.toString()).path("content").request().get();

            // Then
            assertThat(response.getStatus()).isEqualTo(404);
        }

        @Test
        void should_return_404_when_item_is_unpublished() {
            // Given
            var itemId = PortalNavigationFixtures.randomNavigationId();
            var contentId = PortalNavigationFixtures.randomPageId();
            var pageContent = PortalPageContentFixtures.aGraviteeMarkdownPageContent(
                contentId,
                ORGANIZATION_ID,
                ENV_ID,
                "Page content text"
            );

            var unpublishedPage = PortalNavigationFixtures.page(itemId, "Unpublished Page", PortalArea.TOP_NAVBAR, contentId);
            unpublishedPage.setPublished(false);
            unpublishedPage.setEnvironmentId(ENV_ID);

            portalNavigationItemsQueryService.initWith(List.of(unpublishedPage));
            portalPageContentQueryService.initWith(List.of(pageContent));

            // When
            Response response = target(itemId.toString()).path("content").request().get();

            // Then
            assertThat(response.getStatus()).isEqualTo(404);
        }

        @Test
        void should_return_400_when_item_is_not_a_page() {
            // Given
            var folderId = PortalNavigationFixtures.randomNavigationId();
            var folder = PortalNavigationFixtures.folder(folderId, "Folder", PortalArea.TOP_NAVBAR);
            folder.setPublished(true);
            folder.setEnvironmentId(ENV_ID);

            portalNavigationItemsQueryService.initWith(List.of(folder));

            // When
            Response response = target(folderId.toString()).path("content").request().get();

            // Then
            assertThat(response.getStatus()).isEqualTo(400);
        }

        @Test
        void should_return_404_when_page_content_not_found() {
            // Given
            var itemId = PortalNavigationFixtures.randomNavigationId();
            var contentId = PortalNavigationFixtures.randomPageId();

            var publishedPage = PortalNavigationFixtures.page(itemId, "Published Page", PortalArea.TOP_NAVBAR, contentId);
            publishedPage.setPublished(true);
            publishedPage.setEnvironmentId(ENV_ID);

            portalNavigationItemsQueryService.initWith(List.of(publishedPage));
            portalPageContentQueryService.initWith(List.of()); // No content

            // When
            Response response = target(itemId.toString()).path("content").request().get();

            // Then
            assertThat(response.getStatus()).isEqualTo(404);
        }
    }
}
