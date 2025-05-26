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
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.v4.FlowService;
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

    public GenericPlanEntity toGenericPlan(final Api api, final Plan plan) {
        var apiDefinitionVersion = api.getDefinitionVersion() != null ? api.getDefinitionVersion() : DefinitionVersion.V2;
        return switch (apiDefinitionVersion) {
            case V4 -> switch (api.getType()) {
                case PROXY, MESSAGE -> planMapper.toEntity(plan, flowService.findByReference(FlowReferenceType.PLAN, plan.getId()));
                case NATIVE -> planMapper.toNativeEntity(plan, flowCrudService.getNativePlanFlows(plan.getId()));
            };
            case FEDERATED, FEDERATED_AGENT -> planMapper.toEntity(plan, null);
            default -> planConverter.toPlanEntity(plan, flowServiceV2.findByReference(FlowReferenceType.PLAN, plan.getId()));
        };
    }
}
