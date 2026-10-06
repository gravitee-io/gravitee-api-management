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
import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference.ApiReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.rest.api.management.v2.rest.model.BaseUpdatePortalNavigationItem;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationApiProduct;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationFolder;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemSource;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemType;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemsResponse;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationLink;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationPage;
import io.gravitee.rest.api.management.v2.rest.model.PortalVisibility;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationApi;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationApiProduct;
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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

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
    void should_preserve_api_owned_page_when_updating_with_its_rendered_parent() {
        var api = PortalNavigationItemFixtures.anApi();
        var otherApiEntry = PortalNavigationItemFixtures.anApi(
            "20000000-0000-4000-8000-000000000031",
            "Another API entry",
            null,
            api.getApiId()
        );
        var reference = new ApiReference(api.getApiId());
        var page = PortalNavigationItemFixtures.aPage("API documentation", null).toBuilder().reference(reference).build();
        page.markAsRoot();
        initStorageWith(List.of(api, otherApiEntry, page));
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.READ
            )
        ).thenReturn(true);

        var getResponse = target.queryParam("area", api.getArea()).queryParam("parentId", api.getId().id()).request().get();
        assertThat(getResponse).hasStatus(OK_200);
        var items = getResponse.readEntity(PortalNavigationItemsResponse.class).getItems();
        assertThat(items).hasSize(1);
        var renderedPage = (PortalNavigationPage) items.getFirst().getActualInstance();
        assertThat(renderedPage.getParentId()).isEqualTo(api.getId().id());

        var payload = new UpdatePortalNavigationPage()
            .title("Updated API documentation")
            .type(PortalNavigationItemType.PAGE)
            .order(renderedPage.getOrder())
            .parentId(renderedPage.getParentId())
            .published(renderedPage.getPublished())
            .visibility(renderedPage.getVisibility());
        var response = target.path(page.getId().json()).request().put(json(payload));

        assertThat(response).hasStatus(OK_200);
        var updated = portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, page.getId());
        assertThat(updated.getTitle()).isEqualTo("Updated API documentation");
        assertThat(updated.getReference()).isEqualTo(reference);
        assertThat(updated.getParentId()).isNull();
        assertThat(updated.getRootId()).isEqualTo(page.getId());

        for (var navigationEntry : List.of(api, otherApiEntry)) {
            var refreshedResponse = target
                .queryParam("area", navigationEntry.getArea())
                .queryParam("parentId", navigationEntry.getId().id())
                .request()
                .get();
            assertThat(refreshedResponse).hasStatus(OK_200);
            var refreshedItems = refreshedResponse.readEntity(PortalNavigationItemsResponse.class).getItems();
            assertThat(refreshedItems).hasSize(1);
            var refreshedPage = (PortalNavigationPage) refreshedItems.getFirst().getActualInstance();
            assertThat(refreshedPage.getId()).isEqualTo(page.getId().id());
            assertThat(refreshedPage.getTitle()).isEqualTo("Updated API documentation");
            assertThat(refreshedPage.getParentId()).isEqualTo(navigationEntry.getId().id());
        }
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
    void should_replace_api_product_category_ids() {
        var existingCategoryId = PortalCategoryId.random();
        var replacementCategoryIds = List.of(PortalCategoryId.random(), PortalCategoryId.random());
        var parent = PortalNavigationItemFixtures.aFolder(APIS_ID, "APIs");
        parent.markAsRoot();
        var apiProduct = PortalNavigationItemFixtures.anApiProduct().toBuilder().categoryIds(List.of(existingCategoryId)).build();
        apiProduct.updateParent(parent);
        initStorageWith(List.of(parent, apiProduct));

        var payload = new UpdatePortalNavigationApiProduct().categoryIds(
            replacementCategoryIds.stream().map(PortalCategoryId::id).toList()
        );
        payload
            .title(apiProduct.getTitle())
            .type(PortalNavigationItemType.API_PRODUCT)
            .order(apiProduct.getOrder())
            .published(apiProduct.getPublished())
            .parentId(parent.getId().id())
            .visibility(PortalVisibility.PUBLIC);

        Response response = target.path(apiProduct.getId().toString()).request().put(json(payload));

        assertThat(response).hasStatus(OK_200);
        PortalNavigationApiProduct body = response.readEntity(PortalNavigationApiProduct.class);
        assertThat(body.getCategoryIds()).containsExactlyElementsOf(replacementCategoryIds.stream().map(PortalCategoryId::id).toList());

        var updated = portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, apiProduct.getId());
        assertThat(updated).hasFieldOrPropertyWithValue("categoryIds", replacementCategoryIds);
    }

    @Test
    void should_reset_api_product_category_ids_when_omitted() {
        var parent = PortalNavigationItemFixtures.aFolder(APIS_ID, "APIs");
        parent.markAsRoot();
        var apiProduct = PortalNavigationItemFixtures.anApiProduct().toBuilder().categoryIds(List.of(PortalCategoryId.random())).build();
        apiProduct.updateParent(parent);
        initStorageWith(List.of(parent, apiProduct));

        var payload = new UpdatePortalNavigationApiProduct()
            .title(apiProduct.getTitle())
            .type(PortalNavigationItemType.API_PRODUCT)
            .order(apiProduct.getOrder())
            .published(apiProduct.getPublished())
            .parentId(parent.getId().id())
            .visibility(PortalVisibility.PUBLIC);

        Response response = target.path(apiProduct.getId().toString()).request().put(json(payload));

        assertThat(response).hasStatus(OK_200);
        PortalNavigationApiProduct body = response.readEntity(PortalNavigationApiProduct.class);
        assertThat(body.getCategoryIds()).isEmpty();

        var updated = portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, apiProduct.getId());
        assertThat(updated).hasFieldOrPropertyWithValue("categoryIds", List.of());
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
    void should_publish_api_owned_and_physical_documentation_when_propagation_query_param_is_true() {
        var parent = PortalNavigationItemFixtures.aFolder("APIs");
        parent.markAsRoot();
        var api = PortalNavigationItemFixtures.anApi();
        api.setPublished(false);
        api.updateParent(parent);
        var reference = new ApiReference(api.getApiId());
        var ownedFolder = PortalNavigationItemFixtures.aFolder("API guide").toBuilder().reference(reference).published(false).build();
        ownedFolder.markAsRoot();
        var ownedPage = PortalNavigationItemFixtures.aPage("Automation page", ownedFolder.getId())
            .toBuilder()
            .reference(reference)
            .published(false)
            .build();
        ownedPage.updateParent(ownedFolder);
        var physicalPage = PortalNavigationItemFixtures.aPage("Portal page", api.getId()).toBuilder().published(false).build();
        physicalPage.updateParent(api);
        initStorageWith(List.of(parent, api, ownedFolder, ownedPage, physicalPage));
        var expectedFolder = ownedFolder.toBuilder().published(true).build();
        var expectedOwnedPage = ownedPage.toBuilder().published(true).build();
        var expectedPhysicalPage = physicalPage.toBuilder().published(true).build();

        var response = target
            .path(api.getId().json())
            .queryParam("propagatePublishToChildren", true)
            .request()
            .put(json(updateApiPublished(api, true)));

        assertThat(response).hasStatus(OK_200);
        assertThat(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, api.getId()).getPublished()).isTrue();
        assertThat(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, ownedFolder.getId()))
            .usingRecursiveComparison()
            .isEqualTo(expectedFolder);
        assertThat(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, ownedPage.getId()))
            .usingRecursiveComparison()
            .isEqualTo(expectedOwnedPage);
        assertThat(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, physicalPage.getId()))
            .usingRecursiveComparison()
            .isEqualTo(expectedPhysicalPage);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = false)
    void should_publish_only_selected_api_when_propagation_is_omitted_or_false(Boolean propagate) {
        var parent = PortalNavigationItemFixtures.aFolder("APIs");
        parent.markAsRoot();
        var api = PortalNavigationItemFixtures.anApi();
        api.setPublished(false);
        api.updateParent(parent);
        var ownedPage = PortalNavigationItemFixtures.aPage("Automation page", null)
            .toBuilder()
            .reference(new ApiReference(api.getApiId()))
            .published(false)
            .build();
        ownedPage.markAsRoot();
        var physicalPage = PortalNavigationItemFixtures.aPage("Portal page", api.getId()).toBuilder().published(false).build();
        physicalPage.updateParent(api);
        initStorageWith(List.of(parent, api, ownedPage, physicalPage));

        var requestTarget = target.path(api.getId().json());
        if (propagate != null) {
            requestTarget = requestTarget.queryParam("propagatePublishToChildren", propagate);
        }
        var response = requestTarget.request().put(json(updateApiPublished(api, true)));

        assertThat(response).hasStatus(OK_200);
        assertThat(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, api.getId()).getPublished()).isTrue();
        assertThat(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, ownedPage.getId()).getPublished()).isFalse();
        assertThat(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, physicalPage.getId()).getPublished()).isFalse();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = false)
    void should_unpublish_standalone_api_documentation_without_affecting_api_product_documentation(Boolean propagate) {
        var parent = PortalNavigationItemFixtures.aFolder("APIs");
        parent.markAsRoot();
        var api = PortalNavigationItemFixtures.anApi();
        api.updateParent(parent);
        var ownedPage = PortalNavigationItemFixtures.aPage("Automation page", null)
            .toBuilder()
            .reference(new ApiReference(api.getApiId()))
            .build();
        ownedPage.markAsRoot();
        var physicalPage = PortalNavigationItemFixtures.aPage("Portal page", api.getId());
        physicalPage.updateParent(api);
        var product = PortalNavigationItemFixtures.anApiProduct();
        product.updateParent(parent);
        var productFolder = PortalNavigationItemFixtures.aFolder("Product folder", product.getId());
        productFolder.updateParent(product);
        var productApi = PortalNavigationItemFixtures.anApi(
            "20000000-0000-4000-8000-000000000032",
            "Product API",
            productFolder.getId(),
            api.getApiId()
        );
        productApi.updateParent(productFolder);
        var productPage = PortalNavigationItemFixtures.aPage("Product page", productApi.getId());
        productPage.updateParent(productApi);
        initStorageWith(List.of(parent, api, ownedPage, physicalPage, product, productFolder, productApi, productPage));

        var requestTarget = target.path(api.getId().json());
        if (propagate != null) {
            requestTarget = requestTarget.queryParam("propagatePublishToChildren", propagate);
        }
        var response = requestTarget.request().put(json(updateApiPublished(api, false)));

        assertThat(response).hasStatus(OK_200);
        assertThat(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, api.getId()).getPublished()).isFalse();
        assertThat(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, ownedPage.getId()).getPublished()).isFalse();
        assertThat(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, physicalPage.getId()).getPublished()).isFalse();
        assertThat(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, product.getId()).getPublished()).isTrue();
        assertThat(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, productApi.getId()).getPublished()).isTrue();
        assertThat(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, productPage.getId()).getPublished()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void should_not_propagate_unchanged_api_publication_status(boolean published) {
        var parent = PortalNavigationItemFixtures.aFolder("APIs");
        parent.markAsRoot();
        var api = PortalNavigationItemFixtures.anApi();
        api.setPublished(published);
        api.updateParent(parent);
        var ownedPage = PortalNavigationItemFixtures.aPage("Automation page", null)
            .toBuilder()
            .reference(new ApiReference(api.getApiId()))
            .published(!published)
            .build();
        ownedPage.markAsRoot();
        var physicalPage = PortalNavigationItemFixtures.aPage("Portal page", api.getId()).toBuilder().published(!published).build();
        physicalPage.updateParent(api);
        initStorageWith(List.of(parent, api, ownedPage, physicalPage));

        var response = target
            .path(api.getId().json())
            .queryParam("propagatePublishToChildren", true)
            .request()
            .put(json(updateApiPublished(api, published)));

        assertThat(response).hasStatus(OK_200);
        assertThat(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, ownedPage.getId()).getPublished()).isEqualTo(
            !published
        );
        assertThat(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, physicalPage.getId()).getPublished()).isEqualTo(
            !published
        );
    }

    @Test
    void should_unpublish_api_in_product_folder_without_unpublishing_api_owned_documentation() {
        var parent = PortalNavigationItemFixtures.aFolder("APIs");
        parent.markAsRoot();
        var product = PortalNavigationItemFixtures.anApiProduct();
        product.updateParent(parent);
        var productFolder = PortalNavigationItemFixtures.aFolder("Product folder", product.getId());
        productFolder.updateParent(product);
        var api = PortalNavigationItemFixtures.anApi();
        api.updateParent(productFolder);
        var ownedPage = PortalNavigationItemFixtures.aPage("Automation page", null)
            .toBuilder()
            .reference(new ApiReference(api.getApiId()))
            .build();
        ownedPage.markAsRoot();
        var physicalPage = PortalNavigationItemFixtures.aPage("Product page", api.getId());
        physicalPage.updateParent(api);
        initStorageWith(List.of(parent, product, productFolder, api, ownedPage, physicalPage));

        var payload = new UpdatePortalNavigationFolder()
            .title(productFolder.getTitle())
            .parentId(product.getId().id())
            .type(PortalNavigationItemType.FOLDER)
            .order(productFolder.getOrder())
            .published(false)
            .visibility(PortalVisibility.PUBLIC);
        var response = target.path(productFolder.getId().json()).request().put(json(payload));

        assertThat(response).hasStatus(OK_200);
        assertThat(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, api.getId()).getPublished()).isFalse();
        assertThat(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, physicalPage.getId()).getPublished()).isFalse();
        assertThat(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, ownedPage.getId()).getPublished()).isTrue();
        assertThat(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, product.getId()).getPublished()).isTrue();
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
        assertThat(updated.getVisibility()).isEqualTo(io.gravitee.apim.core.portal.model.PortalVisibility.PRIVATE);
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

    @Test
    void should_add_source_to_page_and_ignore_client_provided_fetch_state() {
        // Given an existing PAGE item
        String navId = PAGE11_ID;

        // When: PUT with a source carrying forged server-managed fields (only reachable through the
        // @JsonCreator constructor, exactly how Jackson materializes them from a request body)
        BaseUpdatePortalNavigationItem payload = new UpdatePortalNavigationPage()
            .source(
                new PortalNavigationItemSource(
                    OffsetDateTime.parse("2026-07-17T10:00:00Z"),
                    OffsetDateTime.parse("2026-07-17T11:00:00Z"),
                    "forged error",
                    true
                )
                    .type("github-fetcher")
                    .configuration(Map.of("repository", "docs"))
                    .useAutoFetch(true)
                    .fetchCron("0 */10 * * * *")
            )
            .title("My Page")
            .order(1)
            .type(PortalNavigationItemType.PAGE)
            .published(true)
            .visibility(PortalVisibility.PUBLIC);
        Response response = target.path(navId).request().put(json(payload));

        // Then: 200 OK, the source is applied but the server-managed fetch state is not
        assertThat(response).hasStatus(OK_200);
        PortalNavigationPage body = response.readEntity(PortalNavigationPage.class);
        assertThat(body.getSource()).isNotNull();
        assertThat(body.getSource().getType()).isEqualTo("github-fetcher");
        assertThat(body.getSource().getConfiguration()).isEqualTo(Map.of("repository", "docs"));
        assertThat(body.getSource().getUseAutoFetch()).isTrue();
        assertThat(body.getSource().getLastFetchedAt()).isNull();
        assertThat(body.getSource().getLastFetchAttemptAt()).isNull();
        assertThat(body.getSource().getLastFetchError()).isNull();
        assertThat(body.getSource().getSubtreeImport()).isFalse();

        // And storage reflects the same
        var updated = portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, PortalNavigationItemId.of(navId));
        assertThat(updated.getSource()).isNotNull();
        assertThat(updated.getSource().getSourceType()).isEqualTo("github-fetcher");
        assertThat(updated.getSource().getLastFetchedAt()).isNull();
        assertThat(updated.getSource().getLastFetchAttemptAt()).isNull();
        assertThat(updated.getSource().getLastFetchError()).isNull();
        assertThat(updated.getSource().isSubtreeImport()).isFalse();
    }

    @Test
    void should_preserve_server_managed_fetch_state_when_updating_a_sourced_page() {
        // Given a PAGE whose source has already been fetched by the server
        var sourcedPage = PortalNavigationItemFixtures.aPage("20000000-0000-4000-8000-000000000030", "Sourced Page", null)
            .toBuilder()
            .source(
                io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource.builder()
                    .sourceType("github-fetcher")
                    // stored exactly as the API serializes it, so the PUT below targets the same origin;
                    // hard-coded on purpose: a serialization format change must break this test, as it
                    // would reset the fetch state of every already-stored page on its first PUT
                    .sourceConfiguration(
                        """
                        {
                          "repository" : "docs"
                        }"""
                    )
                    .lastFetchedAt(Instant.parse("2026-07-17T10:00:00Z"))
                    .lastFetchAttemptAt(Instant.parse("2026-07-17T11:00:00Z"))
                    .lastFetchError("boom")
                    .build()
            )
            .build();
        sourcedPage.markAsRoot();
        initStorageWith(List.of(sourcedPage));

        // When: PUT keeping the same source origin (clients never send the readOnly fields back)
        BaseUpdatePortalNavigationItem payload = new UpdatePortalNavigationPage()
            .source(
                new PortalNavigationItemSource(null, null, null, null).type("github-fetcher").configuration(Map.of("repository", "docs"))
            )
            .title("Sourced Page")
            .order(0)
            .type(PortalNavigationItemType.PAGE)
            .published(true)
            .visibility(PortalVisibility.PUBLIC);
        Response response = target.path(sourcedPage.getId().toString()).request().put(json(payload));

        // Then: 200 OK and the server-managed fetch state survives the update
        assertThat(response).hasStatus(OK_200);
        PortalNavigationPage body = response.readEntity(PortalNavigationPage.class);
        assertThat(body.getSource()).isNotNull();
        assertThat(body.getSource().getLastFetchedAt()).isEqualTo(OffsetDateTime.parse("2026-07-17T10:00:00Z"));
        assertThat(body.getSource().getLastFetchAttemptAt()).isEqualTo(OffsetDateTime.parse("2026-07-17T11:00:00Z"));
        assertThat(body.getSource().getLastFetchError()).isEqualTo("boom");

        var updated = portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENVIRONMENT, sourcedPage.getId());
        assertThat(updated.getSource()).isNotNull();
        assertThat(updated.getSource().getLastFetchedAt()).isEqualTo(Instant.parse("2026-07-17T10:00:00Z"));
        assertThat(updated.getSource().getLastFetchAttemptAt()).isEqualTo(Instant.parse("2026-07-17T11:00:00Z"));
        assertThat(updated.getSource().getLastFetchError()).isEqualTo("boom");
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

    private BaseUpdatePortalNavigationItem updateApiPublished(PortalNavigationApi api, boolean published) {
        return new UpdatePortalNavigationApi()
            .apiId(api.getApiId())
            .title(api.getTitle())
            .type(PortalNavigationItemType.API)
            .parentId(api.getParentId().id())
            .order(api.getOrder())
            .published(published)
            .visibility(PortalVisibility.valueOf(api.getVisibility().name()));
    }
}
