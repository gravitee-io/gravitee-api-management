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
package io.gravitee.apim.infra.query_service.cluster;

import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.query_service.ClusterQueryService;
import io.gravitee.apim.infra.adapter.ClusterAdapter;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.ClusterRepository;
import io.gravitee.repository.management.api.search.ClusterCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.Sortable;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class ClusterQueryServiceImpl implements ClusterQueryService {

    private final ClusterRepository clusterRepository;
    private final ClusterAdapter clusterAdapter;

    public ClusterQueryServiceImpl(@Lazy ClusterRepository clusterRepository, ClusterAdapter clusterAdapter) {
        this.clusterRepository = clusterRepository;
        this.clusterAdapter = clusterAdapter;
    }

    @Override
    public Page<Cluster> search(ClusterCriteria criteria, Pageable pageable, Optional<Sortable> sortableOpt) {
        var result = clusterRepository.search(
            criteria,
            new PageableBuilder().pageNumber(pageable.getPageNumber() - 1).pageSize(pageable.getPageSize()).build(),
            sortableOpt.map(sortable -> new SortableBuilder().field(sortable.getField()).setAsc(sortable.isAscOrder()).build())
        );

        return result.map(clusterAdapter::fromRepository);
    }
}
