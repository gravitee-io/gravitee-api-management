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
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
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
import org.springframework.beans.factory.annotation.Autowired;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiDocumentationNavigationItemResource_GetTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";
    private static final String API_ID = "api-id";
    private static final String OTHER_API_ID = "other-api-id";

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
    }

    @AfterEach
    public void cleanUp() {
        GraviteeContext.cleanContext();
        portalNavigationItemsQueryService.reset();
    }

    @Test
    void should_get_an_item() {
        var page = aPageOwnedBy(API_ID);
        portalNavigationItemsQueryService.initWith(List.of(page));

        Response response = rootTarget(page.getId().json()).request().get();

        assertThat(response).hasStatus(OK_200);
        assertThat(response.readEntity(PortalNavigationPage.class))
            .hasFieldOrPropertyWithValue("id", page.getId().id())
            .hasFieldOrPropertyWithValue("title", "Overview")
            .hasFieldOrPropertyWithValue("published", false)
            .hasFieldOrPropertyWithValue("parentId", null);
    }

    @Test
    void should_not_return_the_secrets_of_a_source() {
        var source = PortalNavigationItemSource.builder()
            .sourceType("github")
            .sourceConfiguration(PortalNavigationItemSourceDomainServiceInMemory.SENSITIVE_DATA)
            .build();
        var page = aPageOwnedBy(API_ID).toBuilder().source(source).build();
        portalNavigationItemsQueryService.initWith(List.of(page));

        Response response = rootTarget(page.getId().json()).request().get();

        assertThat(response).hasStatus(OK_200);
        assertThat(response.readEntity(String.class)).doesNotContain(PortalNavigationItemSourceDomainServiceInMemory.SENSITIVE_DATA);
    }

    @Test
    void should_return_404_for_an_unknown_item() {
        Response response = rootTarget(PortalNavigationItemId.random().json()).request().get();

        assertThat(response).hasStatus(NOT_FOUND_404);
    }

    @Test
    void should_reject_an_item_id_belonging_to_another_api() {
        var foreign = aPageOwnedBy(OTHER_API_ID);
        portalNavigationItemsQueryService.initWith(List.of(foreign));

        Response response = rootTarget(foreign.getId().json()).request().get();

        assertThat(response).hasStatus(NOT_FOUND_404);
    }

    @Test
    void should_reject_a_portal_owned_item() {
        var section = PortalNavigationItemFixtures.aFolder("APIs");
        section.setEnvironmentId(ENVIRONMENT);
        portalNavigationItemsQueryService.initWith(List.of(section));

        Response response = rootTarget(section.getId().json()).request().get();

        assertThat(response).hasStatus(NOT_FOUND_404);
    }

    @Test
    void should_return_403_with_environment_documentation_permission_only() {
        var page = aPageOwnedBy(API_ID);
        portalNavigationItemsQueryService.initWith(List.of(page));
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                eq(API_ID),
                eq(RolePermissionAction.READ)
            )
        ).thenReturn(false);

        Response response = rootTarget(page.getId().json()).request().get();

        assertThat(response).hasStatus(FORBIDDEN_403);
    }

    @Test
    void should_succeed_with_api_documentation_permission_only() {
        var page = aPageOwnedBy(API_ID);
        portalNavigationItemsQueryService.initWith(List.of(page));
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                eq(API_ID),
                eq(RolePermissionAction.READ)
            )
        ).thenReturn(true);

        Response response = rootTarget(page.getId().json()).request().get();

        assertThat(response).hasStatus(OK_200);
    }

    private static io.gravitee.apim.core.portal_page.model.PortalNavigationPage aPageOwnedBy(String apiId) {
        var page = PortalNavigationItemFixtures.aPage("Overview", null)
            .toBuilder()
            .reference(new NavigationItemReference.ApiReference(apiId))
            .published(false)
            .build();
        page.setEnvironmentId(ENVIRONMENT);
        return page;
    }
}
