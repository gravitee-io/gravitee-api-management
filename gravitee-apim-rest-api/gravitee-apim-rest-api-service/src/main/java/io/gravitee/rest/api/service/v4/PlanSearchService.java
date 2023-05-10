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
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanQuery;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Guillaume LAMIRAND (final guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface PlanSearchService {
    GenericPlanEntity findById(final ExecutionContext executionContext, final String plan);

    Set<GenericPlanEntity> findByIdIn(final ExecutionContext executionContext, final Set<String> ids);

    Set<GenericPlanEntity> findByApi(final ExecutionContext executionContext, final String apiId);

    List<GenericPlanEntity> search(final ExecutionContext executionContext, final PlanQuery query, String user, boolean isAdmin);

    boolean anyPlanMismatchWithApi(List<String> planIds, String apiId);

    Map<String, Object> findByIdAsMap(String id) throws TechnicalException;
}
