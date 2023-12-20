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
import io.gravitee.rest.api.model.integrations.IntegrationEntity;
import io.gravitee.rest.api.model.integrations.NewIntegrationEntity;
import io.gravitee.rest.api.service.IntegrationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Defines the REST resources to manage integrations.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Integrations")
public class IntegrationsResource extends AbstractResource {

    @Autowired
    private IntegrationService integrationService;

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
}
