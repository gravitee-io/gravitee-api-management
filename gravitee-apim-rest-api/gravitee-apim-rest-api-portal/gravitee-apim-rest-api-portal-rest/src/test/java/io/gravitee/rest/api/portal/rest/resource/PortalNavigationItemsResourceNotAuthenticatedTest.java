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
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class PortalNavigationItemsResourceNotAuthenticatedTest extends AbstractResourceTest {

    private static final String ENV_ID = "DEFAULT";

    @Autowired
    private PortalNavigationItemsQueryServiceInMemory portalNavigationItemsQueryService;

    @Override
    protected String contextPath() {
        return "portal-navigation-items";
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
    }

    @Test
    void should_return_navigation_items_when_user_is_not_authenticated() {
        // Given
        List<PortalNavigationItem> items = PortalNavigationFixtures.sampleList(PortalArea.TOP_NAVBAR);
        items.forEach(item -> item.setEnvironmentId(ENV_ID));

        var publishedAndPublicItem = items.getFirst();
        publishedAndPublicItem.setPublished(true);
        publishedAndPublicItem.setVisibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PUBLIC);

        var unpublishedItem = items.get(1);
        unpublishedItem.setPublished(false);
        unpublishedItem.setVisibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PUBLIC);

        var privateItem = items.get(2);
        privateItem.setPublished(true);
        privateItem.setVisibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PRIVATE);

        portalNavigationItemsQueryService.initWith(List.of(publishedAndPublicItem, unpublishedItem, privateItem));

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
        assertThat(result).hasSize(1);
    }
}
