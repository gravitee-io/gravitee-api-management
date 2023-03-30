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
package io.gravitee.rest.api.service.impl;

import static java.util.stream.Collectors.toSet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanStatus;
import io.gravitee.rest.api.service.ApiDefinitionService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.converter.FlowConverter;
import io.gravitee.rest.api.service.converter.PlanConverter;

import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
public class ApiDefinitionServiceImpl implements ApiDefinitionService {

    private ObjectMapper objectMapper;

    private PlanService planService;

    private PlanConverter planConverter;

    private FlowService flowService;

    public ApiDefinitionServiceImpl(
        ObjectMapper objectMapper,
        PlanService planService,
        PlanConverter planConverter,
        FlowService flowService
    ) {
        this.objectMapper = objectMapper;
        this.planService = planService;
        this.planConverter = planConverter;
        this.flowService = flowService;
    }

    /**
     * Build gateway API definition for given Api.
     *
     * It reads API plans from plan collections, and API flows from flow collection ;
     * And generates gateway API definition from management API definition (containing no plans or flows).
     *
     * @throws JsonProcessingException
     */
    public io.gravitee.definition.model.Api buildGatewayApiDefinition(ExecutionContext executionContext, Api api)
        throws JsonProcessingException {
        var apiDefinition = objectMapper.readValue(api.getDefinition(), io.gravitee.definition.model.Api.class);

        Set<PlanEntity> plans = planService
            .findByApi(executionContext, api.getId())
            .stream()
            .filter(p -> !List.of(PlanStatus.CLOSED, PlanStatus.STAGING).contains(p.getStatus()))
            .collect(toSet());

        apiDefinition.setPlans(planConverter.toPlansDefinitions(plans));
        apiDefinition.setFlows(FlowConverter.toFlows(flowService.findByReference(FlowReferenceType.API, api.getId())));
        return apiDefinition;
    }
}
