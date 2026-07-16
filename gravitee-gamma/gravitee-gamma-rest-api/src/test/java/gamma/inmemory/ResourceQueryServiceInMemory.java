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
package gamma.inmemory;

import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.core.domain.resource.model.Resource;
import io.gravitee.gamma.core.domain.resource.query_service.ResourceQueryService;
import io.gravitee.rest.api.model.common.Pageable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ResourceQueryServiceInMemory implements ResourceQueryService {

    private final Map<String, Resource> storage;

    public ResourceQueryServiceInMemory(ResourceCrudServiceInMemory crud) {
        this.storage = crud.storage;
    }

    @Override
    public Page<Resource> search(Resource.ReferenceType referenceType, String referenceId, Pageable pageable, String query) {
        List<Resource> filtered = storage
            .values()
            .stream()
            .filter(r -> r.referenceType() == referenceType)
            .filter(r -> Objects.equals(r.referenceId(), referenceId))
            .filter(r -> matchesQuery(r, query))
            .sorted(Comparator.comparing(Resource::createdAt))
            .toList();

        // Pageable is 1-based.
        int pageNumber = Math.max(1, pageable.getPageNumber());
        int pageSize = pageable.getPageSize();
        int from = Math.min((pageNumber - 1) * pageSize, filtered.size());
        int to = Math.min(from + pageSize, filtered.size());

        return new Page<>(filtered.subList(from, to), pageNumber, to - from, filtered.size());
    }

    private static boolean matchesQuery(Resource resource, String query) {
        if (query == null || query.isBlank() || resource.definition() == null) {
            return query == null || query.isBlank();
        }
        String lower = query.toLowerCase();
        String name = resource.definition().getName();
        String type = resource.definition().getType();
        return (name != null && name.toLowerCase().contains(lower)) || (type != null && type.toLowerCase().contains(lower));
    }
}
