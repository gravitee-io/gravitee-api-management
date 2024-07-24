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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.apim.core.environment_flow.use_case.CreateEnvironmentFlowUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.v4.environmentflow.EnvironmentFlow;
import io.gravitee.rest.api.model.EnvironmentFlowEntity;
import io.gravitee.rest.api.model.NewEnvironmentFlowEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@Tag(name = "Environment flows")
public class EnvironmentFlowsResource extends AbstractResource {

    @Inject
    private CreateEnvironmentFlowUseCase createEnvironmentFlowUseCase;

    /**
     * Create a new Environment Flow.
     * @param newEnvironmentFlow
     * @return
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create an Environment Flow")
    @ApiResponse(
        responseCode = "201",
        description = "Environment flow successfully created",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EnvironmentFlowEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.CREATE) })
    public Response createEnvironmentFlow(
        @Parameter(name = "environmentFlow", required = true) @Valid @NotNull final NewEnvironmentFlowEntity newEnvironmentFlow
    ) {
        var result = createEnvironmentFlowUseCase.execute(
            new CreateEnvironmentFlowUseCase.Input(
                EnvironmentFlow.builder().name(newEnvironmentFlow.getName()).version(newEnvironmentFlow.getVersion()).build()
            )
        );
        return Response.created(this.getLocationHeader("created-id")).entity(result.flow()).build();
    }
}
