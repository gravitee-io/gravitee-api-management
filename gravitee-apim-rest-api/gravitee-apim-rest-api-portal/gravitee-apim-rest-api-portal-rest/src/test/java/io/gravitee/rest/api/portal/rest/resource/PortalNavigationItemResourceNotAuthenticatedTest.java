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

import fixtures.core.model.PortalPageContentFixtures;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.rest.api.portal.rest.fixture.PortalNavigationFixtures;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class PortalNavigationItemResourceNotAuthenticatedTest extends AbstractResourceTest {

    private static final String ENV_ID = "DEFAULT";

    @Autowired
    private PortalNavigationItemsQueryServiceInMemory portalNavigationItemsQueryService;

    @Autowired
    private PortalPageContentQueryServiceInMemory portalPageContentQueryService;

    @Override
    protected String contextPath() {
        return "portal-navigation-items/";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(NotAuthenticatedAuthenticationFilter.class);
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
        void should_return_portal_navigation_item_when_user_is_not_authenticated() {
            // Given
            var publishedAndPublicItemId = PortalNavigationFixtures.randomNavigationId();
            var publishedAndPublicItem = PortalNavigationFixtures.page(
                publishedAndPublicItemId,
                "Published Public Page",
                PortalArea.TOP_NAVBAR,
                PortalNavigationFixtures.randomPageId()
            );
            publishedAndPublicItem.setPublished(true);
            publishedAndPublicItem.setVisibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PUBLIC);
            publishedAndPublicItem.setEnvironmentId(ENV_ID);

            portalNavigationItemsQueryService.initWith(List.of(publishedAndPublicItem));

            // When
            Response response = target(publishedAndPublicItemId.toString()).request().get();

            // Then
            assertThat(response.getStatus()).isEqualTo(200);
            var result = response.readEntity(io.gravitee.rest.api.portal.rest.model.PortalNavigationItem.class);
            assertThat(result).isNotNull();
            assertThat(result.getActualInstance()).isNotNull();
        }

        @Test
        void should_throw_error_for_private_page_when_user_is_not_authenticated() {
            // Given
            var privateItemId = PortalNavigationFixtures.randomNavigationId();
            var privateItem = PortalNavigationFixtures.page(
                privateItemId,
                "Private Page",
                PortalArea.TOP_NAVBAR,
                PortalNavigationFixtures.randomPageId()
            );
            privateItem.setPublished(true);
            privateItem.setVisibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PRIVATE);
            privateItem.setEnvironmentId(ENV_ID);

            portalNavigationItemsQueryService.initWith(List.of(privateItem));

            // When
            Response response = target(privateItemId.toString()).request().get();

            // Then
            assertThat(response.getStatus()).isEqualTo(404);
        }
    }

    @Nested
    class GetPortalNavigationItemContentById {

        @Test
        void should_return_content_when_user_is_not_authenticated() {
            // Given
            var publishedAndPublicItemId = PortalNavigationFixtures.randomNavigationId();
            var contentId = PortalNavigationFixtures.randomPageId();
            var pageContent = PortalPageContentFixtures.aGraviteeMarkdownPageContent(
                contentId,
                ORGANIZATION_ID,
                ENV_ID,
                "Page content text"
            );

            var publishedAndPublicPage = PortalNavigationFixtures.page(
                publishedAndPublicItemId,
                "Published Public Page",
                PortalArea.TOP_NAVBAR,
                contentId
            );
            publishedAndPublicPage.setPublished(true);
            publishedAndPublicPage.setVisibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PUBLIC);
            publishedAndPublicPage.setEnvironmentId(ENV_ID);

            portalNavigationItemsQueryService.initWith(List.of(publishedAndPublicPage));
            portalPageContentQueryService.initWith(List.of(pageContent));

            // When
            Response response = target(publishedAndPublicItemId.toString()).path("content").request().get();

            // Then
            assertThat(response.getStatus()).isEqualTo(200);
            var content = response.readEntity(String.class);
            assertThat(content).isEqualTo("Page content text");
        }

        @Test
        void should_throw_error_when_page_private_and_user_is_not_authenticated() {
            // Given
            var privateItemId = PortalNavigationFixtures.randomNavigationId();
            var contentId = PortalNavigationFixtures.randomPageId();
            var pageContent = PortalPageContentFixtures.aGraviteeMarkdownPageContent(
                contentId,
                ORGANIZATION_ID,
                ENV_ID,
                "Private Page content text"
            );

            var privatePage = PortalNavigationFixtures.page(privateItemId, "Private Page", PortalArea.TOP_NAVBAR, contentId);
            privatePage.setPublished(true);
            privatePage.setVisibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PRIVATE);
            privatePage.setEnvironmentId(ENV_ID);

            portalNavigationItemsQueryService.initWith(List.of(privatePage));
            portalPageContentQueryService.initWith(List.of(pageContent));

            // When
            Response response = target(privateItemId.toString()).path("content").request().get();

            // Then
            assertThat(response.getStatus()).isEqualTo(404);
        }
    }
}
