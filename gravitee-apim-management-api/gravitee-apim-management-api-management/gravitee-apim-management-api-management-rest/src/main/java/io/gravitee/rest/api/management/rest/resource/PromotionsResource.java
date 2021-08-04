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

import static java.util.stream.Collectors.toList;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.model.promotion.PromotionEntity;
import io.gravitee.rest.api.model.promotion.PromotionEntityStatus;
import io.gravitee.rest.api.model.promotion.PromotionQuery;
import io.gravitee.rest.api.service.promotion.PromotionService;
import io.swagger.annotations.*;
import java.util.List;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * Defines the REST resources to manage Promotions.
 *
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "Promotions" })
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
    @ApiOperation(value = "Search for Promotion")
    @ApiResponses(
        {
            @ApiResponse(
                code = 200,
                message = "List promotions matching request parameters",
                response = PromotionEntity.class,
                responseContainer = "List"
            ),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    public Response searchPromotions(
        @ApiParam(name = "statuses", required = true) @NotNull @QueryParam("statuses") List<String> statuses,
        @ApiParam(name = "apiId", required = true) @NotNull @QueryParam("apiId") String apiId
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
