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
import io.gravitee.apim.core.api.domain_service.ApiExportDomainService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.Excludable;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.cockpit.model.CockpitReplyStatus;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.json.GraviteeDefinitionSerializer;
import io.gravitee.apim.core.json.JsonProcessingException;
import io.gravitee.apim.core.promotion.crud_service.PromotionCrudService;
import io.gravitee.apim.core.promotion.domain_service.CockpitPromotionLegacyWrapper;
import io.gravitee.apim.core.promotion.domain_service.PromotionValidationDomainService;
import io.gravitee.apim.core.promotion.model.Promotion;
import io.gravitee.apim.core.promotion.model.PromotionAuthor;
import io.gravitee.apim.core.promotion.model.PromotionRequest;
import io.gravitee.apim.core.promotion.model.PromotionStatus;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@UseCase
@Slf4j
public class CreatePromotionUseCase {

    private final ApiExportDomainService apiExportDomainService;
    private final EnvironmentCrudService environmentCrudService;
    private final PromotionValidationDomainService promotionValidationDomainService;
    private final GraviteeDefinitionSerializer graviteeDefinitionSerializer;
    private final PromotionCrudService promotionCrudService;
    private final AuditDomainService auditService;
    private final CockpitPromotionLegacyWrapper cockpitPromotionLegacyWrapper;

    public record Input(String apiId, PromotionRequest promotionRequest, BaseUserEntity authenticatedUser, AuditInfo auditInfo) {}

    public record Output(Promotion promotion) {}

    public CreatePromotionUseCase(
        ApiExportDomainService apiExportDomainService,
        EnvironmentCrudService environmentCrudService,
        PromotionValidationDomainService promotionValidationDomainService,
        GraviteeDefinitionSerializer graviteeDefinitionSerializer,
        PromotionCrudService promotionCrudService,
        AuditDomainService auditService,
        CockpitPromotionLegacyWrapper cockpitPromotionLegacyWrapper
    ) {
        this.apiExportDomainService = apiExportDomainService;
        this.environmentCrudService = environmentCrudService;
        this.promotionValidationDomainService = promotionValidationDomainService;
        this.graviteeDefinitionSerializer = graviteeDefinitionSerializer;
        this.promotionCrudService = promotionCrudService;
        this.auditService = auditService;
        this.cockpitPromotionLegacyWrapper = cockpitPromotionLegacyWrapper;
    }

    public Output execute(Input input) {
        var createdPromotion = createPromotion(input.apiId, input.promotionRequest, input.auditInfo, input.authenticatedUser);
        var updatedPromotion = sendCockpitCommand(createdPromotion, input.auditInfo);
        return new Output(updatedPromotion);
    }

    private Promotion createPromotion(
        String apiId,
        PromotionRequest promotionRequest,
        AuditInfo auditInfo,
        BaseUserEntity authenticatedUser
    ) {
        var apiDefinition = apiExportDomainService.export(apiId, auditInfo, Set.of(Excludable.GROUPS, Excludable.MEMBERS));
        var sourceEnvironment = environmentCrudService.get(auditInfo.environmentId());

        promotionValidationDomainService.validate(apiId, promotionRequest.getTargetEnvCockpitId());

        Promotion toSave;
        try {
            toSave = Promotion.builder()
                .id(UuidString.generateRandom())
                .author(
                    PromotionAuthor.builder()
                        .userId(authenticatedUser.getId())
                        .displayName(authenticatedUser.displayName())
                        .email(authenticatedUser.getEmail())
                        .source(authenticatedUser.getSource())
                        .sourceId(authenticatedUser.getSourceId())
                        .build()
                )
                .status(PromotionStatus.CREATED)
                .apiId(apiId)
                .apiDefinition(graviteeDefinitionSerializer.serialize(apiDefinition))
                .sourceEnvCockpitId(sourceEnvironment.getId())
                .sourceEnvName(sourceEnvironment.getName())
                .targetEnvCockpitId(promotionRequest.getTargetEnvCockpitId())
                .targetEnvName(promotionRequest.getTargetEnvName())
                .createdAt(Date.from(TimeProvider.now().toInstant()))
                .build();
        } catch (JsonProcessingException e) {
            throw new TechnicalManagementException("Fail to serialize api definition", e);
        }

        var createdPromotion = promotionCrudService.create(toSave);

        auditService.createApiAuditLog(
            ApiAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiId(createdPromotion.getApiId())
                .event(ApiAuditEvent.PROMOTION_CREATED)
                .actor(auditInfo.actor())
                .oldValue(null)
                .newValue(createdPromotion)
                .createdAt(createdPromotion.getCreatedAt().toInstant().atZone(ZoneId.of("UTC")))
                .properties(Collections.emptyMap())
                .build()
        );

        return createdPromotion;
    }

    private Promotion sendCockpitCommand(Promotion promotion, AuditInfo auditInfo) {
        var cockpitReplyStatus = cockpitPromotionLegacyWrapper.requestPromotion(
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            promotion
        );

        var updated = promotionCrudService.update(
            promotion
                .toBuilder()
                .status(cockpitReplyStatus != CockpitReplyStatus.SUCCEEDED ? PromotionStatus.ERROR : PromotionStatus.TO_BE_VALIDATED)
                .updatedAt(new Date())
                .build()
        );

        if (cockpitReplyStatus != CockpitReplyStatus.SUCCEEDED) {
            throw new TechnicalManagementException("An error occurs while sending promotion request to cockpit");
        }

        return updated;
    }
}
