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
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.api.header.ApiHeaderEntity;
import io.gravitee.rest.api.model.api.header.UpdateApiHeaderEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.ApiHeaderService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Configuration")
public class ApiHeaderResource extends AbstractResource {

    @Inject
    private ApiHeaderService apiHeaderService;

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Update an API header",
        description = "User must have the PORTAL_API_HEADER[UPDATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "API header successfully updated",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiHeaderEntity.class))
    )
    @ApiResponse(responseCode = "404", description = "API header not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API_HEADER, acls = RolePermissionAction.UPDATE) })
    public ApiHeaderEntity updateApiHeader(@PathParam("id") String id, @Valid @NotNull final UpdateApiHeaderEntity updateApiHeaderEntity) {
        updateApiHeaderEntity.setId(id);
        return apiHeaderService.update(GraviteeContext.getCurrentEnvironment(), updateApiHeaderEntity);
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Delete an API header",
        description = "User must have the PORTAL_API_HEADER[DELETE] permission to use this service"
    )
    @ApiResponse(responseCode = "200", description = "API header successfully deleted")
    @ApiResponse(responseCode = "404", description = "API header not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API_HEADER, acls = RolePermissionAction.DELETE) })
    public void deleteApiHeader(@PathParam("id") String id) {
        apiHeaderService.delete(GraviteeContext.getCurrentEnvironment(), id);
    }
}
