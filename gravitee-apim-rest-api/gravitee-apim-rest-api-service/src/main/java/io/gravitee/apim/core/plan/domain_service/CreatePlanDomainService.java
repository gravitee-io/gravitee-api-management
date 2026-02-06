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
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.ApiProductAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.PlanAuditEvent;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.listener.AbstractListener;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
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

    public PlanWithFlows create(Plan plan, List<? extends AbstractFlow> flows, Api api, AuditInfo auditInfo) {
        return switch (api.getDefinitionVersion()) {
            case V4 -> createV4ApiPlan(plan, flows, api, auditInfo);
            case FEDERATED, FEDERATED_AGENT -> createFederatedApiPlan(plan, auditInfo);
            case V2 -> createV2ApiPlan(plan, flows, api, auditInfo);
            case V1 -> throw new IllegalStateException(api.getDefinitionVersion() + " is not supported");
            case null -> throw new IllegalStateException(api.getDefinitionVersion() + " is not supported");
        };
    }

    private PlanWithFlows createV4ApiPlan(Plan plan, List<? extends AbstractFlow> flows, Api api, AuditInfo auditInfo) {
        if (api.isDeprecated()) {
            throw new ApiDeprecatedException(plan.getReferenceId());
        }

        var listeners = api.getApiListeners();

        if (!listeners.isEmpty()) {
            planValidatorDomainService.validatePlanSecurityAgainstEntrypoints(
                plan.getPlanSecurity(),
                listeners.stream().map(AbstractListener::getType).toList()
            );
        }

        planValidatorDomainService.validatePlanSecurity(plan, auditInfo.organizationId(), auditInfo.environmentId(), api.getType());
        planValidatorDomainService.validatePlanTagsAgainstApiTags(plan.getTags(), api.getTags());
        planValidatorDomainService.validateGeneralConditionsPageStatus(plan);

        if (api.isNative()) {
            return createNativeV4ApiPlan(plan, (List<NativeFlow>) flows, api, auditInfo);
        }

        return createHttpV4ApiPlan(plan, (List<Flow>) flows, api, auditInfo);
    }

    private PlanWithFlows createNativeV4ApiPlan(Plan plan, List<NativeFlow> flows, Api api, AuditInfo auditInfo) {
        var sanitizedFlows = flowValidationDomainService.validateAndSanitizeNativeV4(flows);
        var createdPlan = planCrudService.create(
            plan
                .toBuilder()
                .id(plan.getId() != null ? plan.getId() : UuidString.generateRandom())
                .referenceType(GenericPlanEntity.ReferenceType.API)
                .referenceId(api.getId())
                .environmentId(auditInfo.environmentId())
                .definitionVersion(api.getDefinitionVersion())
                .apiType(api.getType())
                .createdAt(TimeProvider.now())
                .updatedAt(TimeProvider.now())
                .needRedeployAt(Date.from(TimeProvider.instantNow()))
                .publishedAt(plan.isPublished() ? TimeProvider.now() : null)
                .build()
        );

        var createdFlows = flowCrudService.saveNativePlanFlows(createdPlan.getId(), sanitizedFlows);

        createAuditLog(createdPlan, auditInfo);

        return new PlanWithFlows(createdPlan, createdFlows);
    }

    private PlanWithFlows createHttpV4ApiPlan(Plan plan, List<Flow> flows, Api api, AuditInfo auditInfo) {
        var sanitizedFlows = flowValidationDomainService.validateAndSanitizeHttpV4(api.getType(), flows);
        flowValidationDomainService.validatePathParameters(
            api.getType(),
            api.getApiDefinitionHttpV4().getFlows() != null ? api.getApiDefinitionHttpV4().getFlows().stream() : Stream.empty(),
            sanitizedFlows.stream()
        );

        var createdPlan = planCrudService.create(
            plan
                .toBuilder()
                .id(plan.getId() != null ? plan.getId() : UuidString.generateRandom())
                .apiType(api.getType())
                .referenceType(GenericPlanEntity.ReferenceType.API)
                .referenceId(api.getId())
                .environmentId(auditInfo.environmentId())
                .createdAt(TimeProvider.now())
                .updatedAt(TimeProvider.now())
                .needRedeployAt(Date.from(TimeProvider.instantNow()))
                .publishedAt(plan.isPublished() ? TimeProvider.now() : null)
                .build()
        );

        var createdFlows = flowCrudService.savePlanFlows(createdPlan.getId(), sanitizedFlows);

        createAuditLog(createdPlan, auditInfo);

        return new PlanWithFlows(createdPlan, createdFlows);
    }

    private PlanWithFlows createFederatedApiPlan(Plan plan, AuditInfo auditInfo) {
        var createdPlan = planCrudService.create(plan);
        createAuditLog(createdPlan, auditInfo);
        return new PlanWithFlows(createdPlan, Collections.emptyList());
    }

    private PlanWithFlows createV2ApiPlan(Plan plan, List<? extends AbstractFlow> flows, Api api, AuditInfo auditInfo) {
        if (api.isDeprecated()) {
            throw new ApiDeprecatedException(plan.getReferenceId());
        }

        planValidatorDomainService.validatePlanSecurity(plan, auditInfo.organizationId(), auditInfo.environmentId(), api.getType());
        planValidatorDomainService.validatePlanTagsAgainstApiTags(plan.getTags(), api.getTags());
        planValidatorDomainService.validateGeneralConditionsPageStatus(plan);

        // TODO handle flows

        var createdPlan = planCrudService.create(
            plan
                .toBuilder()
                .id(plan.getId() != null ? plan.getId() : UuidString.generateRandom())
                .apiType(api.getType())
                .referenceType(GenericPlanEntity.ReferenceType.API)
                .referenceId(api.getId())
                .environmentId(auditInfo.environmentId())
                .definitionVersion(api.getDefinitionVersion())
                .createdAt(TimeProvider.now())
                .updatedAt(TimeProvider.now())
                .needRedeployAt(Date.from(TimeProvider.instantNow()))
                .publishedAt(plan.isPublished() ? TimeProvider.now() : null)
                .build()
        );

        createAuditLog(createdPlan, auditInfo);

        return new PlanWithFlows(createdPlan, flows);
    }

    public Plan createApiProductPlan(Plan plan, ApiProduct apiProduct, AuditInfo auditInfo) {
        //TODO handle deprecated api product
        /*if (apiProduct.isDeprecated()) {
            throw new ApiDeprecatedException(plan.getApiId());
        }*/

        planValidatorDomainService.validatePlanSecurity(plan, auditInfo.organizationId(), auditInfo.environmentId(), null);
        planValidatorDomainService.validateGeneralConditionsPageStatus(plan);
        var createdPlan = planCrudService.create(
            plan
                .toBuilder()
                .id(plan.getId() != null ? plan.getId() : UuidString.generateRandom())
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .referenceId(apiProduct.getId())
                .createdAt(TimeProvider.now())
                .updatedAt(TimeProvider.now())
                .needRedeployAt(Date.from(TimeProvider.instantNow()))
                .publishedAt(plan.isPublished() ? TimeProvider.now() : null)
                .build()
        );
        createApiProductAuditLog(createdPlan, auditInfo);
        return createdPlan;
    }

    private void createAuditLog(Plan createdPlan, AuditInfo auditInfo) {
        auditService.createApiAuditLog(
            ApiAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiId(createdPlan.getReferenceId())
                .event(PlanAuditEvent.PLAN_CREATED)
                .actor(auditInfo.actor())
                .newValue(createdPlan)
                .createdAt(createdPlan.getCreatedAt())
                .properties(Map.of(AuditProperties.PLAN, createdPlan.getId()))
                .build()
        );
    }

    private void createApiProductAuditLog(Plan createdPlan, AuditInfo auditInfo) {
        auditService.createApiProductAuditLog(
            ApiProductAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiProductId(createdPlan.getReferenceId())
                .event(PlanAuditEvent.PLAN_CREATED)
                .actor(auditInfo.actor())
                .newValue(createdPlan)
                .createdAt(createdPlan.getCreatedAt())
                .properties(Map.of(AuditProperties.PLAN, createdPlan.getId()))
                .build()
        );
    }
}
