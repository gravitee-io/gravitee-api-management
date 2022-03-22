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
import io.gravitee.rest.api.model.PlansConfigurationEntity;
import io.gravitee.rest.api.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Plans")
public class PlansResource extends AbstractResource {

    @Inject
    private PlanService planService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List of available plan's type")
    @ApiResponse(
        responseCode = "200",
        description = "List of plans",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PlansConfigurationEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public PlansConfigurationEntity getPlansConfiguration() {
        return planService.getConfiguration();
    }
}
