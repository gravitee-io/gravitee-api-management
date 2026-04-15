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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import inmemory.EventLatestQueryServiceInMemory;
import io.gravitee.apim.core.cluster.model.DeployedCluster;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.rest.api.model.EventType;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetDeployedClustersUseCaseTest {

    private static final String ENV_ID = "env-id";

    private final EventLatestQueryServiceInMemory eventLatestQueryService = new EventLatestQueryServiceInMemory();
    private GetDeployedClustersUseCase useCase;

    @BeforeEach
    void setUp() {
        eventLatestQueryService.reset();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        useCase = new GetDeployedClustersUseCase(eventLatestQueryService, objectMapper);
    }

    @Test
    void should_return_empty_list_when_no_deployed_clusters() {
        var output = useCase.execute(new GetDeployedClustersUseCase.Input(ENV_ID));
        assertThat(output.deployedClusters()).isEmpty();
    }

    @Test
    void should_return_deployed_cluster_with_connections() {
        String payload = """
            {
                "id": "cluster-id",
                "crossId": "my-kafka-cluster",
                "name": "My Kafka Cluster",
                "description": "A test cluster",
                "type": "KAFKA_CLUSTER",
                "deployedAt": 1698000000,
                "version": 2,
                "configuration": {
                    "connections": [
                        { "crossId": "primary", "name": "Primary Connection", "bootstrapServers": "kafka:9092", "security": { "protocol": "PLAINTEXT" } },
                        { "crossId": "secondary", "name": "Secondary Connection", "bootstrapServers": "kafka2:9092", "security": { "protocol": "SSL" } }
                    ]
                }
            }
            """;

        var properties = new EnumMap<Event.EventProperties, String>(Event.EventProperties.class);
        properties.put(Event.EventProperties.CLUSTER_ID, "my-kafka-cluster");

        eventLatestQueryService.initWith(
            List.of(
                Event.builder()
                    .id("event-1")
                    .type(EventType.DEPLOY_CLUSTER)
                    .payload(payload)
                    .environments(Set.of(ENV_ID))
                    .properties(properties)
                    .createdAt(ZonedDateTime.now())
                    .build()
            )
        );

        var output = useCase.execute(new GetDeployedClustersUseCase.Input(ENV_ID));

        assertThat(output.deployedClusters()).hasSize(1);
        DeployedCluster cluster = output.deployedClusters().get(0);
        assertThat(cluster.getCrossId()).isEqualTo("my-kafka-cluster");
        assertThat(cluster.getName()).isEqualTo("My Kafka Cluster");
        assertThat(cluster.getDescription()).isEqualTo("A test cluster");
        assertThat(cluster.getVersion()).isEqualTo(2);
        assertThat(cluster.getConnections()).hasSize(2);
        assertThat(cluster.getConnections().get(0).getCrossId()).isEqualTo("primary");
        assertThat(cluster.getConnections().get(0).getName()).isEqualTo("Primary Connection");
        assertThat(cluster.getConnections().get(1).getCrossId()).isEqualTo("secondary");
    }

    @Test
    void should_not_return_clusters_from_other_environments() {
        String payload = """
            {
                "id": "cluster-id",
                "crossId": "other-cluster",
                "name": "Other Cluster",
                "type": "KAFKA_CLUSTER",
                "configuration": { "connections": [] }
            }
            """;

        var properties = new EnumMap<Event.EventProperties, String>(Event.EventProperties.class);
        properties.put(Event.EventProperties.CLUSTER_ID, "other-cluster");

        eventLatestQueryService.initWith(
            List.of(
                Event.builder()
                    .id("event-1")
                    .type(EventType.DEPLOY_CLUSTER)
                    .payload(payload)
                    .environments(Set.of("other-env"))
                    .properties(properties)
                    .createdAt(ZonedDateTime.now())
                    .build()
            )
        );

        var output = useCase.execute(new GetDeployedClustersUseCase.Input(ENV_ID));
        assertThat(output.deployedClusters()).isEmpty();
    }
}
