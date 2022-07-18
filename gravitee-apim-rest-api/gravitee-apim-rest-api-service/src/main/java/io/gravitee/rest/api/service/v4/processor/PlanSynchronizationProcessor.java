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
package io.gravitee.rest.api.service.v4.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.rest.api.model.DeploymentRequired;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component("PlanSynchronizationProcessorV4")
@Slf4j
public class PlanSynchronizationProcessor {

    private final ObjectMapper objectMapper;

    @Autowired
    public PlanSynchronizationProcessor(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean processCheckSynchronization(PlanEntity deployedPlan, PlanEntity planToDeploy) {
        Class<PlanEntity> cl = PlanEntity.class;
        List<Object> requiredFieldsDeployedPlan = new ArrayList<>();
        List<Object> requiredFieldsPlanToDeploy = new ArrayList<>();
        for (Field f : cl.getDeclaredFields()) {
            if (f.getAnnotation(DeploymentRequired.class) != null) {
                boolean previousAccessibleState = f.isAccessible();
                f.setAccessible(true);
                try {
                    requiredFieldsDeployedPlan.add(f.get(deployedPlan));
                    requiredFieldsPlanToDeploy.add(f.get(planToDeploy));
                } catch (Exception e) {
                    log.error("Error access Plan required deployment fields", e);
                } finally {
                    f.setAccessible(previousAccessibleState);
                }
            }
        }

        try {
            String requiredFieldsDeployedPlanDefinition = objectMapper.writeValueAsString(requiredFieldsDeployedPlan);
            String requiredFieldsPlanToDeployDefinition = objectMapper.writeValueAsString(requiredFieldsPlanToDeploy);

            return requiredFieldsDeployedPlanDefinition.equals(requiredFieldsPlanToDeployDefinition);
        } catch (Exception e) {
            log.error("Unexpected error while generating Plan deployment required fields definition", e);
            return false;
        }
    }
}
