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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import inmemory.RoleQueryServiceInMemory;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.portal.rest.model.ApplicationRole;
import io.gravitee.rest.api.portal.rest.model.ConfigurationApplicationRolesResponse;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ApplicationRolesResourceV2Test extends AbstractResourceTest {

    @Autowired
    private RoleQueryServiceInMemory roleQueryService;

    @Override
    protected String contextPath() {
        return "configuration";
    }

    @BeforeEach
    void init() {
        resetAllMocks();
        roleQueryService.reset();
    }

    @Test
    void should_get_application_roles_v2() {
        roleQueryService.initWith(
            List.of(
                aRole("role-2", "USER", Role.Scope.APPLICATION, GraviteeContext.getCurrentOrganization(), false, false),
                aRole("role-1", "ADMIN", Role.Scope.APPLICATION, GraviteeContext.getCurrentOrganization(), true, true),
                aRole("role-3", "PUBLISHER", Role.Scope.API, GraviteeContext.getCurrentOrganization(), false, false),
                aRole("role-4", "OWNER", Role.Scope.APPLICATION, "other-organization", false, false)
            )
        );

        final var response = target().path("applications").path("rolesV2").request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final var rolesResponse = response.readEntity(ConfigurationApplicationRolesResponse.class);
        assertNotNull(rolesResponse);
        assertNotNull(rolesResponse.getData());
        assertEquals(2, rolesResponse.getData().size());
        assertRole(rolesResponse.getData().get(0), "ADMIN", true, true);
        assertRole(rolesResponse.getData().get(1), "USER", false, false);
    }

    @Test
    void should_get_empty_application_roles_v2() {
        roleQueryService.initWith(
            List.of(aRole("role-1", "PUBLISHER", Role.Scope.API, GraviteeContext.getCurrentOrganization(), false, false))
        );

        final var response = target().path("applications").path("rolesV2").request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final var rolesResponse = response.readEntity(ConfigurationApplicationRolesResponse.class);
        assertNotNull(rolesResponse);
        assertNotNull(rolesResponse.getData());
        assertEquals(0, rolesResponse.getData().size());
    }

    private static Role aRole(String id, String name, Role.Scope scope, String organizationId, boolean defaultRole, boolean system) {
        return Role.builder()
            .id(id)
            .name(name)
            .scope(scope)
            .referenceType(Role.ReferenceType.ORGANIZATION)
            .referenceId(organizationId)
            .defaultRole(defaultRole)
            .system(system)
            .build();
    }

    private static void assertRole(ApplicationRole role, String name, boolean defaultRole, boolean system) {
        assertEquals(name, role.getId());
        assertEquals(name, role.getName());
        assertEquals(defaultRole, role.getDefault());
        assertEquals(system, role.getSystem());
    }
}
