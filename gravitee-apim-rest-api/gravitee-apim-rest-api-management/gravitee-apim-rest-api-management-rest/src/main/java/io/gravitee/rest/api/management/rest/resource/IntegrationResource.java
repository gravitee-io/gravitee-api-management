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
import io.gravitee.rest.api.service.IntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Integrations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class IntegrationResource extends AbstractResource {

    @Autowired
    private IntegrationService integrationService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get an integration",
        description = "User must have the ENVIRONMENT_INTEGRATION[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "An integration",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = IntegrationEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    //    @Permissions(@Permission(value = RolePermission.ORGANIZATION_IDENTITY_PROVIDER, acls = RolePermissionAction.READ))
    public IntegrationEntity getIntegration(@PathParam("integration") String integration) {
        return integrationService.findById(integration);
    }
}
