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
package io.gravitee.rest.api.service.v4;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.PlansConfigurationEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativePlanEntity;
import io.gravitee.rest.api.model.v4.plan.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Guillaume LAMIRAND (final guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface PlanService {
    PlanEntity findById(ExecutionContext executionContext, String plan);

    Set<PlanEntity> findByApi(ExecutionContext executionContext, String api);
    Set<NativePlanEntity> findNativePlansByApi(ExecutionContext executionContext, String api);

    PlanEntity update(final ExecutionContext executionContext, final UpdatePlanEntity plan);

    GenericPlanEntity close(final ExecutionContext executionContext, final String plan);

    void delete(final ExecutionContext executionContext, final String plan);

    PlanEntity publish(final ExecutionContext executionContext, final String plan);

    PlanEntity deprecate(final ExecutionContext executionContext, final String plan);

    PlanEntity deprecate(final ExecutionContext executionContext, final String plan, final boolean allowStaging);

    PlansConfigurationEntity getConfiguration();

    PlanEntity createOrUpdatePlan(final ExecutionContext executionContext, final PlanEntity planEntity);

    boolean anyPlanMismatchWithApi(final List<String> planIds, final String apiId);

    Map<String, Object> findByIdAsMap(final String id) throws TechnicalException;
}
