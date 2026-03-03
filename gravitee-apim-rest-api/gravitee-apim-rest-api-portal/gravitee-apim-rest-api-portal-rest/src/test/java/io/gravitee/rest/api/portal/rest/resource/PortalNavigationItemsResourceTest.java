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
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.rest.api.portal.rest.fixture.PortalNavigationFixtures;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class PortalNavigationItemsResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "DEFAULT";

    @Autowired
    private PortalNavigationItemsQueryServiceInMemory portalNavigationItemsQueryService;

    @Override
    protected String contextPath() {
        return "portal-navigation-items";
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

    @Test
    public void should_return_portal_navigation_items_for_environment() {
        // Given
        List<PortalNavigationItem> items = PortalNavigationFixtures.sampleList(PortalArea.HOMEPAGE);
        items.forEach(item -> item.setEnvironmentId(ENV_ID));
        portalNavigationItemsQueryService.initWith(items);

        // When
        Response response = target()
            .queryParam("area", io.gravitee.rest.api.portal.rest.model.PortalArea.HOMEPAGE)
            .queryParam("loadChildren", true)
            .request()
            .get();

        // Then
        assertThat(response.getStatus()).isEqualTo(200);
        var result = response.readEntity(
            new jakarta.ws.rs.core.GenericType<List<io.gravitee.rest.api.portal.rest.model.PortalNavigationItem>>() {}
        );
        assertThat(result).hasSize(items.size());
    }

    @Test
    public void should_return_portal_navigation_items_with_parent_id() {
        // Given
        List<PortalNavigationItem> items = PortalNavigationFixtures.sampleList(PortalArea.HOMEPAGE);
        items.forEach(item -> item.setEnvironmentId(ENV_ID));
        portalNavigationItemsQueryService.initWith(items);

        // When - using a parentId that doesn't exist in the fixtures
        String parentId = PortalNavigationFixtures.randomNavigationId().toString();
        Response response = target()
            .queryParam("area", io.gravitee.rest.api.portal.rest.model.PortalArea.HOMEPAGE)
            .queryParam("parentId", parentId)
            .queryParam("loadChildren", false)
            .request()
            .get();

        // Then
        assertThat(response.getStatus()).isEqualTo(200);
        var result = response.readEntity(
            new jakarta.ws.rs.core.GenericType<List<io.gravitee.rest.api.portal.rest.model.PortalNavigationItem>>() {}
        );
        assertThat(result).isEmpty();
    }

    @Test
    void should_return_only_published_navigation_items() {
        // Given
        var publishedItem = PortalNavigationFixtures.page(
            PortalNavigationFixtures.randomNavigationId(),
            "Published Page",
            PortalArea.TOP_NAVBAR,
            PortalNavigationFixtures.randomPageId()
        );
        publishedItem.setEnvironmentId(ENV_ID);
        publishedItem.setPublished(true);

        var unpublishedItem = PortalNavigationFixtures.link(
            PortalNavigationFixtures.randomNavigationId(),
            "Unpublished Link",
            PortalArea.TOP_NAVBAR,
            "https://example.com"
        );
        unpublishedItem.setEnvironmentId(ENV_ID);
        unpublishedItem.setPublished(false);

        List<PortalNavigationItem> items = List.of(publishedItem, unpublishedItem);
        portalNavigationItemsQueryService.initWith(items);

        // When
        Response response = target()
            .queryParam("area", io.gravitee.rest.api.portal.rest.model.PortalArea.TOP_NAVBAR)
            .queryParam("loadChildren", true)
            .request()
            .get();

        // Then
        assertThat(response.getStatus()).isEqualTo(200);
        var result = response.readEntity(
            new jakarta.ws.rs.core.GenericType<List<io.gravitee.rest.api.portal.rest.model.PortalNavigationItem>>() {}
        );

        // Only returns published items
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPortalNavigationPage().getTitle()).isEqualTo("Published Page");
    }

    @Test
    void should_not_show_children_of_unpublished_parent() {
        // Given
        List<PortalNavigationItem> items = PortalNavigationFixtures.unpublishedParentHierarchy(PortalArea.TOP_NAVBAR, ENV_ID);
        portalNavigationItemsQueryService.initWith(items);

        // When
        Response response = target().queryParam("area", PortalArea.TOP_NAVBAR).queryParam("loadChildren", true).request().get();

        // Then
        assertThat(response.getStatus()).isEqualTo(200);

        var resultList = response.readEntity(
            new jakarta.ws.rs.core.GenericType<List<io.gravitee.rest.api.portal.rest.model.PortalNavigationItem>>() {}
        );

        assertThat(resultList)
            .extracting(this::getIdFromItem)
            .containsExactlyInAnyOrder(PortalNavigationFixtures.GRANDPARENT_ID.toString(), PortalNavigationFixtures.SIBLING_ID.toString())
            .doesNotContain(
                PortalNavigationFixtures.PARENT_ID.toString(),
                PortalNavigationFixtures.CHILD1_ID.toString(),
                PortalNavigationFixtures.CHILD2_ID.toString()
            );
    }

    private String getIdFromItem(io.gravitee.rest.api.portal.rest.model.PortalNavigationItem item) {
        Object actual = item.getActualInstance();

        if (actual instanceof io.gravitee.rest.api.portal.rest.model.PortalNavigationPage page) {
            return page.getId();
        } else if (actual instanceof io.gravitee.rest.api.portal.rest.model.PortalNavigationFolder folder) {
            return folder.getId();
        } else if (actual instanceof io.gravitee.rest.api.portal.rest.model.PortalNavigationLink link) {
            return link.getId();
        }

        return null;
    }

    @Test
    void should_return_navigation_items_when_user_is_authenticated() {
        // Given
        List<PortalNavigationItem> items = PortalNavigationFixtures.sampleList(PortalArea.HOMEPAGE);
        items.forEach(item -> item.setEnvironmentId(ENV_ID));
        portalNavigationItemsQueryService.initWith(items);

        // When
        Response response = target()
            .queryParam("area", io.gravitee.rest.api.portal.rest.model.PortalArea.HOMEPAGE)
            .queryParam("loadChildren", true)
            .request()
            .get();

        // Then
        assertThat(response.getStatus()).isEqualTo(200);
        var result = response.readEntity(
            new jakarta.ws.rs.core.GenericType<List<io.gravitee.rest.api.portal.rest.model.PortalNavigationItem>>() {}
        );
        assertThat(result).hasSize(items.size());
    }
}
