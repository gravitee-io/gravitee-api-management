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
import io.gravitee.rest.api.model.promotion.PromotionEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.promotion.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Promotion")
public class PromotionResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private PromotionService promotionService;

    @PathParam("promotion")
    @Parameter(name = "promotion", required = true, description = "The ID of the promotion")
    private String promotion;

    @POST
    @Path("/_process")
    @Operation(summary = "Process an API promotion by accepting or rejecting it")
    @ApiResponse(
        responseCode = "200",
        description = "Processed promotion",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PromotionEntity.class))
    )
    @ApiResponse(responseCode = "404", description = "Promotion not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response processPromotion(boolean accepted) {
        return Response.ok(promotionService.processPromotion(GraviteeContext.getExecutionContext(), promotion, accepted)).build();
    }
}
