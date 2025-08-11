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

import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.query_service.ClusterQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.Sortable;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

public class ClusterQueryServiceInMemory extends AbstractQueryServiceInMemory<Cluster> implements ClusterQueryService {

    @Override
    public Page<Cluster> searchByEnvironmentId(String envId, Pageable pageable, Optional<Sortable> sortable) {
        var pageNumber = pageable.getPageNumber();
        var pageSize = pageable.getPageSize();

        var stream = storage.stream().filter(cluster -> cluster.getEnvironmentId().equals(envId)).sorted(sortClusters(sortable));

        var matches = stream.collect(Collectors.toList());

        var page = matches.size() <= pageSize
            ? matches
            : matches.subList((pageNumber - 1) * pageSize, Math.min(pageNumber * pageSize, matches.size()));

        return new Page<>(page, pageNumber, pageSize, matches.size());
    }

    private Comparator<Cluster> sortClusters(Optional<Sortable> sortableOpt) {
        if (sortableOpt.isEmpty()) {
            return Comparator.comparing(Cluster::getName);
        }
        Sortable sortable = sortableOpt.get();
        Comparator<Cluster> comparator = null;
        switch (sortable.getField()) {
            case "id":
                comparator = Comparator.comparing(Cluster::getId);
                break;
            default:
                comparator = Comparator.comparing(Cluster::getName);
        }
        if (sortable.isAscOrder()) {
            return comparator;
        }
        return comparator.reversed();
    }
}
