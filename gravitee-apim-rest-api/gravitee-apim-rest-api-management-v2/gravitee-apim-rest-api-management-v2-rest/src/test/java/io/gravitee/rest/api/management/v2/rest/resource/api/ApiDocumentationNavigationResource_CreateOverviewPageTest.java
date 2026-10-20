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
import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import inmemory.PortalNavigationItemsCrudServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationPage;
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
class ApiDocumentationNavigationResource_CreateOverviewPageTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";
    private static final String API_ID = "api-id";

    @Inject
    private EnvironmentService environmentService;

    @Autowired
    private PortalNavigationItemsCrudServiceInMemory portalNavigationItemCrudService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API_ID + "/portal-navigation-items/_overview-page";
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
        portalNavigationItemCrudService.reset();
        portalPageContentCrudService.reset();
        apiCrudService.reset();
    }

    @Test
    void should_create_an_unpublished_overview_page_owned_by_the_api() {
        Response response = createOverviewPage();

        assertThat(response).hasStatus(CREATED_201);
        var page = response.readEntity(PortalNavigationPage.class);
        assertThat(response.getLocation().getPath()).endsWith("/apis/" + API_ID + "/portal-navigation-items/" + page.getId());
        assertThat(page.getTitle()).isEqualTo("Overview");
        assertThat(page.getPublished()).isFalse();
        assertThat(page.getParentId()).isNull();
        assertThat(portalNavigationItemCrudService.storage())
            .singleElement()
            .satisfies(stored -> {
                assertThat(stored.getId().id()).isEqualTo(page.getId());
                assertThat(stored.getReference()).isEqualTo(new NavigationItemReference.ApiReference(API_ID));
            });
        assertThat(portalPageContentCrudService.storage()).hasSize(1);
    }

    @Test
    void should_return_403_without_api_documentation_create_permission() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                eq(API_ID),
                eq(RolePermissionAction.CREATE)
            )
        ).thenReturn(false);

        Response response = createOverviewPage();

        assertThat(response).hasStatus(FORBIDDEN_403);
        assertThat(portalNavigationItemCrudService.storage()).isEmpty();
    }

    @Test
    void should_succeed_with_api_documentation_create_permission_only() {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                eq(API_ID),
                eq(RolePermissionAction.CREATE)
            )
        ).thenReturn(true);

        Response response = createOverviewPage();

        assertThat(response).hasStatus(CREATED_201);
    }

    private Response createOverviewPage() {
        return rootTarget().request().post(Entity.json(""));
    }
}
