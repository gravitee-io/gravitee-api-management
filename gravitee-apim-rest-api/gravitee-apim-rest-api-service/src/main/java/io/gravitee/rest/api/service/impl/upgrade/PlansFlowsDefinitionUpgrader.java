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
package io.gravitee.rest.api.service.impl.upgrade;

import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This upgrader moves plans flows data from apis.definition to plans.flows.
 *
 * @author GraviteeSource Team
 */
@Component
public class PlansFlowsDefinitionUpgrader extends OneShotUpgrader {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlansFlowsDefinitionUpgrader.class);

    @Autowired
    private ApiRepository apiRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private FlowService flowService;

    @Autowired
    private ObjectMapper objectMapper;

    public PlansFlowsDefinitionUpgrader() {
        super(InstallationService.PLANS_FLOWS_UPGRADER_STATUS);
    }

    @Override
    public int getOrder() {
        return 550;
    }

    @Override
    protected void processOneShotUpgrade() throws Exception {
        for (Api api : apiRepository.findAll()) {
            io.gravitee.definition.model.Api apiDefinition = objectMapper.readValue(
                api.getDefinition(),
                io.gravitee.definition.model.Api.class
            );

            if (DefinitionVersion.V2 == apiDefinition.getDefinitionVersion()) {
                migrateApiPlansFlows(api.getId(), apiDefinition);
            }
        }
    }

    protected void migrateApiPlansFlows(String apiId, io.gravitee.definition.model.Api apiDefinition) throws Exception {
        Map<String, Plan> plansById = planRepository.findByApi(apiId).stream().collect(toMap(Plan::getId, Function.identity()));

        apiDefinition
            .getPlans()
            .forEach(
                apiDefinitionPlan -> {
                    if (
                        apiDefinitionPlan.getFlows() != null &&
                        !apiDefinitionPlan.getFlows().isEmpty() &&
                        plansById.containsKey(apiDefinitionPlan.getId())
                    ) {
                        flowService.save(FlowReferenceType.PLAN, apiDefinitionPlan.getId(), apiDefinitionPlan.getFlows());
                    }
                }
            );
    }
}
