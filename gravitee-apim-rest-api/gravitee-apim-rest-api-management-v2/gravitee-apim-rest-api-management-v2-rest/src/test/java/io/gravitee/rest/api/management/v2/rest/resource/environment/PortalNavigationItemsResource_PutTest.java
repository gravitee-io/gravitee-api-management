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
package io.gravitee.rest.api.management.v2.rest.resource.environment;

import static assertions.MAPIAssertions.assertThat;
import static fixtures.core.model.PortalNavigationItemFixtures.APIS_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.LINK1_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.PAGE11_ID;
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static jakarta.ws.rs.client.Entity.json;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.rest.api.management.v2.rest.model.BaseUpdatePortalNavigationItem;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationFolder;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemType;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationLink;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationPage;
import io.gravitee.rest.api.management.v2.rest.model.PortalVisibility;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationFolder;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationLink;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationPage;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationItemResource_PutTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private PortalNavigationItemsQueryService portalNavigationItemsQueryService;

    @Inject
    private PortalNavigationItemCrudService portalNavigationItemCrudService;

    private WebTarget target;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/portal-navigation-items";
    }

    @BeforeEach
    public void setUp() {
        target = rootTarget();

        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        // Default permission for UPDATE on documentation
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.UPDATE
            )
        ).thenReturn(true);

        // Seed storage with sample items and adjust env/org to current test values
        var items = PortalNavigationItemFixtures.sampleNavigationItems();
        items.forEach(i -> {
            i.setEnvironmentId(ENVIRONMENT);
            i.setOrganizationId(ORGANIZATION);
        });
        ((PortalNavigationItemsQueryServiceInMemory) portalNavigationItemsQueryService).initWith(items);
        ((PortalNavigationItemsCrudServiceInMemory) portalNavigationItemCrudService).initWith(items);
    }

    @AfterEach
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    void should_update_portal_navigation_item_title_page() {
        // Given an existing PAGE item id from fixtures
        String navId = PAGE11_ID;

        // When: PUT with a new title
        BaseUpdatePortalNavigationItem payload = new UpdatePortalNavigationPage()
            .title("  Updated Title  ")
            .order(1)
            .type(PortalNavigationItemType.PAGE)
            .published(true)
            .visibility(PortalVisibility.PUBLIC);
        Response response = target.path(navId).request().put(json(payload));

        // Then: 200 OK and response payload with trimmed title
        assertThat(response).hasStatus(OK_200);
        PortalNavigationPage body = response.readEntity(PortalNavigationPage.class);
        assertThat(body).isNotNull();
        assertThat(body.getId()).isEqualTo(UUID.fromString(navId));
        assertThat(body.getTitle()).isEqualTo("Updated Title");
        assertThat(body.getPublished()).isTrue();

        // And storage reflects the change
        var updated = portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, PortalNavigationItemId.of(navId));
        assertThat(updated.getTitle()).isEqualTo("Updated Title");
    }

    @Test
    void should_update_portal_navigation_item_title_link() {
        // Given an existing LINK item id from fixtures
        String navId = LINK1_ID;

        // When: PUT with a new title
        BaseUpdatePortalNavigationItem payload = new UpdatePortalNavigationLink()
            .url("https://gravitee.io")
            .title("  Updated Title  ")
            .type(PortalNavigationItemType.LINK)
            .order(0)
            .published(true)
            .visibility(PortalVisibility.PUBLIC);
        Response response = target.path(navId).request().put(json(payload));

        // Then: 200 OK and response payload with trimmed title
        assertThat(response).hasStatus(OK_200);
        PortalNavigationLink body = response.readEntity(PortalNavigationLink.class);
        assertThat(body).isNotNull();
        assertThat(body.getTitle()).isEqualTo("Updated Title");
        assertThat(body.getUrl()).isEqualTo("https://gravitee.io");
        assertThat(body.getPublished()).isTrue();
        assertThat(body.getVisibility()).isEqualTo(PortalVisibility.PUBLIC);

        // And storage reflects the change
        var updated = portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, PortalNavigationItemId.of(navId));
        assertThat(updated.getTitle()).isEqualTo("Updated Title");
    }

    @Test
    void should_update_portal_navigation_item_title_folder() {
        // Given an existing FOLDER item id from fixtures
        String navId = APIS_ID;

        // When: PUT with a new title
        BaseUpdatePortalNavigationItem payload = new UpdatePortalNavigationFolder()
            .title("  Updated Title  ")
            .type(PortalNavigationItemType.FOLDER)
            .order(2)
            .published(true)
            .visibility(PortalVisibility.PUBLIC);
        Response response = target.path(navId).request().put(json(payload));

        // Then: 200 OK and response payload with trimmed title
        assertThat(response).hasStatus(OK_200);
        PortalNavigationFolder body = response.readEntity(PortalNavigationFolder.class);
        assertThat(body).isNotNull();
        assertThat(body.getId()).isEqualTo(UUID.fromString(navId));
        assertThat(body.getTitle()).isEqualTo("Updated Title");
        assertThat(body.getPublished()).isTrue();
        assertThat(body.getVisibility()).isEqualTo(PortalVisibility.PUBLIC);

        // And storage reflects the change
        var updated = portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, PortalNavigationItemId.of(navId));
        assertThat(updated.getTitle()).isEqualTo("Updated Title");
    }

    @Test
    void should_check_type_consistency() {
        // Given an existing LINK item id from fixtures
        String navId = LINK1_ID;

        // When: PUT with a new title of type PAGE
        BaseUpdatePortalNavigationItem payload = new UpdatePortalNavigationPage()
            .title("  Updated Title  ")
            .type(PortalNavigationItemType.LINK)
            .order(3)
            .published(false)
            .visibility(PortalVisibility.PUBLIC);
        Response response = target.path(navId).request().put(json(payload));

        // Then: 400 Bad request and response payload with trimmed title
        assertThat(response).hasStatus(BAD_REQUEST_400);

        // And storage does not reflects the change
        var updated = portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, PortalNavigationItemId.of(navId));
        assertThat(updated.getTitle()).isNotEqualTo("Updated Title");
    }

    @Test
    void should_return_404_when_item_not_found() {
        // Given a random id not present in storage
        String unknownId = PortalNavigationItemId.random().toString();

        BaseUpdatePortalNavigationItem payload = new UpdatePortalNavigationPage()
            .title("Won't work")
            .type(PortalNavigationItemType.PAGE)
            .order(1)
            .published(false)
            .visibility(PortalVisibility.PUBLIC);
        Response response = target.path(unknownId).request().put(json(payload));

        assertThat(response).hasStatus(NOT_FOUND_404);
    }

    @Test
    void should_update_portal_navigation_item_order_page() {
        // Given an existing PAGE item id from fixtures
        String navId = PAGE11_ID;

        // When: PUT with a new title
        BaseUpdatePortalNavigationItem payload = new UpdatePortalNavigationPage()
            .title("  Updated Title  ")
            .order(3)
            .type(PortalNavigationItemType.PAGE)
            .published(false)
            .visibility(PortalVisibility.PUBLIC);
        Response response = target.path(navId).request().put(json(payload));

        // Then: 200 OK and response payload with trimmed title and updated order
        assertThat(response).hasStatus(OK_200);
        PortalNavigationPage body = response.readEntity(PortalNavigationPage.class);
        assertThat(body).isNotNull();
        assertThat(body.getId()).isEqualTo(UUID.fromString(navId));
        assertThat(body.getTitle()).isEqualTo("Updated Title");
        assertThat(body.getOrder()).isEqualTo(3);
        assertThat(body.getVisibility()).isEqualTo(PortalVisibility.PUBLIC);

        // And storage reflects the change
        var updated = portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, PortalNavigationItemId.of(navId));
        assertThat(updated.getTitle()).isEqualTo("Updated Title");
        assertThat(updated.getOrder()).isEqualTo(3);
    }

    @Test
    void should_update_portal_navigation_item_parentId_link() {
        // Given an existing LINK item id from fixtures
        String navId = LINK1_ID;

        // When: PUT with a new title
        BaseUpdatePortalNavigationItem payload = new UpdatePortalNavigationLink()
            .url("https://gravitee.io")
            .title("  Updated Title  ")
            .type(PortalNavigationItemType.LINK)
            .order(0)
            .parentId(UUID.fromString(APIS_ID))
            .published(false)
            .visibility(PortalVisibility.PUBLIC);
        Response response = target.path(navId).request().put(json(payload));

        // Then: 200 OK and response payload with trimmed title and updated parentId
        assertThat(response).hasStatus(OK_200);
        PortalNavigationLink body = response.readEntity(PortalNavigationLink.class);
        assertThat(body).isNotNull();
        assertThat(body.getTitle()).isEqualTo("Updated Title");
        assertThat(body.getUrl()).isEqualTo("https://gravitee.io");
        assertThat(body.getParentId()).isEqualTo(UUID.fromString(APIS_ID));
        assertThat(body.getVisibility()).isEqualTo(PortalVisibility.PUBLIC);

        // And storage reflects the change
        var updated = portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, PortalNavigationItemId.of(navId));
        assertThat(updated.getTitle()).isEqualTo("Updated Title");
        assertThat(updated.getParentId()).isEqualTo(PortalNavigationItemId.of(APIS_ID));
    }

    @Test
    void should_update_portal_navigation_item_parentId_folder_to_Root_Level_when_parentId_null() {
        // Given an existing FOLDER item id from fixtures
        String navId = APIS_ID;

        // When: PUT with a new title
        BaseUpdatePortalNavigationItem payload = new UpdatePortalNavigationFolder()
            .title("  Updated Title  ")
            .type(PortalNavigationItemType.FOLDER)
            .order(2)
            .published(false)
            .visibility(PortalVisibility.PUBLIC);
        Response response = target.path(navId).request().put(json(payload));

        // Then: 200 OK and response payload with trimmed title and parentId null ==> moving to root level
        assertThat(response).hasStatus(OK_200);
        PortalNavigationFolder body = response.readEntity(PortalNavigationFolder.class);
        assertThat(body).isNotNull();
        assertThat(body.getId()).isEqualTo(UUID.fromString(navId));
        assertThat(body.getTitle()).isEqualTo("Updated Title");
        assertThat(body.getParentId()).isNull();
        assertThat(body.getVisibility()).isEqualTo(PortalVisibility.PUBLIC);

        // And storage reflects the change
        var updated = portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, PortalNavigationItemId.of(navId));
        assertThat(updated.getTitle()).isEqualTo("Updated Title");
        assertThat(updated.getParentId()).isNull();
    }

    @Test
    void should_publish_an_unpublished_page() {
        // Given an existing PAGE item
        String navId = PAGE11_ID;

        // When: PUT with published = true
        BaseUpdatePortalNavigationItem payload = new UpdatePortalNavigationPage()
            .title("  Updated Title  ")
            .order(1)
            .type(PortalNavigationItemType.PAGE)
            .published(true)
            .visibility(PortalVisibility.PUBLIC);
        Response response = target.path(navId).request().put(json(payload));

        // Then: 200 OK and response payload with trimmed title
        assertThat(response).hasStatus(OK_200);
        PortalNavigationPage body = response.readEntity(PortalNavigationPage.class);
        assertThat(body).isNotNull();
        assertThat(body.getId()).isEqualTo(UUID.fromString(navId));
        assertThat(body.getTitle()).isEqualTo("Updated Title");
        assertThat(body.getPublished()).isTrue();
        assertThat(body.getVisibility()).isEqualTo(PortalVisibility.PUBLIC);

        // And storage reflects the change
        var updated = portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, PortalNavigationItemId.of(navId));
        assertThat(updated.getTitle()).isEqualTo("Updated Title");
        assertThat(updated.getPublished()).isTrue();
    }

    @Test
    void should_publish_only_selected_folder_when_propagation_query_param_is_omitted() {
        var parentFolder = PortalNavigationItemFixtures.aFolder("20000000-0000-4000-8000-000000000019", "Parent")
            .toBuilder()
            .published(false)
            .build();
        var childFolder = PortalNavigationItemFixtures.aFolder("20000000-0000-4000-8000-000000000020", "Child", parentFolder.getId())
            .toBuilder()
            .published(false)
            .build();
        var grandChildPage = PortalNavigationItemFixtures.aPage("20000000-0000-4000-8000-000000000021", "Grand Child", childFolder.getId())
            .toBuilder()
            .published(false)
            .build();
        initStorageWith(List.of(parentFolder, childFolder, grandChildPage));

        Response response = target.path(parentFolder.getId().toString()).request().put(json(updateFolderPublished(parentFolder, true)));

        assertThat(response).hasStatus(OK_200);
        PortalNavigationFolder body = response.readEntity(PortalNavigationFolder.class);
        assertThat(body.getPublished()).isTrue();
        assertThat(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, parentFolder.getId()).getPublished()).isTrue();
        assertThat(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, childFolder.getId()).getPublished()).isFalse();
        assertThat(
            portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, grandChildPage.getId()).getPublished()
        ).isFalse();
    }

    @Test
    void should_publish_folder_descendants_when_propagation_query_param_is_true() {
        var parentFolder = PortalNavigationItemFixtures.aFolder("20000000-0000-4000-8000-000000000022", "Parent")
            .toBuilder()
            .published(false)
            .build();
        var childFolder = PortalNavigationItemFixtures.aFolder("20000000-0000-4000-8000-000000000023", "Child", parentFolder.getId())
            .toBuilder()
            .published(false)
            .build();
        var grandChildPage = PortalNavigationItemFixtures.aPage("20000000-0000-4000-8000-000000000024", "Grand Child", childFolder.getId())
            .toBuilder()
            .published(false)
            .build();
        initStorageWith(List.of(parentFolder, childFolder, grandChildPage));

        Response response = target
            .path(parentFolder.getId().toString())
            .queryParam("propagatePublishToChildren", true)
            .request()
            .put(json(updateFolderPublished(parentFolder, true)));

        assertThat(response).hasStatus(OK_200);
        PortalNavigationFolder body = response.readEntity(PortalNavigationFolder.class);
        assertThat(body.getPublished()).isTrue();
        assertThat(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, parentFolder.getId()).getPublished()).isTrue();
        assertThat(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, childFolder.getId()).getPublished()).isTrue();
        assertThat(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, grandChildPage.getId()).getPublished()).isTrue();
    }

    @Test
    void should_unpublish_folder_descendants_when_propagation_query_param_is_omitted() {
        var parentFolder = PortalNavigationItemFixtures.aFolder("20000000-0000-4000-8000-000000000025", "Parent")
            .toBuilder()
            .published(true)
            .build();
        var childFolder = PortalNavigationItemFixtures.aFolder("20000000-0000-4000-8000-000000000026", "Child", parentFolder.getId())
            .toBuilder()
            .published(true)
            .build();
        var grandChildPage = PortalNavigationItemFixtures.aPage("20000000-0000-4000-8000-000000000027", "Grand Child", childFolder.getId())
            .toBuilder()
            .published(true)
            .build();
        initStorageWith(List.of(parentFolder, childFolder, grandChildPage));

        Response response = target.path(parentFolder.getId().toString()).request().put(json(updateFolderPublished(parentFolder, false)));

        assertThat(response).hasStatus(OK_200);
        PortalNavigationFolder body = response.readEntity(PortalNavigationFolder.class);
        assertThat(body.getPublished()).isFalse();
        assertThat(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, parentFolder.getId()).getPublished()).isFalse();
        assertThat(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, childFolder.getId()).getPublished()).isFalse();
        assertThat(
            portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, grandChildPage.getId()).getPublished()
        ).isFalse();
    }

    @Test
    void should_change_a_page_visibility_to_private() {
        // Given an existing PAGE item
        String navId = PAGE11_ID;

        // When: PUT with visibility = PRIVATE
        BaseUpdatePortalNavigationItem payload = new UpdatePortalNavigationPage()
            .title("  Updated Title  ")
            .order(1)
            .type(PortalNavigationItemType.PAGE)
            .published(true)
            .visibility(PortalVisibility.PRIVATE);
        Response response = target.path(navId).request().put(json(payload));

        // Then: 200 OK and response payload with trimmed title
        assertThat(response).hasStatus(OK_200);
        PortalNavigationPage body = response.readEntity(PortalNavigationPage.class);
        assertThat(body).isNotNull();
        assertThat(body.getId()).isEqualTo(UUID.fromString(navId));
        assertThat(body.getTitle()).isEqualTo("Updated Title");
        assertThat(body.getPublished()).isTrue();
        assertThat(body.getVisibility()).isEqualTo(PortalVisibility.PRIVATE);

        // And storage reflects the change
        var updated = portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, PortalNavigationItemId.of(navId));
        assertThat(updated.getTitle()).isEqualTo("Updated Title");
        assertThat(updated.getVisibility()).isEqualTo(io.gravitee.apim.core.portal_page.model.PortalVisibility.PRIVATE);
    }

    @Test
    void should_change_order() {
        // Given an existing PAGE item id from fixtures
        String navId = PAGE11_ID;

        // When: PUT with a new order that affects siblings
        BaseUpdatePortalNavigationItem payload = new UpdatePortalNavigationPage()
            .title("  Updated Title  ")
            .order(1) // Current order is 2 in fixtures, so moving to 1 should shift others
            .type(PortalNavigationItemType.PAGE)
            .published(true)
            .visibility(PortalVisibility.PUBLIC);
        Response response = target.path(navId).request().put(json(payload));

        // Then: 200 OK and response payload with updated order
        assertThat(response).hasStatus(OK_200);
        PortalNavigationPage body = response.readEntity(PortalNavigationPage.class);
        assertThat(body).isNotNull();
        assertThat(body.getId()).isEqualTo(UUID.fromString(navId));
        assertThat(body.getOrder()).isEqualTo(1);

        // And storage reflects the change
        var updated = portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, PortalNavigationItemId.of(navId));
        assertThat(updated.getOrder()).isEqualTo(1);
    }

    private void initStorageWith(List<io.gravitee.apim.core.portal_page.model.PortalNavigationItem> items) {
        items.forEach(i -> {
            i.setEnvironmentId(ENVIRONMENT);
            i.setOrganizationId(ORGANIZATION);
        });
        ((PortalNavigationItemsQueryServiceInMemory) portalNavigationItemsQueryService).initWith(items);
        ((PortalNavigationItemsCrudServiceInMemory) portalNavigationItemCrudService).initWith(items);
    }

    private BaseUpdatePortalNavigationItem updateFolderPublished(
        io.gravitee.apim.core.portal_page.model.PortalNavigationFolder folder,
        boolean published
    ) {
        return new UpdatePortalNavigationFolder()
            .title(folder.getTitle())
            .type(PortalNavigationItemType.FOLDER)
            .order(folder.getOrder())
            .published(published)
            .visibility(PortalVisibility.valueOf(folder.getVisibility().name()));
    }
}
