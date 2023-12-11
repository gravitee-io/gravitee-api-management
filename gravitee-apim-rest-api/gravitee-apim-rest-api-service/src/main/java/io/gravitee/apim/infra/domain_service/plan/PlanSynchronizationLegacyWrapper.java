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
package io.gravitee.apim.infra.domain_service.plan;

import io.gravitee.apim.core.plan.domain_service.PlanSynchronizationService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.infra.adapter.PlanAdapter;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.rest.api.model.v4.plan.BasePlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.processor.SynchronizationService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PlanSynchronizationLegacyWrapper implements PlanSynchronizationService {

    private final SynchronizationService synchronizationService;

    public PlanSynchronizationLegacyWrapper(SynchronizationService synchronizationService) {
        this.synchronizationService = synchronizationService;
    }

    @Override
    public boolean checkSynchronized(Plan oldPlan, List<Flow> oldFlows, Plan newPlan, List<Flow> newFlows) {
        var oldPlanEntity = PlanAdapter.INSTANCE.toEntityV4(oldPlan);
        oldPlanEntity.setFlows(oldFlows);
        var newPlanEntity = PlanAdapter.INSTANCE.toEntityV4(newPlan);
        newPlanEntity.setFlows(newFlows);

        return (
            synchronizationService.checkSynchronization(PlanEntity.class, oldPlanEntity, newPlanEntity) &&
            synchronizationService.checkSynchronization(BasePlanEntity.class, oldPlanEntity, newPlanEntity)
        );
    }
}
