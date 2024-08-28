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
import io.gravitee.apim.core.portal_menu_link.model.PortalMenuLinkVisibility;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PortalMenuLinksResourceNotAuthenticatedTest extends AbstractResourceTest {

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

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(NotAuthenticatedAuthenticationFilter.class);
    }

    @Test
    public void should_list_public_portal_menu_links_for_environment_only() {
        // Given
        PortalMenuLink publicPortalMenuLink = PortalMenuLinkFixtures
            .aPortalMenuLink()
            .toBuilder()
            .visibility(PortalMenuLinkVisibility.PUBLIC)
            .environmentId(ENV_ID)
            .build();
        PortalMenuLink privatePortalMenuLink = PortalMenuLinkFixtures
            .aPortalMenuLink()
            .toBuilder()
            .visibility(PortalMenuLinkVisibility.PRIVATE)
            .environmentId(ENV_ID)
            .build();
        portalMenuLinkCrudServiceInMemory.initWith(List.of(publicPortalMenuLink, privatePortalMenuLink));

        // When
        final Response response = target().request().get();

        // Then
        assertEquals(OK_200, response.getStatus());
        var result = (List<io.gravitee.rest.api.portal.rest.model.PortalMenuLink>) response.readEntity(List.class);

        assertThat(result)
            .isNotNull()
            .hasSize(1)
            .first()
            .hasFieldOrPropertyWithValue("id", publicPortalMenuLink.getId())
            .hasFieldOrPropertyWithValue("target", publicPortalMenuLink.getTarget())
            .hasFieldOrPropertyWithValue("name", publicPortalMenuLink.getName())
            .hasFieldOrPropertyWithValue("type", io.gravitee.rest.api.portal.rest.model.PortalMenuLink.TypeEnum.EXTERNAL.toString())
            .hasFieldOrPropertyWithValue("order", publicPortalMenuLink.getOrder());
    }
}
