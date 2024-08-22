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
package io.gravitee.rest.api.management.v2.rest.resource.ui;

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import fixtures.core.model.PortalMenuLinkFixtures;
import inmemory.PortalMenuLinkCrudServiceInMemory;
import io.gravitee.apim.core.portal_menu_link.model.PortalMenuLink;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PortalMenuLinkResource_GetTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";
    private static final String PORTAL_MENU_LINK_ID = "my-portal-menu-link-id";

    @Inject
    private PortalMenuLinkCrudServiceInMemory portalMenuLinkCrudServiceInMemory;

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/ui/portal-menu-links/" + PORTAL_MENU_LINK_ID;
    }

    @BeforeEach
    void init() {
        super.setUp();
        GraviteeContext.cleanContext();

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENV_ID);
        environmentEntity.setOrganizationId(ORGANIZATION);
        doReturn(environmentEntity).when(environmentService).findById(ENV_ID);
        doReturn(environmentEntity).when(environmentService).findByOrgAndIdOrHrid(ORGANIZATION, ENV_ID);

        GraviteeContext.setCurrentEnvironment(ENV_ID);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        portalMenuLinkCrudServiceInMemory.reset();
    }

    @Test
    void should_get_portal_menu_link() {
        // Given
        PortalMenuLink portalMenuLink = PortalMenuLinkFixtures
            .aPortalMenuLink()
            .toBuilder()
            .id(PORTAL_MENU_LINK_ID)
            .environmentId(ENV_ID)
            .build();
        portalMenuLinkCrudServiceInMemory.initWith(List.of(portalMenuLink));

        // When
        var response = rootTarget().request().get();

        // Then
        assertThat(response).hasStatus(200);
        var result = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.PortalMenuLink.class);

        assertThat(result)
            .isNotNull()
            .hasFieldOrPropertyWithValue("id", portalMenuLink.getId())
            .hasFieldOrPropertyWithValue("target", portalMenuLink.getTarget())
            .hasFieldOrPropertyWithValue("name", portalMenuLink.getName())
            .hasFieldOrPropertyWithValue("type", io.gravitee.rest.api.management.v2.rest.model.PortalMenuLink.TypeEnum.EXTERNAL)
            .hasFieldOrPropertyWithValue("order", portalMenuLink.getOrder());
    }

    @Test
    void should_not_get_portal_menu_link_when_not_found() {
        // Given
        PortalMenuLink portalMenuLink = PortalMenuLinkFixtures
            .aPortalMenuLink()
            .toBuilder()
            .id("Another-portal-link-id")
            .environmentId(ENV_ID)
            .build();
        portalMenuLinkCrudServiceInMemory.initWith(List.of(portalMenuLink));

        // When
        var response = rootTarget().request().get();

        // Then
        MAPIAssertions
            .assertThat(response)
            .hasStatus(NOT_FOUND_404)
            .asError()
            .hasHttpStatus(NOT_FOUND_404)
            .hasMessage("PortalMenuLink [ " + PORTAL_MENU_LINK_ID + " ] not found");
    }

    @Test
    public void should_return_403_if_incorrect_permissions() {
        // Given
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_SETTINGS),
                eq(ENV_ID),
                eq(RolePermissionAction.READ)
            )
        )
            .thenReturn(false);

        // When
        final Response response = rootTarget().request().get();

        // Then
        MAPIAssertions
            .assertThat(response)
            .hasStatus(FORBIDDEN_403)
            .asError()
            .hasHttpStatus(FORBIDDEN_403)
            .hasMessage("You do not have sufficient rights to access this resource");
    }
}
