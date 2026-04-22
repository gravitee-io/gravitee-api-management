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
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.DeployedCluster;
import io.gravitee.apim.core.cluster.model.KafkaClusterConfiguration;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.event.query_service.EventLatestQueryService;
import io.gravitee.definition.model.cluster.ClusterType;
import io.gravitee.rest.api.model.EventType;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

@UseCase
@AllArgsConstructor
public class GetDeployedClustersUseCase {

    private final EventLatestQueryService eventLatestQueryService;
    private final ObjectMapper objectMapper;

    public record Input(String environmentId, ClusterType type) {}

    public record Output(List<DeployedCluster> deployedClusters) {}

    public Output execute(Input input) {
        List<Event> events = eventLatestQueryService.findAllByTypeAndEnvironments(
            Set.of(EventType.DEPLOY_CLUSTER),
            Set.of(input.environmentId()),
            Event.EventProperties.CLUSTER_ID
        );

        var stream = events.stream().map(this::toDeployedCluster);
        if (input.type() != null) {
            stream = stream.filter(dc -> dc.getType() == input.type());
        }
        List<DeployedCluster> deployedClusters = stream.collect(Collectors.toList());

        return new Output(deployedClusters);
    }

    private DeployedCluster toDeployedCluster(Event event) {
        try {
            Cluster cluster = objectMapper.readValue(event.getPayload(), Cluster.class);

            List<DeployedCluster.DeployedClusterConnection> connections = Collections.emptyList();
            if (cluster.getType() == ClusterType.KAFKA_CLUSTER && cluster.getConfiguration() != null) {
                KafkaClusterConfiguration config = cluster.getKafkaClusterConfiguration(objectMapper);
                if (config.connections() != null) {
                    connections = config
                        .connections()
                        .stream()
                        .map(c -> DeployedCluster.DeployedClusterConnection.builder().crossId(c.crossId()).name(c.name()).build())
                        .collect(Collectors.toList());
                }
            }

            return DeployedCluster.builder()
                .crossId(cluster.getCrossId())
                .name(cluster.getName())
                .description(cluster.getDescription())
                .type(cluster.getType())
                .deployedAt(cluster.getDeployedAt())
                .version(cluster.getVersion())
                .connections(connections)
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize deployed cluster from event payload", e);
        }
    }
}
