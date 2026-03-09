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
package io.gravitee.apim.core.api_product.use_case;

import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_TYPE_VALUE;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.domain_service.ApiAuthorizationDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.model.Sortable;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetApiProductApisUseCase {

    private final ApiProductQueryService apiProductQueryService;
    private final ApiQueryService apiQueryService;
    private final ApiAuthorizationDomainService apiAuthorizationDomainService;
    private final ApiSearchService apiSearchService;

    public Output execute(Input input) {
        Optional<ApiProduct> apiProduct = apiProductQueryService.findById(input.apiProductId());

        if (apiProduct.isEmpty()) {
            return new Output(Optional.empty(), null);
        }

        List<String> apiIds = Optional.ofNullable(apiProduct.get().getApiIds())
            .map(ids -> ids.stream().toList())
            .orElse(List.of());

        if (apiIds.isEmpty()) {
            return new Output(apiProduct, new Page<>(List.of(), 0, 0, 0));
        }

        List<String> searchIds = apiIds;
        if (!input.isAdmin()) {
            Set<String> manageableIds = apiAuthorizationDomainService.findIdsByUser(
                input.executionContext(),
                input.userId(),
                io.gravitee.apim.core.api.model.ApiQueryCriteria.builder().ids(apiIds).build(),
                input.sortable(),
                true // manageOnly
            );
            if (manageableIds == null || manageableIds.isEmpty()) {
                return new Output(apiProduct, new Page<>(List.of(), 0, 0, 0));
            }
            searchIds = new ArrayList<>(manageableIds);
        }

        // When user provides a search query, use the same Lucene-based search as the API list
        // (name/context path/description/etc.) so that "km" matches "akm-basic", "akm2", etc.
        // without changing repository implementations.
        if (input.query() != null && !input.query().isBlank()) {
            return executeWithQuery(input, apiProduct, searchIds);
        }

        // No query: search by ids only via repository (no name filter)
        ApiSearchCriteria criteria = ApiSearchCriteria.builder()
            .ids(searchIds)
            .environmentId(input.executionContext().getEnvironmentId())
            .build();
        Sortable coreSortable = toCoreSortable(input.sortable());
        Page<Api> apisPage = apiQueryService.search(
            criteria,
            coreSortable,
            input.pageable(),
            ApiFieldFilter.builder().pictureExcluded(true).build()
        );
        return new Output(apiProduct, apisPage);
    }

    private Output executeWithQuery(Input input, Optional<ApiProduct> apiProduct, List<String> searchIds) {
        // Step 1: Lucene search restricted to product's API ids (same behaviour as API list search)
        Collection<String> matchedIds = apiSearchService.searchIds(
            input.executionContext(),
            input.query().strip(),
            Collections.singletonMap(FIELD_TYPE_VALUE, searchIds),
            input.sortable(),
            Collections.emptySet()
        );
        if (matchedIds == null || matchedIds.isEmpty()) {
            return new Output(apiProduct, new Page<>(List.of(), 0, 0, 0));
        }
        List<String> orderedIds = new ArrayList<>(matchedIds);

        // Step 2: Paginate the matched ids (Lucene order)
        int pageNumber = input.pageable().getPageNumber();
        int pageSize = input.pageable().getPageSize();
        int totalCount = orderedIds.size();
        int fromIndex = (pageNumber - 1) * pageSize;
        if (fromIndex >= totalCount) {
            return new Output(apiProduct, new Page<>(List.of(), pageNumber, 0, totalCount));
        }
        List<String> pageSubset = orderedIds.subList(fromIndex, Math.min(fromIndex + pageSize, totalCount));

        // Step 3: Load full APIs by id from repository (no name filter; repo unchanged)
        ApiSearchCriteria criteria = ApiSearchCriteria.builder()
            .ids(pageSubset)
            .environmentId(input.executionContext().getEnvironmentId())
            .build();
        Sortable coreSortable = toCoreSortable(input.sortable());
        Page<Api> apisPage = apiQueryService.search(
            criteria,
            coreSortable,
            new PageableImpl(1, pageSubset.size()),
            ApiFieldFilter.builder().pictureExcluded(true).build()
        );
        // Copy to mutable list; getContent() may return an unmodifiable list
        List<Api> content = new ArrayList<>(apisPage.getContent());
        // Preserve Lucene order
        content.sort(Comparator.comparingInt(api -> pageSubset.indexOf(api.getId())));
        Page<Api> resultPage = new Page<>(content, pageNumber, content.size(), totalCount);
        return new Output(apiProduct, resultPage);
    }

    private static Sortable toCoreSortable(io.gravitee.rest.api.model.common.Sortable restSortable) {
        if (restSortable == null || restSortable.getField() == null) {
            return null;
        }
        return Sortable.builder()
            .field(restSortable.getField())
            .order(restSortable.isAscOrder() ? Sortable.Order.ASC : Sortable.Order.DESC)
            .build();
    }

    public record Input(
        ExecutionContext executionContext,
        String apiProductId,
        String query,
        Pageable pageable,
        io.gravitee.rest.api.model.common.Sortable sortable,
        String userId,
        boolean isAdmin
    ) {}

    public record Output(Optional<ApiProduct> apiProduct, Page<Api> apisPage) {}
}
