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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.settings.PortalConfigEntity;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;

@Tag(name = "Portal")
public class PortalResource {

    @Inject
    private ConfigService configService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the portal configuration", description = "Every users can use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Portal configuration",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PortalConfigEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public PortalConfigEntity getPortalConfig() {
        return configService.getPortalConfig(GraviteeContext.getExecutionContext());
    }
}
