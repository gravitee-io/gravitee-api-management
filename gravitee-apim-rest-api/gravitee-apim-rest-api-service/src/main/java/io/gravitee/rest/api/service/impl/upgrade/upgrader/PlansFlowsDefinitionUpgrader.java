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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * This upgrader moves API flows and plans flows data from apis.definition to flows collection.
 *
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class PlansFlowsDefinitionUpgrader implements Upgrader {

    @Lazy
    @Autowired
    private ApiRepository apiRepository;

    @Lazy
    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private FlowService flowService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        return UpgraderOrder.PLANS_FLOWS_DEFINITION_UPGRADER;
    }

    @Override
    public boolean upgrade() {
        try {
            AtomicBoolean upgradeFailed = new AtomicBoolean(false);
            apiRepository
                .search(
                    new ApiCriteria.Builder().definitionVersion(List.of(DefinitionVersion.V2)).build(),
                    null,
                    ApiFieldFilter.allFields()
                )
                .forEach(api -> {
                    try {
                        io.gravitee.definition.model.Api apiDefinition = objectMapper.readValue(
                            api.getDefinition(),
                            io.gravitee.definition.model.Api.class
                        );
                        migrateApiFlows(api.getId(), apiDefinition);
                    } catch (Exception e) {
                        upgradeFailed.set(true);
                        throw new RuntimeException(e);
                    }
                });

            return !upgradeFailed.get();
        } catch (Exception e) {
            log.error("Error applying upgrader", e);
            return false;
        }
    }

    protected void migrateApiFlows(String apiId, io.gravitee.definition.model.Api apiDefinition) throws Exception {
        log.debug("Migrate flows for api [{}]", apiId);
        Map<String, Plan> plansById = planRepository.findByApi(apiId).stream().collect(toMap(Plan::getId, Function.identity()));

        flowService.save(FlowReferenceType.API, apiDefinition.getId(), apiDefinition.getFlows());

        apiDefinition
            .getPlans()
            .forEach(apiDefinitionPlan -> {
                if (
                    apiDefinitionPlan.getFlows() != null &&
                    !apiDefinitionPlan.getFlows().isEmpty() &&
                    plansById.containsKey(apiDefinitionPlan.getId())
                ) {
                    flowService.save(FlowReferenceType.PLAN, apiDefinitionPlan.getId(), apiDefinitionPlan.getFlows());
                }
            });
    }
}
