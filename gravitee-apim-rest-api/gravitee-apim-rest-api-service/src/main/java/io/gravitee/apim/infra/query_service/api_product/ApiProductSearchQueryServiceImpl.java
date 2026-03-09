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
package io.gravitee.apim.infra.query_service.api_product;

import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductSearchQueryService;
import io.gravitee.apim.core.search.model.IndexableApiProduct;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
import io.gravitee.rest.api.service.v4.ApiProductSearchService;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ApiProductSearchQueryServiceImpl implements ApiProductSearchQueryService {

    private static final String FILTER_TYPE_API_PRODUCT = "api_product";

    private final ApiProductSearchService apiProductSearchService;

    public ApiProductSearchQueryServiceImpl(ApiProductSearchService apiProductSearchService) {
        this.apiProductSearchService = apiProductSearchService;
    }

    @Override
    public Page<ApiProduct> search(
        String environmentId,
        String organizationId,
        String query,
        Set<String> ids,
        Pageable pageable,
        Sortable sortable
    ) {
        var executionContext = new ExecutionContext(organizationId, environmentId);
        QueryBuilder<IndexableApiProduct> queryBuilder = QueryBuilder.create(IndexableApiProduct.class);

        if (query != null && !query.isBlank()) {
            queryBuilder.setQuery(query.trim());
        }
        if (CollectionUtils.isNotEmpty(ids)) {
            queryBuilder.addFilter(FILTER_TYPE_API_PRODUCT, ids);
        }
        if (sortable != null) {
            queryBuilder.setSort(sortable);
        }

        return apiProductSearchService.search(executionContext, queryBuilder, pageable);
    }
}
