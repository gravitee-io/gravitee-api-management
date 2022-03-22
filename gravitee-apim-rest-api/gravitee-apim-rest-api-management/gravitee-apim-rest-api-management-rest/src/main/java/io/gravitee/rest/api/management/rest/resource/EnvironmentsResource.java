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

import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.EnvironmentPermissionsEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EnvironmentsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private EnvironmentService environmentService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List available environments for current user organization")
    public Collection<EnvironmentEntity> getEnvironments() {
        return this.environmentService.findByUser(GraviteeContext.getCurrentOrganization(), getAuthenticatedUserOrNull());
    }

    @Path("{envId}")
    public EnvironmentResource getEnvironmentResource(
        @PathParam("envId") @Parameter(
            name = "envId",
            required = true,
            description = "The ID of the environment",
            schema = @Schema(defaultValue = "DEFAULT")
        ) String envId
    ) {
        return resourceContext.getResource(EnvironmentResource.class);
    }

    @GET
    @Produces(io.gravitee.common.http.MediaType.APPLICATION_JSON)
    @Path("/permissions")
    @Operation(summary = "List available environments with their permissions for current user organization")
    @ApiResponse(
        responseCode = "200",
        description = "Current user permissions on its environments",
        content = @Content(
            mediaType = io.gravitee.common.http.MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = EnvironmentPermissionsEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public List<EnvironmentPermissionsEntity> getEnvironmentsPermissions(
        @Parameter(description = "To filter on environment id or hrid") @QueryParam("idOrHrid") String id
    ) {
        List<EnvironmentEntity> environments =
            this.environmentService.findByUserAndIdOrHrid(GraviteeContext.getCurrentOrganization(), getAuthenticatedUserOrNull(), id);

        return environments
            .stream()
            .map(
                environment -> {
                    Map<String, char[]> permissions = new HashMap<>();
                    if (isAuthenticated()) {
                        final String username = getAuthenticatedUser();
                        permissions =
                            membershipService.getUserMemberPermissions(GraviteeContext.getCurrentEnvironment(), environment, username);
                    }

                    EnvironmentPermissionsEntity environmentPermissions = new EnvironmentPermissionsEntity();
                    environmentPermissions.setId(environment.getId());
                    environmentPermissions.setName(environment.getName());
                    environmentPermissions.setHrids(environment.getHrids());
                    environmentPermissions.setPermissions(permissions);

                    return environmentPermissions;
                }
            )
            .collect(Collectors.toList());
    }
}
