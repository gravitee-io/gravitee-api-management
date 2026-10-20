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
import static io.gravitee.common.http.HttpStatusCode.NO_CONTENT_204;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.use_case.DeletePortalNavigationItemUseCase;
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
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiDocumentationNavigationItemResource_DeleteTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";
    private static final String API_ID = "api-id";
    private static final String OTHER_API_ID = "other-api-id";

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private DeletePortalNavigationItemUseCase deletePortalNavigationItemUseCase;

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
        Mockito.reset(deletePortalNavigationItemUseCase);
    }

    @Test
    void should_delete_an_item_owned_by_the_api() {
        var page = aPageOwnedBy(API_ID);
        portalNavigationItemCrudService.initWith(List.of(page));

        Response response = rootTarget(page.getId().json()).request().delete();

        assertThat(response).hasStatus(NO_CONTENT_204);
        verify(deletePortalNavigationItemUseCase).execute(
            new DeletePortalNavigationItemUseCase.Input(ORGANIZATION, ENVIRONMENT, page.getId())
        );
    }

    @Test
    void should_return_404_for_an_unknown_item() {
        Response response = rootTarget(PortalNavigationItemId.random().json()).request().delete();

        assertThat(response).hasStatus(NOT_FOUND_404);
        verifyNoInteractions(deletePortalNavigationItemUseCase);
    }

    @Test
    void should_reject_an_item_id_belonging_to_another_api() {
        var foreign = aPageOwnedBy(OTHER_API_ID);
        portalNavigationItemCrudService.initWith(List.of(foreign));

        Response response = rootTarget(foreign.getId().json()).request().delete();

        assertThat(response).hasStatus(NOT_FOUND_404);
        verifyNoInteractions(deletePortalNavigationItemUseCase);
    }

    @Test
    void should_reject_a_portal_owned_item() {
        var section = PortalNavigationItemFixtures.aFolder("APIs");
        section.setEnvironmentId(ENVIRONMENT);
        portalNavigationItemCrudService.initWith(List.of(section));

        Response response = rootTarget(section.getId().json()).request().delete();

        assertThat(response).hasStatus(NOT_FOUND_404);
        verifyNoInteractions(deletePortalNavigationItemUseCase);
    }

    @Test
    void should_reject_the_api_listing_row() {
        var section = PortalNavigationItemFixtures.aFolder("APIs");
        section.setEnvironmentId(ENVIRONMENT);
        var listing = PortalNavigationItemFixtures.anApi(PortalNavigationItemId.random().json(), "My API", section.getId(), API_ID);
        listing.setEnvironmentId(ENVIRONMENT);
        portalNavigationItemCrudService.initWith(List.of(section, listing));

        Response response = rootTarget(listing.getId().json()).request().delete();

        assertThat(response).hasStatus(NOT_FOUND_404);
        verifyNoInteractions(deletePortalNavigationItemUseCase);
    }

    @Test
    void should_return_403_with_environment_documentation_permission_only() {
        var page = aPageOwnedBy(API_ID);
        portalNavigationItemCrudService.initWith(List.of(page));
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                eq(API_ID),
                eq(RolePermissionAction.DELETE)
            )
        ).thenReturn(false);

        Response response = rootTarget(page.getId().json()).request().delete();

        assertThat(response).hasStatus(FORBIDDEN_403);
        verifyNoInteractions(deletePortalNavigationItemUseCase);
    }

    @Test
    void should_succeed_with_api_documentation_permission_only() {
        var page = aPageOwnedBy(API_ID);
        portalNavigationItemCrudService.initWith(List.of(page));
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                eq(API_ID),
                eq(RolePermissionAction.DELETE)
            )
        ).thenReturn(true);

        Response response = rootTarget(page.getId().json()).request().delete();

        assertThat(response).hasStatus(NO_CONTENT_204);
    }

    private static PortalNavigationPage aPageOwnedBy(String apiId) {
        var page = PortalNavigationItemFixtures.aPage("Overview", null)
            .toBuilder()
            .reference(new NavigationItemReference.ApiReference(apiId))
            .build();
        page.setEnvironmentId(ENVIRONMENT);
        return page;
    }
}
