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
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
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
        var organizationId = GraviteeContext.getCurrentOrganization();
        var targetEnvId = promotionContextDomainService.targetEnvironmentId(promotionId, organizationId);
        checkCanAccessTargetEnvironment(organizationId, targetEnvId);
        var promotionContext = promotionContextDomainService.getPromotionContext(promotionId, organizationId, isAccepted);
        var expectedDefinitionVersion = promotionContext.expectedDefinitionVersion();
        var promotion = promotionContext.promotion();
        var existingPromotedApi = promotionContext.existingPromotedApi();

        ProcessPromotionUseCase.Input input;
        if (DefinitionVersion.V4.equals(expectedDefinitionVersion)) {
            if (isAccepted) {
                checkCanWriteApisOfTargetEnvironment(organizationId, promotionContext);
            }

            var authenticatedUser = getAuthenticatedUserDetails();
            var auditInfo = AuditInfo.builder()
                .organizationId(organizationId)
                .environmentId(promotionContext.targetEnvId())
                .actor(
                    AuditActor.builder()
                        .userId(authenticatedUser.getUsername())
                        .userSource(authenticatedUser.getSource())
                        .userSourceId(authenticatedUser.getSourceId())
                        .build()
                )
                .build();

            ImportDefinition importApi = null;
            if (isAccepted) {
                ExportApiV4 exportApi = ImportExportApiMapper.INSTANCE.definitionToExportApiV4(promotion.getApiDefinition());
                importApi = ImportExportApiMapper.INSTANCE.toImportDefinition(exportApi);
            }

            input = new ProcessPromotionUseCase.Input(
                promotion,
                expectedDefinitionVersion,
                isAccepted,
                existingPromotedApi,
                importApi,
                auditInfo
            );
        } else {
            input = new ProcessPromotionUseCase.Input(promotion, isAccepted, expectedDefinitionVersion);
        }

        return Response.ok(PromotionMapper.INSTANCE.map(promotionUseCase.execute(input).promotion())).build();
    }

    /**
     * CREATE or UPDATE on the target environment. Checked before the promotion status and the target API are resolved,
     * because those errors name the promotion state and, on accept, the target API id.
     */
    private void checkCanAccessTargetEnvironment(String organizationId, String targetEnvId) {
        if (
            !canWriteApis(organizationId, targetEnvId, RolePermissionAction.CREATE) &&
            !canWriteApis(organizationId, targetEnvId, RolePermissionAction.UPDATE)
        ) {
            throw new ForbiddenAccessException();
        }
    }

    /**
     * V4 counterpart of the check {@code PromotionServiceImpl} applies to V2 promotions: processing writes into the
     * target environment, so the caller needs the right there, not in the environment the request came from.
     */
    private void checkCanWriteApisOfTargetEnvironment(
        String organizationId,
        PromotionContextDomainService.PromotionContext promotionContext
    ) {
        var targetEnvId = promotionContext.targetEnvId();
        var action = promotionContext.existingPromotedApi() == null ? RolePermissionAction.CREATE : RolePermissionAction.UPDATE;
        if (!canWriteApis(organizationId, targetEnvId, action)) {
            throw new ForbiddenAccessException();
        }
    }

    private boolean canWriteApis(String organizationId, String targetEnvId, RolePermissionAction action) {
        return hasPermission(new ExecutionContext(organizationId, targetEnvId), RolePermission.ENVIRONMENT_API, targetEnvId, action);
    }
}
