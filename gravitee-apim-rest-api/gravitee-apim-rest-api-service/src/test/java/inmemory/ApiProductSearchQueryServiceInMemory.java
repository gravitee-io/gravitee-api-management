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
package inmemory;

import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductSearchQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.Sortable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class ApiProductSearchQueryServiceInMemory extends AbstractQueryServiceInMemory<ApiProduct> implements ApiProductSearchQueryService {

    private String lastEnvironmentId;
    private String lastOrganizationId;

    public String getLastEnvironmentId() {
        return lastEnvironmentId;
    }

    public String getLastOrganizationId() {
        return lastOrganizationId;
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
        this.lastEnvironmentId = environmentId;
        this.lastOrganizationId = organizationId;

        int pageNumber = pageable != null ? pageable.getPageNumber() : 1;
        int pageSize = pageable != null ? pageable.getPageSize() : 10;

        Stream<ApiProduct> stream = storage
            .stream()
            .filter(apiProduct -> Objects.equals(apiProduct.getEnvironmentId(), environmentId))
            .filter(apiProduct -> ids == null || ids.isEmpty() || (ids.contains(apiProduct.getId())))
            .filter(apiProduct -> {
                if (query == null || query.isBlank()) return true;
                String queryLower = query.trim().toLowerCase();
                return (
                    (apiProduct.getName() != null && apiProduct.getName().toLowerCase().contains(queryLower)) ||
                    (apiProduct.getDescription() != null && apiProduct.getDescription().toLowerCase().contains(queryLower))
                );
            });

        if (sortable != null && sortable.getField() != null) {
            Comparator<ApiProduct> comparator = comparatorForField(sortable.getField());
            if (comparator != null) {
                stream = stream.sorted(sortable.isAscOrder() ? comparator : comparator.reversed());
            }
        }

        List<ApiProduct> matched = stream.toList();
        int total = matched.size();

        if (pageSize <= 0 || total == 0) {
            return new Page<>(List.of(), pageNumber, 0, total);
        }
        int start = (pageNumber - 1) * pageSize;
        if (start >= total) {
            return new Page<>(List.of(), pageNumber, 0, total);
        }
        int end = Math.min(start + pageSize, total);
        List<ApiProduct> pageContent = new ArrayList<>(matched.subList(start, end));
        return new Page<>(pageContent, pageNumber, pageContent.size(), total);
    }

    private static Comparator<ApiProduct> comparatorForField(String fieldName) {
        if (fieldName == null) return null;
        return switch (fieldName) {
            case "name" -> Comparator.comparing(
                apiProduct -> apiProduct.getName() != null ? apiProduct.getName() : "",
                String.CASE_INSENSITIVE_ORDER
            );
            case "createdAt" -> Comparator.comparing(
                apiProduct -> apiProduct.getCreatedAt() != null ? apiProduct.getCreatedAt() : ZonedDateTime.now().minusYears(100),
                Comparator.naturalOrder()
            );
            case "updatedAt" -> Comparator.comparing(
                apiProduct -> apiProduct.getUpdatedAt() != null ? apiProduct.getUpdatedAt() : ZonedDateTime.now().minusYears(100),
                Comparator.naturalOrder()
            );
            default -> null;
        };
    }
}
