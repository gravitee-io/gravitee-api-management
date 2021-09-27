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

import io.gravitee.rest.api.model.InstallationStatus;
import io.gravitee.rest.api.model.promotion.PromotionTargetEntity;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.InstallationNotAcceptedException;
import io.gravitee.rest.api.service.promotion.PromotionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "Promotion" })
public class PromotionTargetsResource extends AbstractResource {

    @Inject
    private PromotionService promotionService;

    @Inject
    private InstallationService installationService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List available targets (environments) for a promotion")
    @ApiResponses(
        {
            @ApiResponse(
                code = 200,
                message = "List of promotion targets",
                response = PromotionTargetEntity.class,
                responseContainer = "List"
            ),
            @ApiResponse(code = 500, message = "Internal server error"),
            @ApiResponse(code = 503, message = "Installation not connected to cockpit"),
        }
    )
    public Response getPromotionTargets() {
        InstallationStatus status = this.installationService.getInstallationStatus();
        if (InstallationStatus.ACCEPTED == status) {
            final List<PromotionTargetEntity> promotionTargetEntities =
                this.promotionService.listPromotionTargets(
                        GraviteeContext.getCurrentOrganization(),
                        GraviteeContext.getCurrentEnvironment()
                    );
            return Response.ok(promotionTargetEntities).build();
        }
        throw new InstallationNotAcceptedException(this.installationService.get(), status);
    }
}
