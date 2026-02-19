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
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemsResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationItemsResource_GetTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";

    @Inject
    private EnvironmentService environmentService;

    @Autowired
    private PortalNavigationItemsQueryServiceInMemory portalNavigationItemsQueryService;

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
    }

    @AfterEach
    public void tearDown() {
        GraviteeContext.cleanContext();
        portalNavigationItemsQueryService.reset();
    }

    @Test
    void should_return_portal_navigation_items() {
        // Given
        var items = PortalNavigationItemFixtures.sampleNavigationItems();
        items.forEach(item -> item.setEnvironmentId(ENVIRONMENT));
        portalNavigationItemsQueryService.initWith(items);

        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.READ
            )
        ).thenReturn(true);

        // When
        Response response = target.queryParam("area", PortalArea.TOP_NAVBAR).queryParam("loadChildren", true).request().get();

        // Then
        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(PortalNavigationItemsResponse.class)
            .satisfies(entity -> assertThat(entity.getItems()).hasSize(13));
    }

    @Test
    void should_return_portal_navigation_items_with_parent_id() {
        // Given
        var allItems = PortalNavigationItemFixtures.sampleNavigationItems();
        allItems.forEach(item -> item.setEnvironmentId(ENVIRONMENT));
        portalNavigationItemsQueryService.initWith(allItems);

        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.READ
            )
        ).thenReturn(true);

        // When
        Response response = target
            .queryParam("area", PortalArea.TOP_NAVBAR)
            .queryParam("parentId", PortalNavigationItemFixtures.APIS_ID)
            .queryParam("loadChildren", false)
            .request()
            .get();

        // Then
        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(PortalNavigationItemsResponse.class)
            .satisfies(entity -> assertThat(entity.getItems()).hasSize(4));
    }

    @Test
    void should_return_portal_navigation_items_without_loading_children() {
        // Given
        var items = PortalNavigationItemFixtures.sampleNavigationItems();
        items.forEach(item -> item.setEnvironmentId(ENVIRONMENT));
        portalNavigationItemsQueryService.initWith(items);

        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.READ
            )
        ).thenReturn(true);

        // When
        Response response = target.queryParam("area", PortalArea.TOP_NAVBAR).queryParam("loadChildren", false).request().get();

        // Then
        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(PortalNavigationItemsResponse.class)
            .satisfies(entity -> {
                // Should only return top-level items (parentId == null)
                var topLevelItems = items
                    .stream()
                    .filter(item -> item.getParentId() == null)
                    .toList();
                assertThat(entity.getItems()).hasSize(topLevelItems.size());
            });
    }

    @Test
    void should_return_portal_navigation_items_with_parent_id_and_loading_children() {
        // Given
        var items = PortalNavigationItemFixtures.sampleNavigationItems();
        items.forEach(item -> item.setEnvironmentId(ENVIRONMENT));
        portalNavigationItemsQueryService.initWith(items);

        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.READ
            )
        ).thenReturn(true);

        // When
        Response response = target
            .queryParam("area", PortalArea.TOP_NAVBAR)
            .queryParam("parentId", PortalNavigationItemFixtures.APIS_ID)
            .queryParam("loadChildren", true)
            .request()
            .get();

        // Then
        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(PortalNavigationItemsResponse.class)
            .satisfies(entity -> assertThat(entity.getItems()).hasSize(9));
    }

    @Test
    void should_not_return_portal_navigation_items_when_no_permission() {
        // Given
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.READ
            )
        ).thenReturn(false);

        // When
        Response response = target.queryParam("area", PortalArea.TOP_NAVBAR).request().get();

        // Then
        assertThat(response).hasStatus(FORBIDDEN_403);
    }

    @Test
    void should_use_TOP_NAVBAR_when_area_is_not_provided() {
        // Given
        var items = PortalNavigationItemFixtures.sampleNavigationItems();
        items.forEach(item -> item.setEnvironmentId(ENVIRONMENT));
        portalNavigationItemsQueryService.initWith(items);

        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.READ
            )
        ).thenReturn(true);

        // When
        Response response = target.queryParam("loadChildren", true).request().get();

        // Then
        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(PortalNavigationItemsResponse.class)
            .satisfies(entity -> assertThat(entity.getItems()).isNotEmpty());
    }

    @Test
    void should_return_200_when_valid_area_is_provided() {
        // Given
        var items = PortalNavigationItemFixtures.sampleNavigationItems();
        items.forEach(item -> item.setEnvironmentId(ENVIRONMENT));
        portalNavigationItemsQueryService.initWith(items);

        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.READ
            )
        ).thenReturn(true);

        // When
        Response response = target.queryParam("area", "TOP_NAVBAR").queryParam("loadChildren", true).request().get();

        // Then
        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(PortalNavigationItemsResponse.class)
            .satisfies(entity -> assertThat(entity.getItems()).isNotEmpty());
    }

    @Test
    void should_return_400_when_invalid_area_is_provided() {
        // Given
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.READ
            )
        ).thenReturn(true);

        // When
        Response response = target.queryParam("area", "invalidArea").queryParam("loadChildren", true).request().get();

        // Then
        assertThat(response).hasStatus(400);

        Map<String, Object> body = response.readEntity(new GenericType<>() {});

        assertThat(body)
            .containsKey("message")
            .extractingByKey("message")
            .asString()
            .contains("Invalid value")
            .contains("invalidArea")
            .contains("area");
    }
}
