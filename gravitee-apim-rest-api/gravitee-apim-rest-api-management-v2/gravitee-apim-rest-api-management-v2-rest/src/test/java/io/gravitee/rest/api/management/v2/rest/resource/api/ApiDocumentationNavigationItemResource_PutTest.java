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
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.rest.api.management.v2.rest.model.BaseUpdatePortalNavigationItem;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemType;
import io.gravitee.rest.api.management.v2.rest.model.PortalVisibility;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationPage;
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
class ApiDocumentationNavigationItemResource_PutTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";
    private static final String API_ID = "api-id";
    private static final String OTHER_API_ID = "other-api-id";

    @Inject
    private EnvironmentService environmentService;

    @Autowired
    private PortalNavigationItemsCrudServiceInMemory portalNavigationItemCrudService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API_ID + "/portal-navigation-items";
    }

    @BeforeEach
    public void init() {
        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    public void cleanUp() {
        GraviteeContext.cleanContext();
        portalNavigationItemCrudService.reset();
    }

    @Test
    void should_update_an_item() {
        var page = aPageOwnedBy(API_ID, "Overview", 0);
        portalNavigationItemCrudService.initWith(List.of(page));

        Response response = put(page.getId(), anUpdateOf(page).title("Introduction"));

        assertThat(response).hasStatus(OK_200);
        assertThat(stored(page.getId()).getTitle()).isEqualTo("Introduction");
        assertThat(stored(page.getId()).getReference()).isEqualTo(new NavigationItemReference.ApiReference(API_ID));
        assertThat(stored(page.getId()).getParentId()).isNull();
    }

    @Test
    void should_flip_only_the_published_flag_of_one_item() {
        var page = aPageOwnedBy(API_ID, "Overview", 0);
        var sibling = aPageOwnedBy(API_ID, "Guide", 1);
        var section = PortalNavigationItemFixtures.aFolder("APIs");
        section.setEnvironmentId(ENVIRONMENT);
        section.markAsRoot();
        var listing = PortalNavigationItemFixtures.anApi(PortalNavigationItemId.random().json(), "My API", section.getId(), API_ID);
        listing.setEnvironmentId(ENVIRONMENT);
        listing.updateParent(section);
        portalNavigationItemCrudService.initWith(List.of(page, sibling, section, listing));

        Response response = put(page.getId(), anUpdateOf(page).published(true));

        assertThat(response).hasStatus(OK_200);
        assertThat(stored(page.getId()).getPublished()).isTrue();
        assertThat(stored(sibling.getId()).getPublished()).isFalse();
        assertThat(stored(listing.getId()).getPublished()).isTrue();
    }

    @Test
    void should_move_an_item_into_a_folder_of_the_same_api_and_keep_its_owner() {
        var folder = aFolderOwnedBy(API_ID);
        var page = aPageOwnedBy(API_ID, "Overview", 1);
        portalNavigationItemCrudService.initWith(List.of(folder, page));

        Response response = put(page.getId(), anUpdateOf(page).parentId(folder.getId().id()).order(0));

        assertThat(response).hasStatus(OK_200);
        assertThat(stored(page.getId()).getParentId()).isEqualTo(folder.getId());
        assertThat(stored(page.getId()).getReference()).isEqualTo(new NavigationItemReference.ApiReference(API_ID));
    }

    /**
     * With no parent in the request there is nothing to check ownership against: the item goes to the top
     * level of whoever owns it already, so it stays documentation of the API and never becomes a portal root.
     */
    @Test
    void should_move_an_item_back_to_the_top_level_of_the_api_when_no_parent_is_sent() {
        var folder = aFolderOwnedBy(API_ID);
        var page = aPageOwnedBy(API_ID, "Overview", 0);
        page.updateParent(folder);
        portalNavigationItemCrudService.initWith(List.of(folder, page));

        Response response = put(page.getId(), anUpdateOf(page));

        assertThat(response).hasStatus(OK_200);
        assertThat(stored(page.getId())).satisfies(moved -> {
            assertThat(moved.getParentId()).isNull();
            assertThat(moved.getRootId()).isEqualTo(page.getId());
            assertThat(moved.getReference()).isEqualTo(new NavigationItemReference.ApiReference(API_ID));
        });
    }

    @Test
    void should_reject_an_item_id_belonging_to_another_api() {
        var foreign = aPageOwnedBy(OTHER_API_ID, "Other overview", 0);
        portalNavigationItemCrudService.initWith(List.of(foreign));

        Response response = put(foreign.getId(), anUpdateOf(foreign).title("Hijacked"));

        assertThat(response).hasStatus(NOT_FOUND_404);
        assertThat(stored(foreign.getId()).getTitle()).isEqualTo("Other overview");
    }

    @Test
    void should_reject_a_parent_belonging_to_another_api() {
        var foreignFolder = aFolderOwnedBy(OTHER_API_ID);
        var page = aPageOwnedBy(API_ID, "Overview", 0);
        portalNavigationItemCrudService.initWith(List.of(foreignFolder, page));

        Response response = put(page.getId(), anUpdateOf(page).parentId(foreignFolder.getId().id()));

        assertThat(response).hasStatus(NOT_FOUND_404);
        assertThat(stored(page.getId()).getParentId()).isNull();
    }

    @Test
    void should_reject_a_portal_owned_parent() {
        var section = PortalNavigationItemFixtures.aFolder("APIs");
        section.setEnvironmentId(ENVIRONMENT);
        section.markAsRoot();
        var page = aPageOwnedBy(API_ID, "Overview", 0);
        portalNavigationItemCrudService.initWith(List.of(section, page));

        Response response = put(page.getId(), anUpdateOf(page).parentId(section.getId().id()));

        assertThat(response).hasStatus(NOT_FOUND_404);
        assertThat(stored(page.getId()).getParentId()).isNull();
    }

    @Test
    void should_reject_the_api_listing_row() {
        var section = PortalNavigationItemFixtures.aFolder("APIs");
        section.setEnvironmentId(ENVIRONMENT);
        section.markAsRoot();
        var listing = PortalNavigationItemFixtures.anApi(PortalNavigationItemId.random().json(), "My API", section.getId(), API_ID);
        listing.setEnvironmentId(ENVIRONMENT);
        listing.updateParent(section);
        portalNavigationItemCrudService.initWith(List.of(section, listing));
        var update = new UpdatePortalNavigationPage()
            .type(PortalNavigationItemType.PAGE)
            .title("My API")
            .order(0)
            .parentId(section.getId().id())
            .published(false)
            .visibility(PortalVisibility.PUBLIC);

        Response response = put(listing.getId(), update);

        assertThat(response).hasStatus(NOT_FOUND_404);
        assertThat(stored(listing.getId()).getPublished()).isTrue();
    }

    @Test
    void should_return_403_with_environment_documentation_permission_only() {
        var page = aPageOwnedBy(API_ID, "Overview", 0);
        portalNavigationItemCrudService.initWith(List.of(page));
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                eq(API_ID),
                eq(RolePermissionAction.UPDATE)
            )
        ).thenReturn(false);

        Response response = put(page.getId(), anUpdateOf(page).title("Introduction"));

        assertThat(response).hasStatus(FORBIDDEN_403);
        assertThat(stored(page.getId()).getTitle()).isEqualTo("Overview");
    }

    @Test
    void should_succeed_with_api_documentation_permission_only() {
        var page = aPageOwnedBy(API_ID, "Overview", 0);
        portalNavigationItemCrudService.initWith(List.of(page));
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                eq(API_ID),
                eq(RolePermissionAction.UPDATE)
            )
        ).thenReturn(true);

        Response response = put(page.getId(), anUpdateOf(page).title("Introduction"));

        assertThat(response).hasStatus(OK_200);
    }

    private Response put(PortalNavigationItemId id, BaseUpdatePortalNavigationItem update) {
        return rootTarget(id.json()).request().put(Entity.json(update));
    }

    private PortalNavigationItem stored(PortalNavigationItemId id) {
        return portalNavigationItemCrudService
            .storage()
            .stream()
            .filter(item -> item.getId().equals(id))
            .findFirst()
            .orElseThrow();
    }

    private static BaseUpdatePortalNavigationItem anUpdateOf(PortalNavigationPage page) {
        return new UpdatePortalNavigationPage()
            .type(PortalNavigationItemType.PAGE)
            .title(page.getTitle())
            .order(page.getOrder())
            .published(page.getPublished())
            .visibility(PortalVisibility.PUBLIC);
    }

    private static PortalNavigationPage aPageOwnedBy(String apiId, String title, int order) {
        var page = PortalNavigationItemFixtures.aPage(title, null)
            .toBuilder()
            .reference(new NavigationItemReference.ApiReference(apiId))
            .order(order)
            .published(false)
            .build();
        page.setEnvironmentId(ENVIRONMENT);
        page.markAsRoot();
        return page;
    }

    private static PortalNavigationFolder aFolderOwnedBy(String apiId) {
        var folder = PortalNavigationItemFixtures.aFolder(PortalNavigationItemId.random().json(), "Guides")
            .toBuilder()
            .reference(new NavigationItemReference.ApiReference(apiId))
            .order(0)
            .published(false)
            .build();
        folder.setEnvironmentId(ENVIRONMENT);
        folder.markAsRoot();
        return folder;
    }
}
