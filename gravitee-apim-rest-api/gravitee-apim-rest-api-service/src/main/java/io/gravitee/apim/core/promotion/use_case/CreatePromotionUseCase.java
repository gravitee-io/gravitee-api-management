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
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.ApiExportDomainService;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.import_definition.GraviteeDefinition;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.Excludable;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.cockpit.model.CockpitReplyStatus;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.json.GraviteeDefinitionSerializer;
import io.gravitee.apim.core.json.JsonProcessingException;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.promotion.crud_service.PromotionCrudService;
import io.gravitee.apim.core.promotion.exception.PromotionAlreadyInProgressException;
import io.gravitee.apim.core.promotion.model.Promotion;
import io.gravitee.apim.core.promotion.model.PromotionAuthor;
import io.gravitee.apim.core.promotion.model.PromotionRequest;
import io.gravitee.apim.core.promotion.model.PromotionStatus;
import io.gravitee.apim.core.promotion.query_service.PromotionQueryService;
import io.gravitee.apim.core.promotion.service_provider.CockpitPromotionServiceProvider;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@UseCase
@Slf4j
public class CreatePromotionUseCase {

    private final ApiExportDomainService apiExportDomainService;
    private final EnvironmentCrudService environmentCrudService;
    private final PromotionQueryService promotionQueryService;
    private final GraviteeDefinitionSerializer graviteeDefinitionSerializer;
    private final PromotionCrudService promotionCrudService;
    private final AuditDomainService auditService;
    private final CockpitPromotionServiceProvider cockpitPromotionServiceProvider;
    private final UserCrudService userCrudService;
    private final ApiCrudService apiCrudService;
    private final PlanCrudService planCrudService;
    private final PageCrudService pageCrudService;

    public record Input(String apiId, PromotionRequest promotionRequest, AuditInfo auditInfo) {}

    public record Output(Promotion promotion) {}

    public CreatePromotionUseCase(
        ApiExportDomainService apiExportDomainService,
        EnvironmentCrudService environmentCrudService,
        PromotionQueryService promotionQueryService,
        GraviteeDefinitionSerializer graviteeDefinitionSerializer,
        PromotionCrudService promotionCrudService,
        AuditDomainService auditService,
        CockpitPromotionServiceProvider cockpitPromotionServiceProvider,
        UserCrudService userCrudService,
        ApiCrudService apiCrudService,
        PlanCrudService planCrudService,
        PageCrudService pageCrudService
    ) {
        this.apiExportDomainService = apiExportDomainService;
        this.environmentCrudService = environmentCrudService;
        this.promotionQueryService = promotionQueryService;
        this.graviteeDefinitionSerializer = graviteeDefinitionSerializer;
        this.promotionCrudService = promotionCrudService;
        this.auditService = auditService;
        this.cockpitPromotionServiceProvider = cockpitPromotionServiceProvider;
        this.userCrudService = userCrudService;
        this.apiCrudService = apiCrudService;
        this.planCrudService = planCrudService;
        this.pageCrudService = pageCrudService;
    }

    public Output execute(Input input) {
        var api = apiCrudService.get(input.apiId);
        var createdPromotion = switch (api.getDefinitionVersion()) {
            case V2 -> cockpitPromotionServiceProvider.createPromotion(
                input.apiId,
                input.promotionRequest,
                input.auditInfo.actor().userId()
            );
            case V4 -> createPromotion(api, input.promotionRequest, input.auditInfo);
            default -> throw new ApiInvalidDefinitionVersionException(input.apiId);
        };
        var updatedPromotion = sendCockpitCommand(createdPromotion, input.auditInfo);
        return new Output(updatedPromotion);
    }

    private Promotion createPromotion(Api api, PromotionRequest promotionRequest, AuditInfo auditInfo) {
        generateCrossIds(api);
        var apiId = api.getId();
        var authenticatedUser = userCrudService.getBaseUser(auditInfo.actor().userId());
        var apiDefinition = apiExportDomainService.export(apiId, auditInfo, Set.of(Excludable.GROUPS, Excludable.MEMBERS, Excludable.IDS));
        var sourceEnvironment = environmentCrudService.get(auditInfo.environmentId());

        var promotionQuery = new PromotionQueryService.PromotionQuery(
            apiId,
            Set.of(promotionRequest.getTargetEnvCockpitId()),
            Set.of(PromotionStatus.CREATED, PromotionStatus.TO_BE_VALIDATED),
            null
        );

        List<Promotion> inProgressPromotions = promotionQueryService.search(promotionQuery).getContent();
        if (!inProgressPromotions.isEmpty()) {
            Promotion promotion = inProgressPromotions.getFirst();
            throw new PromotionAlreadyInProgressException(promotion.getId());
        }

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
                .sourceEnvCockpitId(sourceEnvironment.getCockpitId())
                .sourceEnvName(sourceEnvironment.getName())
                .targetEnvCockpitId(promotionRequest.getTargetEnvCockpitId())
                .targetEnvName(promotionRequest.getTargetEnvName())
                .createdAt(TimeProvider.now().toInstant())
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
                .createdAt(createdPromotion.getCreatedAt().atZone(ZoneId.of("UTC")))
                .properties(Collections.emptyMap())
                .build()
        );

        return createdPromotion;
    }

    private Promotion sendCockpitCommand(Promotion promotion, AuditInfo auditInfo) {
        var cockpitReplyStatus = cockpitPromotionServiceProvider.requestPromotion(
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            promotion
        );

        var updated = promotionCrudService.update(
            promotion
                .toBuilder()
                .status(cockpitReplyStatus != CockpitReplyStatus.SUCCEEDED ? PromotionStatus.ERROR : PromotionStatus.TO_BE_VALIDATED)
                .updatedAt(TimeProvider.now().toInstant())
                .build()
        );

        if (cockpitReplyStatus != CockpitReplyStatus.SUCCEEDED) {
            throw new TechnicalManagementException("An error occurs while sending promotion request to cockpit");
        }

        return updated;
    }

    public void generateCrossIds(Api api) {
        if (StringUtils.isEmpty(api.getCrossId())) {
            api.setCrossId(UuidString.generateRandom());
            apiCrudService.update(api);
        }
        var plans = planCrudService
            .findByApiId(api.getId())
            .stream()
            .peek(plan -> {
                if (StringUtils.isEmpty(plan.getCrossId())) {
                    plan.setCrossId(UuidString.generateRandom());
                }
            })
            .toList();
        planCrudService.updateCrossIds(plans);

        var pages = pageCrudService
            .findByApiId(api.getId())
            .stream()
            .peek(page -> {
                if (StringUtils.isEmpty(page.getCrossId())) {
                    page.setCrossId(UuidString.generateRandom());
                }
            })
            .toList();
        pageCrudService.updateCrossIds(pages);
    }
}
