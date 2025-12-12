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
import static fixtures.core.model.PortalNavigationItemFixtures.PAGE11_ID;
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.NO_CONTENT_204;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemHasChildrenException;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.use_case.DeletePortalNavigationItemUseCase;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationItemResource_DeleteTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private DeletePortalNavigationItemUseCase deletePortalNavigationItemUseCase;

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

        // Default permission for DELETE on documentation
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.DELETE
            )
        ).thenReturn(true);

        // In this test we mock the Delete use case; no need to seed in-memory CRUD/query services
    }

    @AfterEach
    public void tearDown() {
        GraviteeContext.cleanContext();
        Mockito.reset(deletePortalNavigationItemUseCase);
    }

    @Test
    void should_delete_item_and_return_204() {
        String navId = PAGE11_ID;

        when(deletePortalNavigationItemUseCase.execute(any())).thenReturn(new DeletePortalNavigationItemUseCase.Output());

        Response response = target.path(navId).request().delete();

        assertThat(response).hasStatus(NO_CONTENT_204);

        verify(deletePortalNavigationItemUseCase).execute(
            org.mockito.ArgumentMatchers.argThat(
                input ->
                    input != null &&
                    input.environmentId().equals(ENVIRONMENT) &&
                    input.navigationItemId() != null &&
                    input.navigationItemId().toString().equals(navId)
            )
        );
    }

    @Test
    void should_return_404_when_item_not_found() {
        String unknownId = PortalNavigationItemId.random().toString();

        when(deletePortalNavigationItemUseCase.execute(any())).thenThrow(new PortalNavigationItemNotFoundException(unknownId));

        Response response = target.path(unknownId).request().delete();

        assertThat(response).hasStatus(NOT_FOUND_404);

        verify(deletePortalNavigationItemUseCase).execute(
            org.mockito.ArgumentMatchers.argThat(
                input ->
                    input != null &&
                    input.environmentId().equals(ENVIRONMENT) &&
                    input.navigationItemId() != null &&
                    input.navigationItemId().toString().equals(unknownId)
            )
        );
    }

    @Test
    void should_return_400_when_deleting_item_with_children() {
        String navId = fixtures.core.model.PortalNavigationItemFixtures.APIS_ID; // fixture parent with children

        when(deletePortalNavigationItemUseCase.execute(any())).thenThrow(
            PortalNavigationItemHasChildrenException.forId(
                io.gravitee.apim.core.portal_page.model.PortalNavigationItemId.of(navId).toString()
            )
        );

        Response response = target.path(navId).request().delete();

        assertThat(response).hasStatus(BAD_REQUEST_400);

        verify(deletePortalNavigationItemUseCase).execute(
            org.mockito.ArgumentMatchers.argThat(
                input ->
                    input != null &&
                    input.environmentId().equals(ENVIRONMENT) &&
                    input.navigationItemId() != null &&
                    input.navigationItemId().toString().equals(navId)
            )
        );
    }
}
