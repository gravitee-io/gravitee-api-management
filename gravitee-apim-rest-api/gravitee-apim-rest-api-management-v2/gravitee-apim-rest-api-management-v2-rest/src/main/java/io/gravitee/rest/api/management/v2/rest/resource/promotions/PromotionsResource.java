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

import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.promotion.domain_service.PromotionContextDomainService;
import io.gravitee.apim.core.promotion.use_case.ProcessPromotionUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.management.v2.rest.mapper.ImportExportApiMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.PromotionMapper;
import io.gravitee.rest.api.management.v2.rest.model.ExportApiV4;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

public class PromotionsResource extends AbstractResource {

    @Inject
    private ProcessPromotionUseCase promotionUseCase;

    @Inject
    private PromotionContextDomainService promotionContextDomainService;

    @POST
    @Path("{promotionId}/_process")
    @Produces(MediaType.APPLICATION_JSON)
    public Response processPromotion(@PathParam("promotionId") String promotionId, boolean isAccepted) {
        var promotionContext = promotionContextDomainService.getPromotionContext(promotionId, isAccepted);
        var expectedDefinitionVersion = promotionContext.expectedDefinitionVersion();
        var promotion = promotionContext.promotion();
        var existingPromotedApi = promotionContext.existingPromotedApi();

        ProcessPromotionUseCase.Input input;
        if (DefinitionVersion.V4.equals(expectedDefinitionVersion)) {
            ExportApiV4 exportApi = ImportExportApiMapper.INSTANCE.definitionToExportApiV4(promotion.getApiDefinition());
            ImportDefinition importApi = ImportExportApiMapper.INSTANCE.toImportDefinition(exportApi);
            var authenticatedUser = getAuthenticatedUserDetails();
            input = new ProcessPromotionUseCase.Input(
                promotion,
                expectedDefinitionVersion,
                isAccepted,
                existingPromotedApi,
                importApi,
                AuditInfo.builder()
                    .organizationId(GraviteeContext.getCurrentOrganization())
                    .environmentId(promotionContext.targetEnvId())
                    .actor(
                        AuditActor.builder()
                            .userId(authenticatedUser.getUsername())
                            .userSource(authenticatedUser.getSource())
                            .userSourceId(authenticatedUser.getSourceId())
                            .build()
                    )
                    .build()
            );
        } else {
            input = new ProcessPromotionUseCase.Input(promotion, isAccepted, expectedDefinitionVersion);
        }

        return Response.ok(PromotionMapper.INSTANCE.map(promotionUseCase.execute(input).promotion())).build();
    }
}
