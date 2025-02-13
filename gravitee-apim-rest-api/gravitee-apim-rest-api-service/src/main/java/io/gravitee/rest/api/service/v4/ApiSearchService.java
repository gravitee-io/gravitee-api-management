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

import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApiSearchService {
    ApiEntity findById(ExecutionContext executionContext, String apiId);

    GenericApiEntity findGenericById(final ExecutionContext executionContext, final String apiId);

    Set<GenericApiEntity> findAllGenericByEnvironment(ExecutionContext executionContext);

    Api findV4RepositoryApiById(ExecutionContext executionContext, String apiId);

    Api findRepositoryApiById(ExecutionContext executionContext, String apiId);

    Optional<String> findIdByEnvironmentIdAndCrossId(final String environment, final String crossId);

    boolean exists(final String apiId);

    Set<GenericApiEntity> findGenericByEnvironmentAndIdIn(ExecutionContext executionContext, Set<String> apiIds);

    Collection<GenericApiEntity> search(final ExecutionContext executionContext, final ApiQuery query);

    Collection<GenericApiEntity> search(ExecutionContext executionContext, ApiQuery query, boolean excludeDefinitionV4);

    Page<GenericApiEntity> search(
        final ExecutionContext executionContext,
        final String userId,
        final boolean isAdmin,
        final QueryBuilder<ApiEntity> queryBuilder,
        final Pageable pageable,
        final boolean mapToFullGenericApiEntity,
        final boolean manageOnly
    );

    Collection<String> searchIds(
        final ExecutionContext executionContext,
        final String query,
        Map<String, Object> filters,
        final Sortable sortable
    );

    Collection<String> searchIds(
        ExecutionContext executionContext,
        String query,
        Map<String, Object> filters,
        Sortable sortable,
        Collection<DefinitionVersion> excludeDefinitionVersions
    );
}
