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

import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.promotion.PromotionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
@Api(tags = { "Promotion" })
public class PromotionResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private PromotionService promotionService;

    @PathParam("promotion")
    @ApiParam(name = "promotion", required = true, value = "The ID of the promotion")
    private String promotion;

    @POST
    @Path("/_process")
    @ApiOperation(value = "Process an API promotion by accepting or rejecting it")
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "Processed promotion"),
            @ApiResponse(code = 404, message = "Promotion not found"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    public Response processPromotion(boolean accepted) {
        return Response
            .ok(
                promotionService.processPromotion(
                    GraviteeContext.getCurrentOrganization(),
                    GraviteeContext.getCurrentEnvironment(),
                    promotion,
                    accepted,
                    getAuthenticatedUser()
                )
            )
            .build();
    }
}
