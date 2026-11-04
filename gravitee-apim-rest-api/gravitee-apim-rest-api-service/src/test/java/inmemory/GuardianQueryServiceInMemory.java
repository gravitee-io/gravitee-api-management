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

import io.gravitee.apim.core.guardian.model.Guardian;
import io.gravitee.apim.core.guardian.query_service.GuardianQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.Sortable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * @author GraviteeSource Team
 */
public class GuardianQueryServiceInMemory implements GuardianQueryService, InMemoryAlternative<Guardian> {

    final ArrayList<Guardian> storage = new ArrayList<>();

    @Override
    public Page<Guardian> searchByEnvironmentId(String environmentId, String q, Pageable pageable, Sortable sortable) {
        var matching = storage
            .stream()
            .filter(guardian -> environmentId.equals(guardian.getEnvironmentId()))
            .filter(guardian -> q == null || guardian.getName().toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT)))
            .sorted(byName(sortable))
            .toList();

        var from = (pageable.getPageNumber() - 1) * pageable.getPageSize();
        if (from >= matching.size()) {
            return new Page<>(List.of(), pageable.getPageNumber(), 0, matching.size());
        }
        var content = matching.subList(from, Math.min(from + pageable.getPageSize(), matching.size()));

        return new Page<>(content, pageable.getPageNumber(), content.size(), matching.size());
    }

    private static Comparator<Guardian> byName(Sortable sortable) {
        var comparator = Comparator.comparing(Guardian::getName, Comparator.nullsLast(Comparator.naturalOrder()));
        return sortable != null && !sortable.isAscOrder() ? comparator.reversed() : comparator;
    }

    @Override
    public void initWith(List<Guardian> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Guardian> storage() {
        return List.copyOf(storage);
    }
}
