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
import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static jakarta.ws.rs.client.Entity.json;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fixtures.PortalNavigationItemsFixtures;
import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.use_case.CreatePortalNavigationItemUseCase;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationPage;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiDocumentationNavigationResource_CreateTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";
    private static final String API_ID = "api-id";
    private static final String OTHER_API_ID = "other-api-id";

    @Inject
    private CreatePortalNavigationItemUseCase createPortalNavigationItemUseCase;

    @Inject
    private EnvironmentService environmentService;

    @Autowired
    private PortalNavigationItemsQueryServiceInMemory portalNavigationItemsQueryService;

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

        apiCrudService.initWith(List.of(Api.builder().id(API_ID).name("My API").environmentId(ENVIRONMENT).build()));

        when(createPortalNavigationItemUseCase.execute(any())).thenReturn(
            new CreatePortalNavigationItemUseCase.Output(PortalNavigationItemsFixtures.aPortalNavigationPage(ORGANIZATION, ENVIRONMENT))
        );
    }

    @AfterEach
    public void cleanUp() {
        GraviteeContext.cleanContext();
        apiCrudService.reset();
        portalNavigationItemsQueryService.reset();
        Mockito.reset(createPortalNavigationItemUseCase);
    }

    @Test
    void should_return_canonical_api_documentation_from_the_use_case() {
        var page = PortalNavigationItemsFixtures.aCreatePortalNavigationPage().parentId(null);
        var output = PortalNavigationItemFixtures.aPage(page.getId().toString(), page.getTitle(), null)
            .toBuilder()
            .reference(new NavigationItemReference.ApiReference(API_ID))
            .organizationId(ORGANIZATION)
            .environmentId(ENVIRONMENT)
            .published(false)
            .build();
        output.markAsRoot();
        when(createPortalNavigationItemUseCase.execute(any())).thenReturn(new CreatePortalNavigationItemUseCase.Output(output));

        Response response = rootTarget().request().post(json(page));

        assertThat(response).hasStatus(CREATED_201);
        var item = response.readEntity(PortalNavigationPage.class);
        assertThat(item.getTitle()).isEqualTo(page.getTitle());
        assertThat(item.getId()).isEqualTo(output.getId().id());
        assertThat(item.getParentId()).isNull();
        assertThat(item.getRootId()).isEqualTo(output.getId().id());
        assertThat(item.getPortalPageContentId()).isEqualTo(output.getPortalPageContentId().id());
    }

    @Test
    void should_create_an_item_owned_by_the_api_in_the_url_and_unpublished() {
        var page = PortalNavigationItemsFixtures.aCreatePortalNavigationPage().parentId(null);

        rootTarget().request().post(json(page));

        var item = capturedItem();
        assertThat(item.getReference()).isEqualTo(new NavigationItemReference.ApiReference(API_ID));
        assertThat(item.getParentId()).isNull();
        assertThat(item.getRenderedParentId()).isNull();
        assertThat(item.getPublished()).isFalse();
        assertThat(item.getArea()).isEqualTo(PortalArea.TOP_NAVBAR);
    }

    @Test
    void should_ignore_the_page_content_id_of_the_request_so_that_no_existing_content_can_be_attached() {
        var page = PortalNavigationItemsFixtures.aCreatePortalNavigationPage().parentId(null);

        Response response = rootTarget().request().post(json(page));

        assertThat(response).hasStatus(CREATED_201);
        assertThat(capturedItem().getPortalPageContentId()).isNull();
    }

    @Test
    void should_create_an_item_under_a_folder_of_the_same_api() {
        var folder = aFolderOwnedBy(API_ID);
        portalNavigationItemsQueryService.initWith(List.of(folder));
        var page = PortalNavigationItemsFixtures.aCreatePortalNavigationPage().parentId(folder.getId().id());

        Response response = rootTarget().request().post(json(page));

        assertThat(response).hasStatus(CREATED_201);
        assertThat(capturedItem().getParentId()).isEqualTo(folder.getId());
    }

    @Test
    void should_reject_a_parent_belonging_to_another_api() {
        var foreignFolder = aFolderOwnedBy(OTHER_API_ID);
        portalNavigationItemsQueryService.initWith(List.of(foreignFolder));
        var page = PortalNavigationItemsFixtures.aCreatePortalNavigationPage().parentId(foreignFolder.getId().id());

        Response response = rootTarget().request().post(json(page));

        assertThat(response).hasStatus(NOT_FOUND_404);
        verifyNoInteractions(createPortalNavigationItemUseCase);
    }

    @Test
    void should_reject_a_portal_owned_parent() {
        var section = PortalNavigationItemFixtures.aFolder("APIs");
        section.setEnvironmentId(ENVIRONMENT);
        portalNavigationItemsQueryService.initWith(List.of(section));
        var page = PortalNavigationItemsFixtures.aCreatePortalNavigationPage().parentId(section.getId().id());

        Response response = rootTarget().request().post(json(page));

        assertThat(response).hasStatus(NOT_FOUND_404);
        verifyNoInteractions(createPortalNavigationItemUseCase);
    }

    @Test
    void should_reject_an_item_of_type_api() {
        var api = PortalNavigationItemsFixtures.aCreatePortalNavigationApi().parentId(null);

        Response response = rootTarget().request().post(json(api));

        assertThat(response).hasStatus(BAD_REQUEST_400);
        verifyNoInteractions(createPortalNavigationItemUseCase);
    }

    @Test
    void should_return_403_with_environment_documentation_permission_only() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                eq(API_ID),
                eq(RolePermissionAction.CREATE)
            )
        ).thenReturn(false);

        Response response = rootTarget().request().post(json(PortalNavigationItemsFixtures.aCreatePortalNavigationPage().parentId(null)));

        assertThat(response).hasStatus(FORBIDDEN_403);
        verifyNoInteractions(createPortalNavigationItemUseCase);
    }

    @Test
    void should_succeed_with_api_documentation_permission_only() {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                eq(API_ID),
                eq(RolePermissionAction.CREATE)
            )
        ).thenReturn(true);

        Response response = rootTarget().request().post(json(PortalNavigationItemsFixtures.aCreatePortalNavigationPage().parentId(null)));

        assertThat(response).hasStatus(CREATED_201);
    }

    private io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem capturedItem() {
        var captor = ArgumentCaptor.forClass(CreatePortalNavigationItemUseCase.Input.class);
        verify(createPortalNavigationItemUseCase).execute(captor.capture());
        return captor.getValue().item();
    }

    private static PortalNavigationFolder aFolderOwnedBy(String apiId) {
        var folder = PortalNavigationItemFixtures.aFolder(PortalNavigationItemId.random().json(), "Auth")
            .toBuilder()
            .reference(new NavigationItemReference.ApiReference(apiId))
            .build();
        folder.setEnvironmentId(ENVIRONMENT);
        return folder;
    }
}
