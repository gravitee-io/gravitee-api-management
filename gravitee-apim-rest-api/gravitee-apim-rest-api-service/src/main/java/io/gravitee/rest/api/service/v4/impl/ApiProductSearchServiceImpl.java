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
package io.gravitee.rest.api.service.v4.impl;

import static java.util.stream.Collectors.toList;

import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.membership.domain_service.ApiProductPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.exception.ApiProductPrimaryOwnerNotFoundException;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.search.model.IndexableApiProduct;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.PaginationInvalidException;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
import io.gravitee.rest.api.service.v4.ApiProductSearchService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@CustomLog
public class ApiProductSearchServiceImpl implements ApiProductSearchService {

    private final SearchEngineService searchEngineService;
    private final ApiProductQueryService apiProductQueryService;
    private final ApiProductPrimaryOwnerDomainService apiProductPrimaryOwnerDomainService;

    @Override
    public Page<ApiProduct> search(ExecutionContext executionContext, QueryBuilder<IndexableApiProduct> queryBuilder, Pageable pageable) {
        var query = queryBuilder.build();

        SearchResult searchResult = searchEngineService.search(executionContext, query);

        if (!searchResult.hasResults()) {
            return new Page<>(List.of(), 0, 0, 0);
        }

        List<String> sortedIds = new ArrayList<>(searchResult.getDocuments());
        List<String> pageIds = getPageSubset(sortedIds, pageable);

        if (pageIds.isEmpty()) {
            return new Page<>(List.of(), pageable.getPageNumber(), 0, sortedIds.size());
        }

        Set<ApiProduct> apiProducts = apiProductQueryService.findByEnvironmentIdAndIdIn(
            executionContext.getEnvironmentId(),
            Set.copyOf(pageIds)
        );

        Map<String, ApiProduct> byId = new LinkedHashMap<>();
        apiProducts.forEach(apiProduct -> byId.put(apiProduct.getId(), apiProduct));

        List<ApiProduct> orderedPage = pageIds
            .stream()
            .map(byId::get)
            .filter(Objects::nonNull)
            .map(apiProduct -> addPrimaryOwner(apiProduct, executionContext.getOrganizationId()))
            .collect(toList());

        return new Page<>(orderedPage, pageable.getPageNumber(), orderedPage.size(), sortedIds.size());
    }

    private ApiProduct addPrimaryOwner(ApiProduct apiProduct, String organizationId) {
        PrimaryOwnerEntity primaryOwner = null;
        try {
            primaryOwner = apiProductPrimaryOwnerDomainService.getApiProductPrimaryOwner(organizationId, apiProduct.getId());
        } catch (ApiProductPrimaryOwnerNotFoundException e) {
            log.warn("Failed to retrieve primary owner for API Product [{}]: {}", apiProduct.getId(), e.getMessage());
        }
        return apiProduct.toBuilder().primaryOwner(primaryOwner).build();
    }

    private List<String> getPageSubset(List<String> ids, Pageable pageable) {
        int total = ids.size();
        int pageSize = pageable.getPageSize();

        if (pageSize <= 0 || total <= 0) {
            return List.of();
        }

        int pageNumber = pageable.getPageNumber();
        int startIndex = (pageNumber - 1) * pageSize;

        if (startIndex >= total || pageNumber < 1) {
            throw new PaginationInvalidException();
        }

        return ids.stream().skip(startIndex).limit(pageSize).collect(toList());
    }
}
