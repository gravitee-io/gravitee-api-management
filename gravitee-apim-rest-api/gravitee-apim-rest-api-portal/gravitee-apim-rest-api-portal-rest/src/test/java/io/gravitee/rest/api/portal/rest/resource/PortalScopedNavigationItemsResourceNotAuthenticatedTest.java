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

import static io.gravitee.rest.api.portal.rest.resource.PortalScopedNavigationItemsResourceTest.ENV_ID;
import static io.gravitee.rest.api.portal.rest.resource.PortalScopedNavigationItemsResourceTest.ORG_ID;
import static io.gravitee.rest.api.portal.rest.resource.PortalScopedNavigationItemsResourceTest.givenPortal;
import static io.gravitee.rest.api.portal.rest.resource.PortalScopedNavigationItemsResourceTest.page;
import static io.gravitee.rest.api.portal.rest.resource.PortalScopedNavigationItemsResourceTest.statusesForEveryPath;
import static org.assertj.core.api.Assertions.assertThat;

import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class PortalScopedNavigationItemsResourceNotAuthenticatedTest extends AbstractResourceTest {

    @Autowired
    private PortalNavigationItemsQueryServiceInMemory portalNavigationItemsQueryService;

    @Autowired
    private PortalCrudServiceInMemory portalCrudService;

    private PortalId portal;

    @Override
    protected String contextPath() {
        return "portal/";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(NotAuthenticatedAuthenticationFilter.class);
    }

    @BeforeEach
    public void init() {
        GraviteeContext.setCurrentOrganization(ORG_ID);
        GraviteeContext.setCurrentEnvironment(ENV_ID);
        portal = givenPortal(portalCrudService, ENV_ID);
    }

    @AfterEach
    public void tearDown() {
        GraviteeContext.cleanContext();
        portalNavigationItemsQueryService.reset();
        portalCrudService.reset();
    }

    @Test
    void should_answer_not_found_on_every_path_for_an_unknown_portal() {
        var page = page(portal, PortalPageContentId.random());
        portalNavigationItemsQueryService.initWith(List.of(page));

        assertThat(statusesForEveryPath(this::target, "6c2b1e9e-0000-4000-8000-000000000000", page.getId().json())).containsOnly(404);
    }

    @Test
    void should_answer_bad_request_on_every_path_for_a_malformed_portal_id() {
        var page = page(portal, PortalPageContentId.random());
        portalNavigationItemsQueryService.initWith(List.of(page));

        assertThat(statusesForEveryPath(this::target, "not-a-uuid", page.getId().json())).containsOnly(400);
    }

    @Test
    void should_answer_not_found_for_a_portal_of_another_environment() {
        var elsewhere = givenPortal(portalCrudService, "another-env");
        var page = page(portal, PortalPageContentId.random());
        portalNavigationItemsQueryService.initWith(List.of(page));

        assertThat(statusesForEveryPath(this::target, elsewhere.toString(), page.getId().json())).containsOnly(404);
    }

    @Test
    void should_not_return_an_item_of_another_portal() {
        var otherPortal = givenPortal(portalCrudService, ENV_ID);
        var page = page(otherPortal, PortalPageContentId.random());
        portalNavigationItemsQueryService.initWith(List.of(page));

        assertThat(target(portal + "/portal-navigation-items/" + page.getId().json()).request().get().getStatus()).isEqualTo(404);
    }
}
