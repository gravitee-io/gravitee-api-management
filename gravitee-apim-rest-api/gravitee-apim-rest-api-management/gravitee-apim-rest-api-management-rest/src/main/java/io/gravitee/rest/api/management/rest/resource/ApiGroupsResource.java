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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.GroupMemberEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

public class ApiGroupsResource extends AbstractResource {

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("api")
    @ApiParam(name = "api", hidden = true)
    private String apiId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get API groups mapped to members", notes = "User must have the MANAGE_MEMBERS permission to use this service")
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "API groups with members", response = MemberEntity.class, responseContainer = "Map"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal Server Error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.API_MEMBER, acls = RolePermissionAction.READ) })
    public Map<String, List<GroupMemberEntity>> getApiGroupsWithMembers() {
        return apiService.getGroupsWithMembers(GraviteeContext.getCurrentEnvironment(), apiId);
    }
}
