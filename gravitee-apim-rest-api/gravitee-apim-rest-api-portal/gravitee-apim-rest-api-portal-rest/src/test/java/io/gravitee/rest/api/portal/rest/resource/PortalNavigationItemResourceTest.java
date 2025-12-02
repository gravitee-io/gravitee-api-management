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

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.portal_page.model.PortalArea;
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
}
