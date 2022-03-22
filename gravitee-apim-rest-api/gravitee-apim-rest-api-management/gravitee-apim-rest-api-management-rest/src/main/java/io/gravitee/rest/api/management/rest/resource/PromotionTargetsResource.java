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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Promotion")
public class PromotionTargetsResource extends AbstractResource {

    @Inject
    private PromotionService promotionService;

    @Inject
    private InstallationService installationService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List available targets (environments) for a promotion")
    @ApiResponse(
        responseCode = "200",
        description = "List of promotion targets",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = PromotionTargetEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @ApiResponse(responseCode = "503", description = "Installation not connected to cockpit")
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
