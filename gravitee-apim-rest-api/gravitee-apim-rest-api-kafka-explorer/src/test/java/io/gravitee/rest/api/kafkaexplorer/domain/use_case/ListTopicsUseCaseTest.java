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
package io.gravitee.rest.api.kafkaexplorer.domain.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import inmemory.ClusterCrudServiceInMemory;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.rest.api.kafkaexplorer.domain.exception.KafkaExplorerException;
import io.gravitee.rest.api.kafkaexplorer.domain.exception.TechnicalCode;
import io.gravitee.rest.api.kafkaexplorer.domain.model.KafkaTopic;
import io.gravitee.rest.api.kafkaexplorer.infrastructure.domain_service.KafkaClusterDomainServiceInMemory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ListTopicsUseCaseTest {

    private static final String CLUSTER_ID = "cluster-1";
    private static final String ENVIRONMENT_ID = "env-1";

    private final ClusterCrudServiceInMemory clusterCrudService = new ClusterCrudServiceInMemory();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KafkaClusterDomainServiceInMemory clusterDomainService = new KafkaClusterDomainServiceInMemory();

    private ListTopicsUseCase useCase;

    @BeforeEach
    void setUp() {
        clusterCrudService.reset();
        useCase = new ListTopicsUseCase(clusterCrudService, clusterDomainService, objectMapper);
    }

    @Test
    void should_return_topics_page() {
        var clusterConfig = Map.of("bootstrapServers", "localhost:9092", "security", Map.of("protocol", "PLAINTEXT"));
        clusterCrudService.create(Cluster.builder().id(CLUSTER_ID).environmentId(ENVIRONMENT_ID).configuration(clusterConfig).build());

        var allTopics = List.of(
            new KafkaTopic("__consumer_offsets", 50, 1, 0, true, 1024L, 100L),
            new KafkaTopic("my-topic", 3, 2, 0, false, 2048L, 200L)
        );
        clusterDomainService.givenTopics(allTopics);

        var result = useCase.execute(new ListTopicsUseCase.Input(CLUSTER_ID, ENVIRONMENT_ID, null, 0, 25));

        assertThat(result.topicsPage().data()).hasSize(2);
        assertThat(result.topicsPage().totalCount()).isEqualTo(2);
        assertThat(result.topicsPage().page()).isEqualTo(0);
        assertThat(result.topicsPage().perPage()).isEqualTo(25);
    }

    @Test
    void should_filter_topics_by_name() {
        var clusterConfig = Map.of("bootstrapServers", "localhost:9092", "security", Map.of("protocol", "PLAINTEXT"));
        clusterCrudService.create(Cluster.builder().id(CLUSTER_ID).environmentId(ENVIRONMENT_ID).configuration(clusterConfig).build());

        var allTopics = List.of(
            new KafkaTopic("__consumer_offsets", 50, 1, 0, true, 1024L, 100L),
            new KafkaTopic("my-topic", 3, 2, 0, false, 2048L, 200L),
            new KafkaTopic("orders", 6, 3, 0, false, 4096L, 500L)
        );
        clusterDomainService.givenTopics(allTopics);

        var result = useCase.execute(new ListTopicsUseCase.Input(CLUSTER_ID, ENVIRONMENT_ID, "my", 0, 25));

        assertThat(result.topicsPage().data()).hasSize(1);
        assertThat(result.topicsPage().data().get(0).name()).isEqualTo("my-topic");
        assertThat(result.topicsPage().totalCount()).isEqualTo(1);
    }

    @Test
    void should_propagate_kafka_explorer_exception() {
        var clusterConfig = Map.of("bootstrapServers", "localhost:9092", "security", Map.of("protocol", "PLAINTEXT"));
        clusterCrudService.create(Cluster.builder().id(CLUSTER_ID).environmentId(ENVIRONMENT_ID).configuration(clusterConfig).build());

        clusterDomainService.givenException(new KafkaExplorerException("Connection failed", TechnicalCode.CONNECTION_FAILED));

        assertThatThrownBy(() -> useCase.execute(new ListTopicsUseCase.Input(CLUSTER_ID, ENVIRONMENT_ID, null, 0, 25)))
            .isInstanceOf(KafkaExplorerException.class)
            .satisfies(e -> assertThat(((KafkaExplorerException) e).getTechnicalCode()).isEqualTo(TechnicalCode.CONNECTION_FAILED));
    }
}
