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

import static io.gravitee.common.http.MediaType.APPLICATION_JSON;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.api.header.ApiHeaderEntity;
import io.gravitee.rest.api.model.api.header.NewApiHeaderEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.ApiHeaderService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Configuration")
public class ApiHeadersResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApiHeaderService apiHeaderService;

    @GET
    @Produces(APPLICATION_JSON)
    @Operation(summary = "List API headers")
    @ApiResponse(
        responseCode = "200",
        description = "List of API headers",
        content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = ApiHeaderEntity.class)))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API_HEADER, acls = RolePermissionAction.READ) })
    public List<ApiHeaderEntity> getApiHeaders() {
        return apiHeaderService.findAll(GraviteeContext.getCurrentEnvironment());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Create an API header",
        description = "User must have the PORTAL_API_HEADER[CREATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "API header successfully created",
        content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ApiHeaderEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API_HEADER, acls = RolePermissionAction.CREATE) })
    public ApiHeaderEntity createApiHeader(@Valid @NotNull final NewApiHeaderEntity newApiHeaderEntity) {
        return apiHeaderService.create(GraviteeContext.getExecutionContext(), newApiHeaderEntity);
    }

    @Path("{id}")
    public ApiHeaderResource getApiHeaderResource() {
        return resourceContext.getResource(ApiHeaderResource.class);
    }
}
