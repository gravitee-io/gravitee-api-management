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
package io.gravitee.apim.infra.query_service.subscription;

import io.gravitee.apim.core.search.model.IndexableApiProduct;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.query_service.SubscriptionTargetSearchQueryService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionTargetSearchQueryServiceImpl implements SubscriptionTargetSearchQueryService {

    private final ApiSearchService apiSearchService;
    private final SearchEngineService searchEngineService;

    @Override
    public MatchingTargetIds search(ExecutionContext executionContext, String query, Set<SubscriptionReferenceType> referenceTypes) {
        Set<String> apiIds = referenceTypes.contains(SubscriptionReferenceType.API)
            ? new LinkedHashSet<>(apiSearchService.searchIds(executionContext, query, Map.of(), null))
            : Set.of();
        Set<String> apiProductIds = referenceTypes.contains(SubscriptionReferenceType.API_PRODUCT)
            ? searchApiProductIds(executionContext, query)
            : Set.of();

        return new MatchingTargetIds(apiIds, apiProductIds);
    }

    private Set<String> searchApiProductIds(ExecutionContext executionContext, String query) {
        QueryBuilder<IndexableApiProduct> queryBuilder = QueryBuilder.create(IndexableApiProduct.class);
        queryBuilder.setQuery(query);
        return new LinkedHashSet<>(searchEngineService.search(executionContext, queryBuilder.build()).getDocuments());
    }
}
