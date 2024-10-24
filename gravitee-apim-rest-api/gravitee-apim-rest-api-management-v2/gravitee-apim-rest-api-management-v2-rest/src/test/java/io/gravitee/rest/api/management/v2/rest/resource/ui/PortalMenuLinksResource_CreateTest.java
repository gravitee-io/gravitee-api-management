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
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static jakarta.ws.rs.client.Entity.json;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import fixtures.core.model.PortalMenuLinkFixtures;
import inmemory.PortalMenuLinkCrudServiceInMemory;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalMenuLink;
import io.gravitee.rest.api.management.v2.rest.model.PortalMenuLink;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PortalMenuLinksResource_CreateTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";

    @Inject
    private PortalMenuLinkCrudServiceInMemory portalMenuLinkCrudServiceInMemory;

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/ui/portal-menu-links";
    }

    @BeforeEach
    void init() {
        super.setUp();
        GraviteeContext.cleanContext();

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENV_ID);
        environmentEntity.setOrganizationId(ORGANIZATION);
        when(environmentService.findById(ENV_ID)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENV_ID)).thenReturn(environmentEntity);

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
    void should_create_portal_menu_link_with_default_visibility() {
        // When
        var portalMenuLinkToCreate = CreatePortalMenuLink
            .builder()
            .name("new menu link")
            .target("http://newTarget")
            .type(CreatePortalMenuLink.TypeEnum.EXTERNAL)
            .build();
        final Response response = rootTarget().request().post(json(portalMenuLinkToCreate));

        // Then
        assertThat(response.getStatus()).isEqualTo(CREATED_201);
        var result = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.PortalMenuLink.class);

        assertThat(result)
            .isNotNull()
            .hasFieldOrProperty("id")
            .hasFieldOrPropertyWithValue("target", portalMenuLinkToCreate.getTarget())
            .hasFieldOrPropertyWithValue("name", portalMenuLinkToCreate.getName())
            .hasFieldOrPropertyWithValue("type", PortalMenuLink.TypeEnum.EXTERNAL)
            .hasFieldOrPropertyWithValue("visibility", PortalMenuLink.VisibilityEnum.PRIVATE)
            .hasFieldOrPropertyWithValue("order", 1);
        assertThat(portalMenuLinkCrudServiceInMemory.storage()).hasSize(1);
    }

    @Test
    void should_create_portal_menu_link_with_public_visibility() {
        // When
        var portalMenuLinkToCreate = CreatePortalMenuLink
            .builder()
            .name("new menu link")
            .target("http://newTarget")
            .type(CreatePortalMenuLink.TypeEnum.EXTERNAL)
            .visibility(CreatePortalMenuLink.VisibilityEnum.PUBLIC)
            .build();
        final Response response = rootTarget().request().post(json(portalMenuLinkToCreate));

        // Then
        assertThat(response.getStatus()).isEqualTo(CREATED_201);
        var result = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.PortalMenuLink.class);

        assertThat(result)
            .isNotNull()
            .hasFieldOrProperty("id")
            .hasFieldOrPropertyWithValue("target", portalMenuLinkToCreate.getTarget())
            .hasFieldOrPropertyWithValue("name", portalMenuLinkToCreate.getName())
            .hasFieldOrPropertyWithValue("type", PortalMenuLink.TypeEnum.EXTERNAL)
            .hasFieldOrPropertyWithValue("visibility", PortalMenuLink.VisibilityEnum.PUBLIC)
            .hasFieldOrPropertyWithValue("order", 1);
        assertThat(portalMenuLinkCrudServiceInMemory.storage()).hasSize(1);
    }

    @Test
    void should_return_400_if_execute_fails_with_invalid_data_exception() {
        // When
        var createPortalMenuLink = PortalMenuLinkFixtures.aCreatePortalMenuLink().toBuilder().name(null).build();
        final Response response = rootTarget().request().post(json(createPortalMenuLink));

        // Then
        MAPIAssertions
            .assertThat(response)
            .hasStatus(BAD_REQUEST_400)
            .asError()
            .hasHttpStatus(BAD_REQUEST_400)
            .hasMessage("Validation error");
        assertThat(portalMenuLinkCrudServiceInMemory.storage()).hasSize(0);
    }

    @Test
    void should_return_400_if_missing_body() {
        // When
        final Response response = rootTarget().request().post(json(null));

        // Then
        MAPIAssertions
            .assertThat(response)
            .hasStatus(BAD_REQUEST_400)
            .asError()
            .hasHttpStatus(BAD_REQUEST_400)
            .hasMessage("Validation error");
        assertThat(portalMenuLinkCrudServiceInMemory.storage()).hasSize(0);
    }

    @Test
    void should_return_400_if_name_not_specified() {
        // When
        final Response response = rootTarget().request().post(json(new CreatePortalMenuLink()));

        // Then
        MAPIAssertions
            .assertThat(response)
            .hasStatus(BAD_REQUEST_400)
            .asError()
            .hasHttpStatus(BAD_REQUEST_400)
            .hasMessage("Validation error");
        assertThat(portalMenuLinkCrudServiceInMemory.storage()).hasSize(0);
    }

    @Test
    public void should_return_403_if_incorrect_permissions() {
        // Given
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_SETTINGS),
                eq(ENV_ID),
                eq(RolePermissionAction.UPDATE)
            )
        )
            .thenReturn(false);

        // When
        final Response response = rootTarget().request().post(json(PortalMenuLinkFixtures.aCreatePortalMenuLink()));

        // Then
        MAPIAssertions
            .assertThat(response)
            .hasStatus(FORBIDDEN_403)
            .asError()
            .hasHttpStatus(FORBIDDEN_403)
            .hasMessage("You do not have sufficient rights to access this resource");
        assertThat(portalMenuLinkCrudServiceInMemory.storage()).hasSize(0);
    }
}
