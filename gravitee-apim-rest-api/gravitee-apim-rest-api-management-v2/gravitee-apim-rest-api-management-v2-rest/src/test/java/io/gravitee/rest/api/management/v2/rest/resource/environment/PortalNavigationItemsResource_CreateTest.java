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
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static jakarta.ws.rs.client.Entity.json;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fixtures.PortalNavigationItemsFixtures;
import fixtures.core.model.PortalNavigationItemFixtures;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.portal_page.use_case.CreatePortalNavigationItemUseCase;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationApi;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationLink;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationPage;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemType;
import io.gravitee.rest.api.management.v2.rest.model.PortalVisibility;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationItemsResource_CreateTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";

    @Inject
    private CreatePortalNavigationItemUseCase createPortalNavigationItemUseCase;

    @Inject
    private EnvironmentService environmentService;

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

        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.UPDATE
            )
        ).thenReturn(true);
    }

    @AfterEach
    public void tearDown() {
        GraviteeContext.cleanContext();
        Mockito.reset(createPortalNavigationItemUseCase);
    }

    @Test
    void should_not_create_portal_navigation_items_when_no_permission() {
        // Given
        final var folder = PortalNavigationItemFixtures.aFolder(UUID.randomUUID().toString(), "My Folder");
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.UPDATE
            )
        ).thenReturn(false);

        // When
        Response response = target.request().post(json(folder));

        // Then
        assertThat(response).hasStatus(FORBIDDEN_403);
    }

    @Test
    void should_create_portal_navigation_page() {
        // Given
        final var page = PortalNavigationItemsFixtures.aCreatePortalNavigationPage();

        final var output = PortalNavigationItemsFixtures.aPortalNavigationPage(ORGANIZATION, ENVIRONMENT);
        when(createPortalNavigationItemUseCase.execute(any())).thenReturn(new CreatePortalNavigationItemUseCase.Output(output));

        // When
        Response response = target.request().post(json(page));

        // Then
        assertThat(response).hasStatus(CREATED_201);

        final var item = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.PortalNavigationPage.class);
        assertThat(item)
            .isNotNull()
            .hasFieldOrPropertyWithValue("id", page.getId())
            .hasFieldOrPropertyWithValue("title", page.getTitle())
            .hasFieldOrPropertyWithValue("type", PortalNavigationItemType.PAGE)
            .hasFieldOrPropertyWithValue("portalPageContentId", ((CreatePortalNavigationPage) page).getPortalPageContentId())
            .hasFieldOrPropertyWithValue("parentId", page.getParentId())
            .hasFieldOrPropertyWithValue("order", page.getOrder())
            .hasFieldOrPropertyWithValue("area", io.gravitee.rest.api.management.v2.rest.model.PortalArea.TOP_NAVBAR)
            .hasFieldOrPropertyWithValue("published", false)
            .hasFieldOrPropertyWithValue("visibility", io.gravitee.rest.api.management.v2.rest.model.PortalVisibility.PUBLIC);
    }

    @Test
    void should_create_portal_navigation_folder() {
        // Given
        final var folder = PortalNavigationItemsFixtures.aCreatePortalNavigationFolder();

        final var output = PortalNavigationItemsFixtures.aPortalNavigationFolder(ORGANIZATION, ENVIRONMENT);
        when(createPortalNavigationItemUseCase.execute(any())).thenReturn(new CreatePortalNavigationItemUseCase.Output(output));

        // When
        Response response = target.request().post(json(folder));

        // Then
        assertThat(response).hasStatus(CREATED_201);

        final var item = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.PortalNavigationFolder.class);
        assertThat(item)
            .isNotNull()
            .hasFieldOrPropertyWithValue("id", folder.getId())
            .hasFieldOrPropertyWithValue("title", folder.getTitle())
            .hasFieldOrPropertyWithValue("type", PortalNavigationItemType.FOLDER)
            .hasFieldOrPropertyWithValue("parentId", folder.getParentId())
            .hasFieldOrPropertyWithValue("order", folder.getOrder())
            .hasFieldOrPropertyWithValue("area", io.gravitee.rest.api.management.v2.rest.model.PortalArea.TOP_NAVBAR)
            .hasFieldOrPropertyWithValue("published", false)
            .hasFieldOrPropertyWithValue("visibility", io.gravitee.rest.api.management.v2.rest.model.PortalVisibility.PUBLIC);
    }

    @Test
    void should_create_portal_navigation_link() {
        // Given
        final var link = PortalNavigationItemsFixtures.aCreatePortalNavigationLink();

        final var output = PortalNavigationItemsFixtures.aPortalNavigationLink(ORGANIZATION, ENVIRONMENT);
        when(createPortalNavigationItemUseCase.execute(any())).thenReturn(new CreatePortalNavigationItemUseCase.Output(output));

        // When
        Response response = target.request().post(json(link));

        // Then
        assertThat(response).hasStatus(CREATED_201);

        final var item = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.PortalNavigationLink.class);
        assertThat(item)
            .isNotNull()
            .hasFieldOrPropertyWithValue("id", link.getId())
            .hasFieldOrPropertyWithValue("title", link.getTitle())
            .hasFieldOrPropertyWithValue("type", PortalNavigationItemType.LINK)
            .hasFieldOrPropertyWithValue("url", ((CreatePortalNavigationLink) link).getUrl())
            .hasFieldOrPropertyWithValue("parentId", link.getParentId())
            .hasFieldOrPropertyWithValue("order", link.getOrder())
            .hasFieldOrPropertyWithValue("area", io.gravitee.rest.api.management.v2.rest.model.PortalArea.TOP_NAVBAR)
            .hasFieldOrPropertyWithValue("published", false)
            .hasFieldOrPropertyWithValue("visibility", io.gravitee.rest.api.management.v2.rest.model.PortalVisibility.PUBLIC);
    }

    @Test
    void should_create_portal_navigation_api() {
        // Given
        final var api = PortalNavigationItemsFixtures.aCreatePortalNavigationApi();

        final var output = PortalNavigationItemsFixtures.aPortalNavigationApi(ORGANIZATION, ENVIRONMENT);
        when(createPortalNavigationItemUseCase.execute(any())).thenReturn(new CreatePortalNavigationItemUseCase.Output(output));

        // When
        Response response = target.request().post(json(api));

        // Then
        assertThat(response).hasStatus(CREATED_201);

        final var item = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.PortalNavigationApi.class);
        assertThat(item)
            .isNotNull()
            .hasFieldOrPropertyWithValue("id", api.getId())
            .hasFieldOrPropertyWithValue("title", api.getTitle())
            .hasFieldOrPropertyWithValue("type", PortalNavigationItemType.API)
            .hasFieldOrPropertyWithValue("apiId", ((CreatePortalNavigationApi) api).getApiId())
            .hasFieldOrPropertyWithValue("parentId", api.getParentId())
            .hasFieldOrPropertyWithValue("order", api.getOrder())
            .hasFieldOrPropertyWithValue("area", io.gravitee.rest.api.management.v2.rest.model.PortalArea.TOP_NAVBAR)
            .hasFieldOrPropertyWithValue("published", false)
            .hasFieldOrPropertyWithValue("visibility", io.gravitee.rest.api.management.v2.rest.model.PortalVisibility.PUBLIC);
    }

    @Test
    void should_fail_create_portal_navigation_api_when_api_not_found() {
        // Given
        final var api = PortalNavigationItemsFixtures.aCreatePortalNavigationApi();

        when(createPortalNavigationItemUseCase.execute(any())).thenThrow(new ApiNotFoundException("apiId"));

        // When
        Response response = target.request().post(json(api));

        // Then
        assertThat(response).hasStatus(NOT_FOUND_404);
    }

    @Test
    void should_check_visibility_when_portal_navigation_page_created_with_visibility_set_to_private() {
        // Given
        final var page = PortalNavigationItemsFixtures.aPrivateCreatePortalNavigationPage();

        final var output = PortalNavigationItemsFixtures.aPrivatePortalNavigationPage(ORGANIZATION, ENVIRONMENT);
        when(createPortalNavigationItemUseCase.execute(any())).thenReturn(new CreatePortalNavigationItemUseCase.Output(output));

        // When
        Response response = target.request().post(json(page));

        // Then
        assertThat(response).hasStatus(CREATED_201);

        final var item = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.PortalNavigationPage.class);
        assertThat(item)
            .isNotNull()
            .hasFieldOrPropertyWithValue("id", page.getId())
            .hasFieldOrPropertyWithValue("title", page.getTitle())
            .hasFieldOrPropertyWithValue("type", PortalNavigationItemType.PAGE)
            .hasFieldOrPropertyWithValue("portalPageContentId", ((CreatePortalNavigationPage) page).getPortalPageContentId())
            .hasFieldOrPropertyWithValue("parentId", page.getParentId())
            .hasFieldOrPropertyWithValue("order", page.getOrder())
            .hasFieldOrPropertyWithValue("area", io.gravitee.rest.api.management.v2.rest.model.PortalArea.TOP_NAVBAR)
            .hasFieldOrPropertyWithValue("published", false)
            .hasFieldOrPropertyWithValue("visibility", PortalVisibility.PRIVATE);
    }

    @Test
    void should_return_validation_error_when_portal_navigation_page_title_is_null() {
        // Given
        final var page = (CreatePortalNavigationPage) PortalNavigationItemsFixtures.aCreatePortalNavigationPage();
        page.setTitle(null);

        // When
        final var response = target.request().post(json(page));

        // Then
        assertThat(response).hasStatus(BAD_REQUEST_400);
        verifyNoInteractions(createPortalNavigationItemUseCase);
    }

    @Test
    void should_return_validation_error_when_portal_navigation_api_api_id_is_null() {
        // Given
        final var api = (CreatePortalNavigationApi) PortalNavigationItemsFixtures.aCreatePortalNavigationApi();
        api.setApiId(null);

        // When
        final var response = target.request().post(json(api));

        // Then
        assertThat(response).hasStatus(BAD_REQUEST_400);
        verifyNoInteractions(createPortalNavigationItemUseCase);
    }
}
