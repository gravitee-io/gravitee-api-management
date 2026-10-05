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
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.rest.api.management.v2.rest.model.ApiPortalNavigationItemsResponse;
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
class ApiDocumentationNavigationResource_ListTest extends AbstractResourceTest {

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
    void should_list_the_api_documentation() {
        var folder = inEnvironment(PortalNavigationItemFixtures.aFolder("Auth").toBuilder().reference(ownedBy(API_ID)).build());
        var page = inEnvironment(
            PortalNavigationItemFixtures.aPage("Setup", folder.getId()).toBuilder().reference(ownedBy(API_ID)).build()
        );
        var section = inEnvironment(PortalNavigationItemFixtures.aFolder("APIs"));
        var listing = inEnvironment(
            PortalNavigationItemFixtures.anApi(PortalNavigationItemId.random().json(), "My API", section.getId(), API_ID)
        );
        portalNavigationItemsQueryService.initWith(List.of(folder, page, section, listing));

        Response response = rootTarget().request().get();

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiPortalNavigationItemsResponse.class)
            .satisfies(entity -> {
                assertThat(entity.getItems())
                    .extracting(item ->
                        ((io.gravitee.rest.api.management.v2.rest.model.BasePortalNavigationItem) item.getActualInstance()).getId()
                    )
                    .containsExactlyInAnyOrder(folder.getId().id(), page.getId().id());
                assertThat(entity.getPublication().getNavigationItemId()).isEqualTo(listing.getId().id());
                assertThat(entity.getPublication().getSectionId()).isEqualTo(section.getId().id());
                assertThat(entity.getPublication().getSectionName()).isEqualTo("APIs");
                assertThat(entity.getPublication().getPublished()).isTrue();
            });
    }

    @Test
    void should_return_an_empty_list_and_no_publication_for_an_api_with_no_documentation() {
        var foreign = inEnvironment(
            PortalNavigationItemFixtures.aPage("Other overview", null).toBuilder().reference(ownedBy(OTHER_API_ID)).build()
        );
        portalNavigationItemsQueryService.initWith(List.of(foreign));

        Response response = rootTarget().request().get();

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiPortalNavigationItemsResponse.class)
            .satisfies(entity -> {
                assertThat(entity.getItems()).isEmpty();
                assertThat(entity.getPublication()).isNull();
            });
    }

    @Test
    void should_return_403_with_environment_documentation_permission_only() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                eq(API_ID),
                eq(RolePermissionAction.READ)
            )
        ).thenReturn(false);

        Response response = rootTarget().request().get();

        assertThat(response).hasStatus(FORBIDDEN_403);
    }

    @Test
    void should_succeed_with_api_documentation_permission_only() {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                eq(API_ID),
                eq(RolePermissionAction.READ)
            )
        ).thenReturn(true);

        Response response = rootTarget().request().get();

        assertThat(response).hasStatus(OK_200);
    }

    private static NavigationItemReference ownedBy(String apiId) {
        return new NavigationItemReference.ApiReference(apiId);
    }

    private static <T extends PortalNavigationItem> T inEnvironment(T item) {
        item.setEnvironmentId(ENVIRONMENT);
        return item;
    }
}
