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
package io.gravitee.rest.api.service.v4;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.PlansConfigurationEntity;
import io.gravitee.rest.api.model.v4.plan.NewPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanQuery;
import io.gravitee.rest.api.model.v4.plan.UpdatePlanEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Guillaume LAMIRAND (final guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface PlanService {
    PlanEntity findById(final ExecutionContext executionContext, final String plan);

    Set<PlanEntity> findByIdIn(final ExecutionContext executionContext, final Set<String> ids);

    Set<PlanEntity> findByApi(final ExecutionContext executionContext, final String apiId);

    List<PlanEntity> search(final ExecutionContext executionContext, final PlanQuery query);

    PlanEntity create(final ExecutionContext executionContext, final NewPlanEntity plan);

    PlanEntity update(final ExecutionContext executionContext, final UpdatePlanEntity plan);

    PlanEntity update(final ExecutionContext executionContext, final UpdatePlanEntity plan, final boolean fromImport);

    PlanEntity close(final ExecutionContext executionContext, final String plan, final String username);

    void delete(final ExecutionContext executionContext, final String plan);

    PlanEntity publish(final ExecutionContext executionContext, final String plan);

    PlanEntity deprecate(final ExecutionContext executionContext, final String plan);

    PlanEntity deprecate(final ExecutionContext executionContext, final String plan, final boolean allowStaging);

    PlansConfigurationEntity getConfiguration();

    PlanEntity createOrUpdatePlan(final ExecutionContext executionContext, final PlanEntity planEntity);

    boolean anyPlanMismatchWithApi(final List<String> planIds, final String apiId);

    Map<String, Object> findByIdAsMap(final String id) throws TechnicalException;
}
