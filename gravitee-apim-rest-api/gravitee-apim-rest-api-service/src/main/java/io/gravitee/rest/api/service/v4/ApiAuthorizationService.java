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

import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Set;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApiAuthorizationService {
    boolean canManageApi(RoleEntity role);

    boolean canConsumeApi(ExecutionContext executionContext, String userId, GenericApiEntity apiEntity);

    default Set<String> findAccessibleApiIdsForUser(final ExecutionContext executionContext, final String userId) {
        return findAccessibleApiIdsForUser(executionContext, userId, new ApiQuery());
    }

    default Set<String> findAccessibleApiIdsForUser(
        final ExecutionContext executionContext,
        final String userId,
        final Set<String> apiIds
    ) {
        ApiQuery apiQuery = new ApiQuery();
        apiQuery.setIds(apiIds);
        return findAccessibleApiIdsForUser(executionContext, userId, apiQuery);
    }

    Set<String> findAccessibleApiIdsForUser(final ExecutionContext executionContext, final String userId, final ApiQuery apiQuery);

    default Set<String> findIdsByUser(final ExecutionContext executionContext, final String userId, final boolean manageOnly) {
        return this.findIdsByUser(executionContext, userId, new ApiQuery(), null, manageOnly);
    }

    default Set<String> findIdsByUser(
        final ExecutionContext executionContext,
        final String userId,
        final ApiQuery apiQuery,
        final boolean manageOnly
    ) {
        return this.findIdsByUser(executionContext, userId, apiQuery, null, manageOnly);
    }

    Set<String> findIdsByUser(
        final ExecutionContext executionContext,
        final String userId,
        final ApiQuery apiQuery,
        final Sortable sortable,
        final boolean manageOnly
    );

    Set<String> findApiIdsByUserId(ExecutionContext executionContext, String userId, ApiQuery apiQuery);

    List<ApiCriteria> computeApiCriteriaForUser(ExecutionContext executionContext, String userId, ApiQuery apiQuery, boolean manageOnly);
}
