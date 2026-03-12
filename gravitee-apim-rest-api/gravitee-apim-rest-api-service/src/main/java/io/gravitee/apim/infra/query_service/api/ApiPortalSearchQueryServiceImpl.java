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
package io.gravitee.apim.infra.query_service.api;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.query_service.ApiPortalSearchQueryService;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ApiPortalSearchQueryServiceImpl implements ApiPortalSearchQueryService {

    private final ApiSearchService apiSearchService;
    private final ApiQueryService apiQueryService;

    public ApiPortalSearchQueryServiceImpl(ApiSearchService apiSearchService, ApiQueryService apiQueryService) {
        this.apiSearchService = apiSearchService;
        this.apiQueryService = apiQueryService;
    }

    @Override
    public Page<Api> search(
        String environmentId,
        String organizationId,
        String query,
        Set<String> allowedApiIds,
        Pageable pageable,
        Sortable sortable
    ) {
        int pageNumber = pageable != null ? pageable.getPageNumber() : 1;

        if (allowedApiIds == null || allowedApiIds.isEmpty()) {
            return new Page<>(List.of(), pageNumber, 0, 0);
        }

        if (query == null || query.isBlank()) {
            // No text query: delegate sorting and pagination to the repository
            return apiQueryService.search(
                ApiSearchCriteria.builder().ids(List.copyOf(allowedApiIds)).environmentId(environmentId).build(),
                toCoreSortable(sortable),
                pageable,
                null
            );
        }

        var executionContext = new ExecutionContext(organizationId, environmentId);
        Collection<String> luceneIds = apiSearchService.searchIds(executionContext, query.trim(), Map.of(), sortable);
        List<String> intersected = luceneIds.stream().filter(allowedApiIds::contains).toList();

        int total = intersected.size();
        int pageSize = pageable != null ? pageable.getPageSize() : 10;

        if (total == 0 || pageSize <= 0) {
            return new Page<>(List.of(), pageNumber, 0, total);
        }

        int start = (pageNumber - 1) * pageSize;
        if (start >= total) {
            return new Page<>(List.of(), pageNumber, 0, total);
        }
        List<String> pageSubset = intersected.subList(start, Math.min(start + pageSize, total));

        // Fetch by IDs then reorder to preserve Lucene relevance order
        Map<String, Api> apiById = apiQueryService
            .search(ApiSearchCriteria.builder().ids(pageSubset).environmentId(environmentId).build(), null, null, null)
            .getContent()
            .stream()
            .collect(Collectors.toMap(Api::getId, Function.identity()));

        List<Api> pageContent = pageSubset.stream().map(apiById::get).filter(Objects::nonNull).toList();

        return new Page<>(pageContent, pageNumber, pageContent.size(), total);
    }

    private io.gravitee.apim.core.api.model.Sortable toCoreSortable(Sortable sortable) {
        if (sortable == null) {
            return null;
        }
        return io.gravitee.apim.core.api.model.Sortable.builder()
            .field(sortable.getField())
            .order(
                sortable.isAscOrder()
                    ? io.gravitee.apim.core.api.model.Sortable.Order.ASC
                    : io.gravitee.apim.core.api.model.Sortable.Order.DESC
            )
            .build();
    }
}
