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
package io.gravitee.rest.api.service.v4.mapper;

import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativeApiEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.v4.FlowService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class GenericPlanMapper {

    private final PlanMapper planMapper;
    private final FlowService flowService;
    private final PlanConverter planConverter;
    private final io.gravitee.rest.api.service.configuration.flow.FlowService flowServiceV2;
    private final FlowCrudService flowCrudService;

    public GenericPlanMapper(
        final PlanMapper planMapper,
        @Lazy final FlowService flowService,
        final PlanConverter planConverter,
        @Lazy final io.gravitee.rest.api.service.configuration.flow.FlowService flowServiceV2,
        @Lazy final FlowCrudService flowCrudService
    ) {
        this.planMapper = planMapper;
        this.flowService = flowService;
        this.planConverter = planConverter;
        this.flowServiceV2 = flowServiceV2;
        this.flowCrudService = flowCrudService;
    }

    public GenericPlanEntity toGenericPlanWithFlow(final Api api, final Plan plan) {
        return toGenericPlansWithFlow(api, Set.of(plan)).iterator().next();
    }

    public Set<GenericPlanEntity> toGenericPlansWithFlow(final Api api, final Set<Plan> plans) {
        var apiDefinitionVersion = api.getDefinitionVersion() != null ? api.getDefinitionVersion() : DefinitionVersion.V2;
        final Set<String> planIds = plans.stream().map(Plan::getId).collect(Collectors.toSet());

        return switch (apiDefinitionVersion) {
            case V4 -> switch (api.getType()) {
                case A2A_PROXY, LLM_PROXY, MCP_PROXY, PROXY, MESSAGE -> {
                    final Map<String, List<Flow>> flowsByPlanId = flowService != null
                        ? flowService.findByReferences(FlowReferenceType.PLAN, planIds)
                        : Map.of();

                    yield plans
                        .stream()
                        .map(plan -> planMapper.toEntity(plan, flowsByPlanId.getOrDefault(plan.getId(), Collections.emptyList())))
                        .collect(Collectors.toSet());
                }
                case NATIVE -> {
                    final Map<String, List<NativeFlow>> nativeFlowsByPlanId = flowCrudService != null
                        ? flowCrudService.getNativePlanFlows(planIds)
                        : Map.of();

                    yield plans
                        .stream()
                        .map(plan ->
                            planMapper.toNativeEntity(plan, nativeFlowsByPlanId.getOrDefault(plan.getId(), Collections.emptyList()))
                        )
                        .collect(Collectors.toSet());
                }
            };
            case FEDERATED, FEDERATED_AGENT -> handleGenericPlanWithoutFlow(plans);
            default -> {
                final Map<String, List<io.gravitee.definition.model.flow.Flow>> v2FlowsByPlanId = flowCrudService != null
                    ? flowCrudService.getPlanV2Flows(planIds)
                    : Map.of();
                yield plans
                    .stream()
                    .map(plan -> planConverter.toPlanEntity(plan, v2FlowsByPlanId.getOrDefault(plan.getId(), Collections.emptyList())))
                    .collect(Collectors.toSet());
            }
        };
    }

    public GenericPlanEntity toGenericPlanWithFlow(final GenericApiEntity api, final Plan plan) {
        var apiDefinitionVersion = api.getDefinitionVersion() != null ? api.getDefinitionVersion() : DefinitionVersion.V2;
        return switch (apiDefinitionVersion) {
            case V4 -> {
                if (api instanceof NativeApiEntity) {
                    yield planMapper.toNativeEntity(plan, flowCrudService.getNativePlanFlows(plan.getId()));
                }
                if (api instanceof ApiEntity) {
                    yield planMapper.toEntity(plan, flowService.findByReference(FlowReferenceType.PLAN, plan.getId()));
                }
                throw new IllegalStateException("Unsupported GenericApiEntity type");
            }
            case FEDERATED, FEDERATED_AGENT -> planMapper.toEntity(plan, null);
            default -> planConverter.toPlanEntity(plan, flowServiceV2.findByReference(FlowReferenceType.PLAN, plan.getId()));
        };
    }

    private @NonNull Set<GenericPlanEntity> handleGenericPlanWithoutFlow(Set<Plan> plans) {
        return plans
            .stream()
            .map(plan -> planMapper.toEntity(plan, null))
            .collect(Collectors.toSet());
    }

    public GenericPlanEntity toGenericPlanWithoutFlow(final Api api, final Plan plan) {
        var apiDefinitionVersion = api.getDefinitionVersion() != null ? api.getDefinitionVersion() : DefinitionVersion.V2;
        return switch (apiDefinitionVersion) {
            case V4 -> switch (api.getType()) {
                case A2A_PROXY, LLM_PROXY, MCP_PROXY, PROXY, MESSAGE -> planMapper.toEntity(plan, null);
                case NATIVE -> planMapper.toNativeEntity(plan, null);
            };
            case FEDERATED, FEDERATED_AGENT -> planMapper.toEntity(plan, null);
            default -> planConverter.toPlanEntity(plan, null);
        };
    }

    public Set<GenericPlanEntity> toGenericPlansWithoutFlow(final Api api, final Set<Plan> plans) {
        var apiDefinitionVersion = api.getDefinitionVersion() != null ? api.getDefinitionVersion() : DefinitionVersion.V2;
        return switch (apiDefinitionVersion) {
            case V4 -> switch (api.getType()) {
                case A2A_PROXY, LLM_PROXY, MCP_PROXY, PROXY, MESSAGE -> handleGenericPlanWithoutFlow(plans);
                case NATIVE -> plans
                    .stream()
                    .map(plan -> planMapper.toNativeEntity(plan, null))
                    .collect(Collectors.toSet());
            };
            case FEDERATED, FEDERATED_AGENT -> handleGenericPlanWithoutFlow(plans);
            default -> plans
                .stream()
                .map(plan -> planConverter.toPlanEntity(plan, null))
                .collect(Collectors.toSet());
        };
    }

    public GenericPlanEntity toGenericPlanWithoutFlow(final GenericApiEntity api, final Plan plan) {
        var apiDefinitionVersion = api.getDefinitionVersion() != null ? api.getDefinitionVersion() : DefinitionVersion.V2;
        return switch (apiDefinitionVersion) {
            case V4 -> {
                if (api instanceof NativeApiEntity) {
                    yield planMapper.toNativeEntity(plan, null);
                }
                if (api instanceof ApiEntity) {
                    yield planMapper.toEntity(plan, null);
                }
                throw new IllegalStateException("Unsupported GenericApiEntity type");
            }
            case FEDERATED, FEDERATED_AGENT -> planMapper.toEntity(plan, null);
            default -> planConverter.toPlanEntity(plan, null);
        };
    }
}
