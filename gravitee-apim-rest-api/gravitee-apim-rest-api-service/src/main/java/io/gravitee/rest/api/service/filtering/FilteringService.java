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
package io.gravitee.rest.api.service.filtering;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collection;
import java.util.Set;

public interface FilteringService {
    Collection<String> getApisOrderByNumberOfSubscriptions(final Collection<String> ids, final boolean excluded);

    Collection<String> getApplicationsOrderByNumberOfSubscriptions(final Collection<String> ids, Order order);

    Collection<String> filterApis(
        ExecutionContext executionContext,
        final Set<String> apis,
        final FilterType filterType,
        final FilterType excludedFilterType
    );

    Collection<String> filterApis(
        ExecutionContext executionContext,
        final String userId,
        final FilterType filterType,
        final FilterType excludedFilterType,
        final ApiQuery apiQuery
    );

    default Collection<String> searchApis() throws TechnicalException {
        return searchApis(null, null, null);
    }

    Collection<String> searchApis(ExecutionContext executionContext, final String userId, final String query) throws TechnicalException;

    Collection<String> searchApisWithCategory(
        ExecutionContext executionContext,
        final String userId,
        final String query,
        final String category
    ) throws TechnicalException;

    Set<CategoryEntity> listCategories(
        ExecutionContext executionContext,
        final String userId,
        final FilterType filterType,
        final FilterType excludedFilterType
    );

    enum FilterType {
        ALL,
        FEATURED,
        MINE,
        STARRED,
        TRENDINGS,
    }
}
