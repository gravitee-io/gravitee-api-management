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
package io.gravitee.apim.core.cluster.use_case;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.domain_service.ApiAuthorizationDomainService;
import io.gravitee.apim.core.cluster.crud_service.ClusterCrudService;
import io.gravitee.apim.core.cluster.domain_service.ClusterApisDomainService;
import io.gravitee.apim.core.cluster.domain_service.ClusterApisDomainService.ClusterApi;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.KafkaClusterConfiguration;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.cluster.ClusterType;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.service.common.ExecutionContext;
import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;

/**
 * A page of the APIs using a cluster, whatever their state, for the cluster's "APIs" tab. The caller sees every API, even
 * those it cannot read, but only their name, version, type and state: whoever can read the cluster has to know what uses
 * it. Each API says whether the caller can read it.
 */
@AllArgsConstructor
@UseCase
public class GetClusterApisUseCase {

    private final ClusterCrudService clusterCrudService;
    private final ClusterApisDomainService clusterApisDomainService;
    private final ApiAuthorizationDomainService apiAuthorizationDomainService;
    private final ObjectMapper objectMapper;

    /** @param name narrows the APIs to those whose name contains it, ignoring case; none when blank */
    public record Input(
        String clusterId,
        @Nullable String name,
        Pageable pageable,
        ExecutionContext executionContext,
        String userId,
        boolean isAdmin
    ) {}

    /** @param readable whether the caller can read the API: an organization admin, or a member of the API */
    public record Item(ClusterApi clusterApi, boolean readable) {}

    /**
     * @param apis sorted by name then id
     * @param connectionNames the name of each connection of the cluster, by cross id
     */
    public record Output(Page<Item> apis, Map<String, String> connectionNames) {}

    public Output execute(Input input) {
        var cluster = clusterCrudService.findByIdAndEnvironmentId(input.clusterId(), input.executionContext().getEnvironmentId());
        Page<ClusterApi> apis = clusterApisDomainService.apisUsing(
            cluster,
            input.executionContext().getEnvironmentId(),
            input.name(),
            input.pageable()
        );
        if (apis.getContent().isEmpty()) {
            return new Output(new Page<>(List.of(), apis.getPageNumber(), 0, apis.getTotalElements()), Map.of());
        }
        Set<String> readable = input.isAdmin()
            ? null
            : apiAuthorizationDomainService.findMemberApiIds(input.executionContext(), input.userId());
        List<Item> items = apis
            .getContent()
            .stream()
            .map(api -> new Item(api, readable == null || readable.contains(api.api().getId())))
            .toList();
        return new Output(new Page<>(items, apis.getPageNumber(), items.size(), apis.getTotalElements()), connectionNames(cluster));
    }

    private Map<String, String> connectionNames(Cluster cluster) {
        if (cluster.getType() != ClusterType.KAFKA_CLUSTER) {
            return Map.of();
        }
        KafkaClusterConfiguration configuration = cluster.getKafkaClusterConfiguration(objectMapper);
        if (configuration == null || configuration.connections() == null) {
            return Map.of();
        }
        Map<String, String> names = new HashMap<>();
        configuration
            .connections()
            .stream()
            .filter(connection -> connection != null && connection.crossId() != null && connection.name() != null)
            .forEach(connection -> names.put(connection.crossId(), connection.name()));
        return names;
    }
}
