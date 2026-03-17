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

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.query_service.ApiPortalSearchQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.Sortable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ApiPortalSearchQueryServiceInMemory extends AbstractQueryServiceInMemory<Api> implements ApiPortalSearchQueryService {

    @Override
    public Page<Api> search(Query q) {
        String environmentId = q.environmentId();
        String query = q.query().orElse(null);
        Set<String> allowedApiIds = q.allowedApiIds();
        Pageable pageable = q.pageable().orElse(null);
        Sortable sortable = q.sortable().orElse(null);

        int pageNumber = pageable != null ? pageable.getPageNumber() : 1;
        int pageSize = pageable != null ? pageable.getPageSize() : 10;

        if (allowedApiIds != null && allowedApiIds.isEmpty()) {
            return new Page<>(List.of(), pageNumber, 0, 0);
        }

        Comparator<Api> comparator = buildComparator(sortable);

        List<Api> matched = storage
            .stream()
            .filter(api -> Objects.equals(api.getEnvironmentId(), environmentId))
            .filter(api -> allowedApiIds == null || allowedApiIds.contains(api.getId()))
            .filter(api -> {
                if (query == null || query.isBlank()) return true;
                String queryLower = query.trim().toLowerCase();
                return api.getName() != null && api.getName().toLowerCase().contains(queryLower);
            })
            .sorted(comparator)
            .toList();

        int total = matched.size();

        if (pageSize <= 0 || total == 0) {
            return new Page<>(List.of(), pageNumber, 0, total);
        }
        int start = (pageNumber - 1) * pageSize;
        if (start >= total) {
            return new Page<>(List.of(), pageNumber, 0, total);
        }
        int end = Math.min(start + pageSize, total);
        List<Api> pageContent = new ArrayList<>(matched.subList(start, end));
        return new Page<>(pageContent, pageNumber, pageContent.size(), total);
    }

    private Comparator<Api> buildComparator(Sortable sortable) {
        if (sortable == null || sortable.getField() == null) {
            return Comparator.comparing(Api::getName, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        Comparator<Api> comparator = sortable.getField().equalsIgnoreCase("name")
            ? Comparator.comparing(Api::getName, Comparator.nullsLast(Comparator.naturalOrder()))
            : Comparator.comparing(Api::getId, Comparator.nullsLast(Comparator.naturalOrder()));

        return sortable.isAscOrder() ? comparator : comparator.reversed();
    }
}
