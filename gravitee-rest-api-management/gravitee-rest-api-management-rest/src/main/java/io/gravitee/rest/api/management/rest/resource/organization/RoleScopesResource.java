/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.ApplicationPermission;
import io.gravitee.rest.api.model.permissions.EnvironmentPermission;
import io.gravitee.rest.api.model.permissions.OrganizationPermission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "Roles" })
public class RoleScopesResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List availables role scopes")
    @ApiResponses(
        { @ApiResponse(code = 200, message = "List of role scopes"), @ApiResponse(code = 500, message = "Internal server error") }
    )
    public Map<String, List<String>> getRoleScopes() {
        final Map<String, List<String>> roles = new LinkedHashMap<>(4);
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
        return roles;
    }

    @Path("{scope}/roles")
    public RoleScopeResource getRoleScopeResource() {
        return resourceContext.getResource(RoleScopeResource.class);
    }
}
