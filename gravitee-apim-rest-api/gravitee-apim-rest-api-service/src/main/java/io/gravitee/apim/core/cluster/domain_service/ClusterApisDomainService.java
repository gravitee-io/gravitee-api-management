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
package io.gravitee.apim.core.cluster.domain_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.cluster.domain_service.ClusterBoundApisQueryService.ClusterUse;
import io.gravitee.apim.core.cluster.domain_service.ClusterInUseDomainService.VirtualClusterOn;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.query_service.ClusterApiSearchQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import jakarta.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * The APIs using a cluster, whatever their state: directly, or for a registered cluster through the virtual clusters
 * deployed at the gateway with a backend on it. The API search index finds and pages them; only the APIs of the page
 * are loaded.
 */
@DomainService
@RequiredArgsConstructor
public class ClusterApisDomainService {

    private final ClusterApiSearchQueryService clusterApiSearchQueryService;
    private final ApiQueryService apiQueryService;
    private final ClusterInUseDomainService clusterInUseDomainService;
    private final ObjectMapper objectMapper;

    /**
     * An API using a cluster.
     *
     * @param connectionCrossIds the connections of the cluster it uses, directly or through {@code virtualClusters}
     * @param virtualClusters the deployed virtual clusters it reaches the cluster through
     */
    public record ClusterApi(Api api, Set<String> connectionCrossIds, List<Cluster> virtualClusters) {}

    /**
     * A page of the APIs using {@code cluster}, sorted by name then id.
     *
     * @param name narrows the APIs to those whose name contains it, ignoring case; none when blank
     */
    public Page<ClusterApi> apisUsing(Cluster cluster, String environmentId, @Nullable String name, Pageable pageable) {
        if (cluster.getCrossId() == null) {
            return new Page<>(List.of(), pageable.getPageNumber(), 0, 0);
        }
        Map<String, VirtualClusterOn> virtualClusters = clusterInUseDomainService.virtualClustersOn(environmentId, cluster);
        Set<String> clusterCrossIds = new LinkedHashSet<>();
        clusterCrossIds.add(cluster.getCrossId());
        clusterCrossIds.addAll(virtualClusters.keySet());

        Page<String> ids = clusterApiSearchQueryService.search(environmentId, clusterCrossIds, name, pageable);
        if (ids.getContent().isEmpty()) {
            return new Page<>(List.of(), ids.getPageNumber(), 0, ids.getTotalElements());
        }
        Map<String, Api> apis = apiQueryService
            .search(
                ApiSearchCriteria.builder().environmentId(environmentId).ids(ids.getContent()).build(),
                null,
                ApiFieldFilter.builder().pictureExcluded(true).build()
            )
            .collect(Collectors.toMap(Api::getId, Function.identity(), (first, second) -> first));
        // The index is updated asynchronously: for a few seconds it may name an API deleted since, or one no longer using
        // the cluster. Such an API is left out of the page, which keeps the total of the index.
        List<ClusterApi> page = ids
            .getContent()
            .stream()
            .map(apis::get)
            .filter(Objects::nonNull)
            .flatMap(api -> clusterApi(api, cluster, virtualClusters).stream())
            .toList();
        return new Page<>(page, ids.getPageNumber(), page.size(), ids.getTotalElements());
    }

    /** How {@code api} uses {@code cluster}; empty when it no longer does. */
    private Optional<ClusterApi> clusterApi(Api api, Cluster cluster, Map<String, VirtualClusterOn> virtualClusters) {
        Set<String> connections = new LinkedHashSet<>();
        Map<String, Cluster> through = new LinkedHashMap<>();
        boolean uses = false;
        for (ClusterUse use : ClusterBoundApisQueryService.clusterUses(api, objectMapper)) {
            if (cluster.getCrossId().equals(use.clusterCrossId())) {
                uses = true;
                if (use.connectionCrossId() != null) {
                    connections.add(use.connectionCrossId());
                }
            } else if (virtualClusters.get(use.clusterCrossId()) instanceof VirtualClusterOn on) {
                uses = true;
                through.put(on.virtualCluster().getCrossId(), on.virtualCluster());
                on.backendConnectionCrossIds().stream().filter(Objects::nonNull).forEach(connections::add);
            }
        }
        return uses ? Optional.of(new ClusterApi(api, connections, List.copyOf(through.values()))) : Optional.empty();
    }
}
