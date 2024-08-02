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
package io.gravitee.rest.api.management.rest.resource.organization;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

import io.gravitee.common.http.MediaType;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.rest.api.management.rest.model.wrapper.RoleScopesLinkedHashMap;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.ApplicationPermission;
import io.gravitee.rest.api.model.permissions.EnvironmentPermission;
import io.gravitee.rest.api.model.permissions.IntegrationPermission;
import io.gravitee.rest.api.model.permissions.OrganizationPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Roles")
public class RoleScopesResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List availables role scopes")
    @ApiResponse(
        responseCode = "200",
        description = "List of role scopes",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RoleScopesLinkedHashMap.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public RoleScopesLinkedHashMap getRoleScopes() {
        final RoleScopesLinkedHashMap roles = new RoleScopesLinkedHashMap(4);
        roles.put(
            RoleScope.ORGANIZATION.name(),
            stream(OrganizationPermission.values()).map(OrganizationPermission::getName).sorted().collect(toList())
        );
        roles.put(
            RoleScope.ENVIRONMENT.name(),
            stream(EnvironmentPermission.values()).map(EnvironmentPermission::getName).sorted().collect(toList())
        );
        roles.put(RoleScope.API.name(), stream(ApiPermission.values()).map(ApiPermission::getName).sorted().collect(toList()));
        roles.put(
            RoleScope.APPLICATION.name(),
            stream(ApplicationPermission.values()).map(ApplicationPermission::getName).sorted().collect(toList())
        );
        roles.put(
            RoleScope.INTEGRATION.name(),
            stream(IntegrationPermission.values()).map(IntegrationPermission::getName).sorted().collect(toList())
        );
        return roles;
    }

    @Path("{scope}/roles")
    public RoleScopeResource getRoleScopeResource() {
        return resourceContext.getResource(RoleScopeResource.class);
    }
}
