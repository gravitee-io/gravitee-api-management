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
package io.gravitee.rest.api.portal.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalMenuLinkFixtures;
import inmemory.PortalMenuLinkCrudServiceInMemory;
import io.gravitee.apim.core.portal_menu_link.model.PortalMenuLink;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PortalMenuLinksResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "DEFAULT";

    @Inject
    private PortalMenuLinkCrudServiceInMemory portalMenuLinkCrudServiceInMemory;

    @Override
    protected String contextPath() {
        return "portal-menu-links";
    }

    @Before
    public void init() {
        GraviteeContext.setCurrentEnvironment(ENV_ID);
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void should_list_portal_menu_links_for_environment() {
        // Given
        PortalMenuLink portalMenuLink = PortalMenuLinkFixtures.aPortalMenuLink().toBuilder().environmentId(ENV_ID).build();
        portalMenuLinkCrudServiceInMemory.initWith(List.of(portalMenuLink));
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_SETTINGS),
                eq(ENV_ID),
                eq(RolePermissionAction.READ)
            )
        )
            .thenReturn(true);

        // When
        final Response response = target().request().get();

        // Then
        assertEquals(OK_200, response.getStatus());
        var result = (List<io.gravitee.rest.api.portal.rest.model.PortalMenuLink>) response.readEntity(List.class);

        assertThat(result)
            .isNotNull()
            .hasSize(1)
            .first()
            .hasFieldOrPropertyWithValue("id", portalMenuLink.getId())
            .hasFieldOrPropertyWithValue("target", portalMenuLink.getTarget())
            .hasFieldOrPropertyWithValue("name", portalMenuLink.getName())
            .hasFieldOrPropertyWithValue("type", io.gravitee.rest.api.portal.rest.model.PortalMenuLink.TypeEnum.EXTERNAL.toString())
            .hasFieldOrPropertyWithValue("order", portalMenuLink.getOrder());
    }

    @Test
    public void should_return_403_if_incorrect_permissions() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_SETTINGS),
                eq(ENV_ID),
                eq(RolePermissionAction.READ)
            )
        )
            .thenReturn(false);

        final Response response = target().request().get();

        assertEquals(FORBIDDEN_403, response.getStatus());
    }
}
