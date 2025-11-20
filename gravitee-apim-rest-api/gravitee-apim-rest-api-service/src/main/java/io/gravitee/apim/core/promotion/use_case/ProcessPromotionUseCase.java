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
package io.gravitee.apim.core.promotion.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.domain_service.import_definition.ImportDefinitionCreateDomainService;
import io.gravitee.apim.core.api.domain_service.import_definition.ImportDefinitionUpdateDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.cockpit.model.CockpitReplyStatus;
import io.gravitee.apim.core.promotion.crud_service.PromotionCrudService;
import io.gravitee.apim.core.promotion.model.Promotion;
import io.gravitee.apim.core.promotion.model.PromotionStatus;
import io.gravitee.apim.core.promotion.service_provider.CockpitPromotionServiceProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;

@UseCase
public class ProcessPromotionUseCase {

    private final PromotionCrudService promotionCrudService;
    private final CockpitPromotionServiceProvider cockpitPromotionServiceProvider;
    private final ImportDefinitionCreateDomainService importDefinitionCreateDomainService;
    private final ImportDefinitionUpdateDomainService importDefinitionUpdateDomainService;

    public ProcessPromotionUseCase(
        PromotionCrudService promotionCrudService,
        CockpitPromotionServiceProvider cockpitPromotionServiceProvider,
        ImportDefinitionCreateDomainService importDefinitionCreateDomainService,
        ImportDefinitionUpdateDomainService importDefinitionUpdateDomainService
    ) {
        this.promotionCrudService = promotionCrudService;
        this.cockpitPromotionServiceProvider = cockpitPromotionServiceProvider;
        this.importDefinitionCreateDomainService = importDefinitionCreateDomainService;
        this.importDefinitionUpdateDomainService = importDefinitionUpdateDomainService;
    }

    public record Input(
        Promotion promotion,
        DefinitionVersion definitionVersion,
        boolean isAccepted,
        Api existingPromotedApi,
        ImportDefinition importDefinition,
        AuditInfo auditInfo
    ) {
        public Input(Promotion promotion, boolean isAccepted, DefinitionVersion definitionVersion) {
            this(promotion, definitionVersion, isAccepted, null, null, null);
        }
    }

    public record Output(Promotion promotion) {}

    public Output execute(Input input) {
        var processedPromotion = switch (input.definitionVersion()) {
            case V2 -> cockpitPromotionServiceProvider.process(input.promotion().getId(), input.isAccepted);
            case V4 -> processPromotion(
                input.promotion,
                input.isAccepted,
                input.existingPromotedApi,
                input.importDefinition(),
                input.auditInfo
            );
            default -> throw new IllegalStateException("Only V2 and V4 API definition are supported");
        };
        return new Output(processedPromotion);
    }

    private Promotion processPromotion(
        Promotion promotion,
        boolean isAccepted,
        Api existingPromotedApi,
        ImportDefinition importDefinition,
        AuditInfo auditInfo
    ) {
        if (isAccepted) {
            acceptPromotion(promotion, existingPromotedApi, importDefinition, auditInfo);
        } else {
            promotion.setStatus(PromotionStatus.REJECTED);
        }

        var cockpitReplyStatus = cockpitPromotionServiceProvider.requestPromotion(
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            promotion
        );

        if (cockpitReplyStatus != CockpitReplyStatus.SUCCEEDED) {
            throw new TechnicalManagementException("An error occurs while sending promotion " + promotion.getId() + " request to cockpit");
        }

        return promotionCrudService.update(promotion);
    }

    private void acceptPromotion(Promotion promotion, Api existingPromotedApi, ImportDefinition importDefinition, AuditInfo auditInfo) {
        var exportedApi = importDefinition.getApiExport();

        if (exportedApi.getCrossId() == null || exportedApi.getCrossId().isEmpty()) {
            throw new IllegalStateException("Promotion " + promotion.getId() + " failed. A crossId is required to promote an API");
        }

        if (existingPromotedApi != null) {
            importDefinitionUpdateDomainService.update(importDefinition, existingPromotedApi, auditInfo);
        } else {
            importDefinitionCreateDomainService.create(auditInfo, importDefinition);
        }
        promotion.setStatus(PromotionStatus.ACCEPTED);
    }
}
