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
package io.gravitee.rest.api.management.v2.rest.resource.promotions;

import io.gravitee.apim.core.promotion.use_case.ProcessPromotionUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.PromotionMapper;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

public class PromotionsResource {

    @Inject
    private ProcessPromotionUseCase promotionUseCase;

    @POST
    @Path("{promotionId}/_process")
    @Produces(MediaType.APPLICATION_JSON)
    public Response processPromotion(@PathParam("promotionId") String promotionId, boolean isAccepted) {
        var input = new ProcessPromotionUseCase.Input(promotionId, isAccepted, GraviteeContext.getCurrentOrganization());
        var output = promotionUseCase.execute(input);
        return Response.ok(PromotionMapper.INSTANCE.map(output.promotion())).build();
    }
}
