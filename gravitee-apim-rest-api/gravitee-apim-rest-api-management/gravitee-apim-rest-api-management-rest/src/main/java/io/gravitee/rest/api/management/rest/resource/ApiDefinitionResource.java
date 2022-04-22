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

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.gravitee.rest.api.service.exceptions.JsonPatchTestFailedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.*;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 * @author GraviteeSource Team
 */
@Tag(name = "API Definition")
public class ApiDefinitionResource extends AbstractResource {

    private static final String EXPORT_VERSION = "default";

    @Inject
    private ApiExportService apiExportService;

    @Inject
    private ApiDuplicatorService apiDuplicatorService;

    @Inject
    private JsonPatchService jsonPatchService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("api")
    @Parameter(name = "api", hidden = true)
    private String api;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Export the API definition in JSON format",
        description = "User must have the API_DEFINITION[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "API definition",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonNode.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.READ) })
    public Response getApiDefinition() {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final String apiDefinition = apiExportService.exportAsJson(executionContext, api, EXPORT_VERSION);
        return Response.ok(apiDefinition).build();
    }

    @PATCH
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Update the API with json patches",
        description = "User must have the API_DEFINITION[UPDATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "API successfully updated with json patches",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = JsonNode.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response patch(
        @RequestBody(required = true) @Valid @NotNull final Collection<JsonPatch> patches,
        @QueryParam("dryRun") @DefaultValue("false") boolean dryRun
    ) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final ApiEntity apiEntity = getApi(executionContext);
        String apiDefinition = apiExportService.exportAsJson(executionContext, apiEntity.getId(), EXPORT_VERSION);
        try {
            String apiDefinitionModified = jsonPatchService.execute(apiDefinition, patches);
            if (dryRun) {
                return Response.ok(apiDefinitionModified).build();
            }
            ApiEntity updatedApi = apiDuplicatorService.updateWithImportedDefinition(executionContext, apiDefinitionModified);
            final String apiDefinitionUpdated = apiExportService.exportAsJson(executionContext, api, EXPORT_VERSION);
            return Response
                .ok(apiDefinitionUpdated)
                .tag(Long.toString(updatedApi.getUpdatedAt().getTime()))
                .lastModified(updatedApi.getUpdatedAt())
                .build();
        } catch (JsonPatchTestFailedException e) {
            return Response
                .noContent()
                .tag(Long.toString(apiEntity.getUpdatedAt().getTime()))
                .lastModified(apiEntity.getUpdatedAt())
                .build();
        }
    }

    private ApiEntity getApi(final ExecutionContext executionContext) {
        ApiEntity apiEntity = apiService.findById(executionContext, api);

        if (!canManageApi(apiEntity)) {
            throw new ForbiddenAccessException();
        }
        return apiEntity;
    }
}
