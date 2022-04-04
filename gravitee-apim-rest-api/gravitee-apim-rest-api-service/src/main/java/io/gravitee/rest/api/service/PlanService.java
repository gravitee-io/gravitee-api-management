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
package io.gravitee.rest.api.service;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.NewPlanEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlansConfigurationEntity;
import io.gravitee.rest.api.model.UpdatePlanEntity;
import io.gravitee.rest.api.model.plan.PlanQuery;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface PlanService {
    PlanEntity findById(ExecutionContext executionContext, String plan);

    Set<PlanEntity> findByIdIn(ExecutionContext executionContext, Set<String> ids);

    Set<PlanEntity> findByApi(ExecutionContext executionContext, String api);

    List<PlanEntity> search(ExecutionContext executionContext, PlanQuery query);

    PlanEntity create(ExecutionContext executionContext, NewPlanEntity plan);

    PlanEntity update(ExecutionContext executionContext, UpdatePlanEntity plan);
    PlanEntity update(ExecutionContext executionContext, UpdatePlanEntity plan, boolean fromImport);

    PlanEntity close(ExecutionContext executionContext, String plan, String username);

    void delete(ExecutionContext executionContext, String plan);

    PlanEntity publish(ExecutionContext executionContext, String plan);

    PlanEntity deprecate(ExecutionContext executionContext, String plan);

    PlanEntity deprecate(ExecutionContext executionContext, String plan, boolean allowStaging);

    PlansConfigurationEntity getConfiguration();

    PlanEntity createOrUpdatePlan(ExecutionContext executionContext, PlanEntity planEntity);

    boolean anyPlanMismatchWithApi(List<String> planIds, String apiId);

    Map<String, Object> findByIdAsMap(String id) throws TechnicalException;
}
