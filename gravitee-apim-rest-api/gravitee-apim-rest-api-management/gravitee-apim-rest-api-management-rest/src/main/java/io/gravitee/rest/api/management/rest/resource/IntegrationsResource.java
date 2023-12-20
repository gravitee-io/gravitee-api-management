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

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.model.configuration.integration.IntegrationListItem;
import io.gravitee.rest.api.model.integrations.IntegrationEntity;
import io.gravitee.rest.api.model.integrations.NewIntegrationEntity;
import io.gravitee.rest.api.service.IntegrationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Defines the REST resources to manage integrations.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Integrations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class IntegrationsResource extends AbstractResource {

    @Autowired
    private IntegrationService integrationService;

    @Context
    private ResourceContext resourceContext;

    @GET
    //    @Permissions(@Permission(value = RolePermission.ORGANIZATION_IDENTITY_PROVIDER, acls = RolePermissionAction.READ))
    @Operation(
        summary = "Get the list of integrations",
        description = "User must have the ENVIRONMENT_INTEGRATION[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "List integrations",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = IntegrationListItem.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public List<IntegrationListItem> getIntegrations() {
        return integrationService
            .findAll(GraviteeContext.getExecutionContext())
            .stream()
            .map(integration -> {
                IntegrationListItem item = new IntegrationListItem();
                item.setId(integration.getId());
                item.setName(integration.getName());
                item.setDescription(integration.getDescription());
                item.setType(integration.getType());
                return item;
            })
            .collect(Collectors.toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Create an integration",
        description = "User must have the MANAGEMENT_INTEGRATION[CREATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "201",
        description = "Integration successfully created",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = IntegrationEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response createIntegration(
        @Parameter(name = "integration", required = true) @Valid @NotNull NewIntegrationEntity newIntegrationEntity
    ) {
        IntegrationEntity newIntegration = integrationService.create(GraviteeContext.getExecutionContext(), newIntegrationEntity);

        if (newIntegration != null) {
            return Response.created(this.getLocationHeader(newIntegration.getId())).entity(newIntegration).build();
        }

        return Response.serverError().build();
    }

    @Path("{integration}")
    public IntegrationResource getIntegrationResource() {
        return resourceContext.getResource(IntegrationResource.class);
    }
}
