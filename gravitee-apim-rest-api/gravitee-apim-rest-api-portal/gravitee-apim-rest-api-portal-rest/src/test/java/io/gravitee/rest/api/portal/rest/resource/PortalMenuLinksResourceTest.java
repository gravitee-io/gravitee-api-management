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

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import fixtures.core.model.PortalMenuLinkFixtures;
import inmemory.PortalMenuLinkCrudServiceInMemory;
import io.gravitee.apim.core.portal_menu_link.model.PortalMenuLink;
import io.gravitee.apim.core.portal_menu_link.model.PortalMenuLinkVisibility;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PortalMenuLinksResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "DEFAULT";

    @Inject
    private PortalMenuLinkCrudServiceInMemory portalMenuLinkCrudServiceInMemory;

    @Override
    protected String contextPath() {
        return "portal-menu-links";
    }

    @BeforeEach
    public void init() {
        GraviteeContext.setCurrentEnvironment(ENV_ID);
    }

    @AfterEach
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void should_list_portal_menu_links_for_environment() {
        // Given
        PortalMenuLink publicPortalMenuLink = PortalMenuLinkFixtures.aPortalMenuLink()
            .toBuilder()
            .id("public")
            .visibility(PortalMenuLinkVisibility.PUBLIC)
            .environmentId(ENV_ID)
            .build();
        PortalMenuLink privatePortalMenuLink = PortalMenuLinkFixtures.aPortalMenuLink()
            .toBuilder()
            .id("private")
            .visibility(PortalMenuLinkVisibility.PRIVATE)
            .environmentId(ENV_ID)
            .build();
        List<PortalMenuLink> links = List.of(publicPortalMenuLink, privatePortalMenuLink);
        portalMenuLinkCrudServiceInMemory.initWith(links);

        // When
        final Response response = target().request().get();

        // Then
        assertEquals(OK_200, response.getStatus());
        var result = (List<io.gravitee.rest.api.portal.rest.model.PortalMenuLink>) response.readEntity(List.class);

        assertThat(result)
            .isNotNull()
            .hasSize(2)
            .element(0)
            .hasFieldOrPropertyWithValue("id", publicPortalMenuLink.getId())
            .hasFieldOrPropertyWithValue("target", publicPortalMenuLink.getTarget())
            .hasFieldOrPropertyWithValue("name", publicPortalMenuLink.getName())
            .hasFieldOrPropertyWithValue("type", io.gravitee.rest.api.portal.rest.model.PortalMenuLink.TypeEnum.EXTERNAL.toString())
            .hasFieldOrPropertyWithValue("order", publicPortalMenuLink.getOrder());
        assertThat(result)
            .element(1)
            .hasFieldOrPropertyWithValue("id", privatePortalMenuLink.getId())
            .hasFieldOrPropertyWithValue("target", privatePortalMenuLink.getTarget())
            .hasFieldOrPropertyWithValue("name", privatePortalMenuLink.getName())
            .hasFieldOrPropertyWithValue("type", io.gravitee.rest.api.portal.rest.model.PortalMenuLink.TypeEnum.EXTERNAL.toString())
            .hasFieldOrPropertyWithValue("order", privatePortalMenuLink.getOrder());
    }
}
