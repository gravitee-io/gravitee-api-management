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

import static java.util.stream.Collectors.toList;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.model.promotion.PromotionEntity;
import io.gravitee.rest.api.model.promotion.PromotionEntityStatus;
import io.gravitee.rest.api.model.promotion.PromotionQuery;
import io.gravitee.rest.api.service.promotion.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * Defines the REST resources to manage Promotions.
 *
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Promotions")
public class PromotionsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private PromotionService promotionService;

    @Path("{promotion}")
    public PromotionResource getPolicyResource() {
        return resourceContext.getResource(PromotionResource.class);
    }

    @POST
    @Path("_search")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Search for Promotion")
    @ApiResponse(
        responseCode = "200",
        description = "List promotions matching request parameters",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = PromotionEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response searchPromotions(
        @Parameter(name = "statuses", required = true) @NotNull @QueryParam("statuses") List<String> statuses,
        @Parameter(name = "apiId", required = true) @NotNull @QueryParam("apiId") String apiId
    ) {
        PromotionQuery promotionQuery = new PromotionQuery();
        promotionQuery.setStatuses(statuses.stream().map(PromotionEntityStatus::valueOf).collect(toList()));
        promotionQuery.setApiId(apiId);

        List<PromotionEntity> promotions = promotionService
            .search(promotionQuery, new SortableImpl("created_at", false), null)
            .getContent();

        return Response.ok().entity(promotions).build();
    }
}
