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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NO_CONTENT_204;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiDocumentationNavigationResource_UnpublishTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";
    private static final String API_ID = "api-id";

    @Inject
    private EnvironmentService environmentService;

    @Autowired
    private PortalNavigationItemsCrudServiceInMemory portalNavigationItemCrudService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API_ID + "/portal-navigation-items/_unpublish";
    }

    @BeforeEach
    public void init() {
        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        apiCrudService.initWith(List.of(Api.builder().id(API_ID).name("My API").environmentId(ENVIRONMENT).build()));
    }

    @AfterEach
    public void cleanUp() {
        GraviteeContext.cleanContext();
        apiCrudService.reset();
        portalNavigationItemCrudService.reset();
    }

    @Test
    void should_unpublish_the_api_by_hiding_its_listing_and_its_documentation() {
        var section = aSection();
        var listing = aListing(section);
        var page = aPublishedPageOwnedByTheApi();
        portalNavigationItemCrudService.initWith(List.of(section, listing, page));

        Response response = unpublish();

        assertThat(response).hasStatus(NO_CONTENT_204);
        assertThat(portalNavigationItemCrudService.storage())
            .extracting(PortalNavigationItem::getId)
            .containsExactlyInAnyOrder(section.getId(), listing.getId(), page.getId());
        assertThat(listing.getPublished()).isFalse();
        assertThat(page.getPublished()).isFalse();
        assertThat(section.getPublished()).isTrue();
    }

    @Test
    void should_return_400_when_the_api_is_not_listed() {
        var page = aPublishedPageOwnedByTheApi();
        portalNavigationItemCrudService.initWith(List.of(page));

        Response response = unpublish();

        assertThat(response).hasStatus(BAD_REQUEST_400);
        assertThat(page.getPublished()).isTrue();
    }

    @Test
    void should_return_400_when_the_listing_is_already_hidden() {
        var section = aSection();
        var listing = aListing(section);
        listing.setPublished(false);
        portalNavigationItemCrudService.initWith(List.of(section, listing));

        Response response = unpublish();

        assertThat(response).hasStatus(BAD_REQUEST_400);
    }

    @Test
    void should_return_403_with_environment_documentation_permission_only() {
        var section = aSection();
        var listing = aListing(section);
        portalNavigationItemCrudService.initWith(List.of(section, listing));
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                eq(API_ID),
                eq(RolePermissionAction.UPDATE)
            )
        ).thenReturn(false);

        Response response = unpublish();

        assertThat(response).hasStatus(FORBIDDEN_403);
        assertThat(listing.getPublished()).isTrue();
    }

    @Test
    void should_succeed_with_api_documentation_permission_only() {
        var section = aSection();
        var listing = aListing(section);
        portalNavigationItemCrudService.initWith(List.of(section, listing));
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                eq(API_ID),
                eq(RolePermissionAction.UPDATE)
            )
        ).thenReturn(true);

        Response response = unpublish();

        assertThat(response).hasStatus(NO_CONTENT_204);
    }

    private Response unpublish() {
        return rootTarget().request().post(Entity.json(""));
    }

    private static PortalNavigationFolder aSection() {
        var section = PortalNavigationItemFixtures.aFolder("APIs");
        section.setEnvironmentId(ENVIRONMENT);
        section.markAsRoot();
        return section;
    }

    private static PortalNavigationApi aListing(PortalNavigationFolder section) {
        var listing = PortalNavigationItemFixtures.anApi(PortalNavigationItemId.random().json(), "My API", section.getId(), API_ID);
        listing.setEnvironmentId(ENVIRONMENT);
        listing.updateParent(section);
        return listing;
    }

    private static PortalNavigationPage aPublishedPageOwnedByTheApi() {
        var page = PortalNavigationItemFixtures.aPage("Overview", null)
            .toBuilder()
            .reference(new NavigationItemReference.ApiReference(API_ID))
            .published(true)
            .build();
        page.setEnvironmentId(ENVIRONMENT);
        page.markAsRoot();
        return page;
    }
}
