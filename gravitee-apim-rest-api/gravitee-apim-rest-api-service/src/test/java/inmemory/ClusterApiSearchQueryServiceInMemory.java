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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.cluster.domain_service.ClusterBoundApisQueryService;
import io.gravitee.apim.core.cluster.query_service.ClusterApiSearchQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Searches the APIs it holds as the search index would: by the cluster cross ids of their endpoints. */
public class ClusterApiSearchQueryServiceInMemory extends AbstractQueryServiceInMemory<Api> implements ClusterApiSearchQueryService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public Page<String> search(String environmentId, Collection<String> clusterCrossIds, String name, Pageable pageable) {
        List<String> matching = storage
            .stream()
            .filter(api -> Objects.equals(api.getEnvironmentId(), environmentId))
            .filter(api ->
                ClusterBoundApisQueryService.clusterCrossIds(ClusterBoundApisQueryService.clusterUses(api, OBJECT_MAPPER))
                    .stream()
                    .anyMatch(clusterCrossIds::contains)
            )
            .filter(
                api ->
                    name == null ||
                    name.isBlank() ||
                    (api.getName() != null && api.getName().toLowerCase(Locale.ROOT).contains(name.trim().toLowerCase(Locale.ROOT)))
            )
            .sorted(
                Comparator.comparing((Api api) -> String.valueOf(api.getName()), String.CASE_INSENSITIVE_ORDER).thenComparing(Api::getId)
            )
            .map(Api::getId)
            .toList();
        int from = (pageable.getPageNumber() - 1) * pageable.getPageSize();
        List<String> page = from >= matching.size()
            ? List.of()
            : matching.subList(from, Math.min(from + pageable.getPageSize(), matching.size()));
        return new Page<>(page, pageable.getPageNumber(), page.size(), matching.size());
    }
}
