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
package io.gravitee.apim.core.plan.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.exception.ApiDeprecatedException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.PlanAuditEvent;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.rest.api.service.common.UuidString;
import java.sql.Date;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@DomainService
public class CreatePlanDomainService {

    private final PlanValidatorDomainService planValidatorDomainService;
    private final FlowValidationDomainService flowValidationDomainService;
    private final PlanCrudService planCrudService;
    private final FlowCrudService flowCrudService;
    private final AuditDomainService auditService;

    public CreatePlanDomainService(
        PlanValidatorDomainService planValidatorDomainService,
        FlowValidationDomainService flowValidationDomainService,
        PlanCrudService planCrudService,
        FlowCrudService flowCrudService,
        AuditDomainService auditDomainService
    ) {
        this.planValidatorDomainService = planValidatorDomainService;
        this.flowValidationDomainService = flowValidationDomainService;
        this.planCrudService = planCrudService;
        this.flowCrudService = flowCrudService;
        this.auditService = auditDomainService;
    }

    public PlanWithFlows create(Plan plan, List<Flow> flows, Api api, AuditInfo auditInfo) {
        return switch (api.getDefinitionVersion()) {
            case V4 -> createV4ApiPlan(plan, flows, api, auditInfo);
            case FEDERATED -> createFederatedApiPlan(plan, auditInfo);
            default -> throw new IllegalStateException(api.getDefinitionVersion() + " is not supported");
        };
    }

    private PlanWithFlows createV4ApiPlan(Plan plan, List<Flow> flows, Api api, AuditInfo auditInfo) {
        if (api.isDeprecated()) {
            throw new ApiDeprecatedException(plan.getApiId());
        }

        if (api.getApiDefinitionV4().getListeners() != null) {
            planValidatorDomainService.validatePlanSecurityAgainstEntrypoints(
                plan.getPlanSecurity(),
                api.getApiDefinitionV4().getListeners().stream().map(Listener::getType).toList()
            );
        }

        planValidatorDomainService.validatePlanSecurity(plan, auditInfo.organizationId(), auditInfo.environmentId());
        planValidatorDomainService.validatePlanTagsAgainstApiTags(plan.getPlanDefinitionHttpV4().getTags(), api.getTags());
        planValidatorDomainService.validateGeneralConditionsPageStatus(plan);

        var sanitizedFlows = flowValidationDomainService.validateAndSanitizeHttpV4(api.getType(), flows);
        flowValidationDomainService.validatePathParameters(
            api.getType(),
            api.getApiDefinitionV4().getFlows() != null ? api.getApiDefinitionV4().getFlows().stream() : Stream.empty(),
            sanitizedFlows.stream()
        );

        var createdPlan = planCrudService.create(
            plan
                .toBuilder()
                .id(plan.getId() != null ? plan.getId() : UuidString.generateRandom())
                .apiId(api.getId())
                .createdAt(TimeProvider.now())
                .updatedAt(TimeProvider.now())
                .needRedeployAt(Date.from(TimeProvider.instantNow()))
                .publishedAt(plan.isPublished() ? TimeProvider.now() : null)
                .build()
        );

        var createdFlows = flowCrudService.savePlanFlows(createdPlan.getId(), sanitizedFlows);

        createAuditLog(createdPlan, auditInfo);

        return toPlanWithFlows(createdPlan, createdFlows);
    }

    private PlanWithFlows createFederatedApiPlan(Plan plan, AuditInfo auditInfo) {
        var createdPlan = planCrudService.create(plan);
        createAuditLog(createdPlan, auditInfo);
        return toPlanWithFlows(plan, Collections.emptyList());
    }

    private void createAuditLog(Plan createdPlan, AuditInfo auditInfo) {
        auditService.createApiAuditLog(
            ApiAuditLogEntity
                .builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiId(createdPlan.getApiId())
                .event(PlanAuditEvent.PLAN_CREATED)
                .actor(auditInfo.actor())
                .newValue(createdPlan)
                .createdAt(createdPlan.getCreatedAt())
                .properties(Map.of(AuditProperties.PLAN, createdPlan.getId()))
                .build()
        );
    }

    private PlanWithFlows toPlanWithFlows(Plan plan, List<Flow> flows) {
        return PlanWithFlows
            .builder()
            .flows(flows)
            .id(plan.getId())
            .crossId(plan.getCrossId())
            .name(plan.getName())
            .description(plan.getDescription())
            .createdAt(plan.getCreatedAt())
            .updatedAt(plan.getUpdatedAt())
            .publishedAt(plan.getPublishedAt())
            .closedAt(plan.getClosedAt())
            .needRedeployAt(plan.getNeedRedeployAt())
            .validation(plan.getValidation())
            .type(plan.getType())
            .planDefinitionHttpV4(plan.getPlanDefinitionHttpV4())
            .planDefinitionNativeV4(plan.getPlanDefinitionNativeV4())
            .planDefinitionV2(plan.getPlanDefinitionV2())
            .apiId(plan.getApiId())
            .order(plan.getOrder())
            .characteristics(plan.getCharacteristics())
            .excludedGroups(plan.getExcludedGroups())
            .commentRequired(plan.isCommentRequired())
            .commentMessage(plan.getCommentMessage())
            .generalConditions(plan.getGeneralConditions())
            .build();
    }
}
