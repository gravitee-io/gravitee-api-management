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
import io.gravitee.rest.api.kafkaexplorer.domain.model.BrokerDetail;
import io.gravitee.rest.api.kafkaexplorer.domain.model.KafkaClusterInfo;
import io.gravitee.rest.api.kafkaexplorer.domain.model.KafkaNode;
import io.gravitee.rest.api.kafkaexplorer.infrastructure.domain_service.KafkaClusterDomainServiceInMemory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DescribeKafkaClusterUseCaseTest {

    private static final String CLUSTER_ID = "cluster-1";
    private static final String ENVIRONMENT_ID = "env-1";

    private final ClusterCrudServiceInMemory clusterCrudService = new ClusterCrudServiceInMemory();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KafkaClusterDomainServiceInMemory clusterDomainService = new KafkaClusterDomainServiceInMemory();

    private DescribeKafkaClusterUseCase useCase;

    @BeforeEach
    void setUp() {
        clusterCrudService.reset();
        useCase = new DescribeKafkaClusterUseCase(clusterCrudService, clusterDomainService, objectMapper);
    }

    @Test
    void should_return_cluster_info() {
        var clusterConfig = Map.of("bootstrapServers", "localhost:9092", "security", Map.of("protocol", "PLAINTEXT"));
        clusterCrudService.create(Cluster.builder().id(CLUSTER_ID).environmentId(ENVIRONMENT_ID).configuration(clusterConfig).build());

        var expectedInfo = new KafkaClusterInfo(
            "cluster-1",
            new KafkaNode(0, "broker-host", 9092),
            List.of(new BrokerDetail(0, "broker-host", 9092, null, 5, 10, 1024L)),
            1,
            5
        );
        clusterDomainService.givenClusterInfo(expectedInfo);

        var result = useCase.execute(new DescribeKafkaClusterUseCase.Input(CLUSTER_ID, ENVIRONMENT_ID));

        assertThat(result.clusterInfo()).isEqualTo(expectedInfo);
    }

    @Test
    void should_propagate_kafka_explorer_exception() {
        var clusterConfig = Map.of("bootstrapServers", "localhost:9092", "security", Map.of("protocol", "PLAINTEXT"));
        clusterCrudService.create(Cluster.builder().id(CLUSTER_ID).environmentId(ENVIRONMENT_ID).configuration(clusterConfig).build());

        clusterDomainService.givenException(new KafkaExplorerException("Connection failed", TechnicalCode.CONNECTION_FAILED));

        assertThatThrownBy(() -> useCase.execute(new DescribeKafkaClusterUseCase.Input(CLUSTER_ID, ENVIRONMENT_ID)))
            .isInstanceOf(KafkaExplorerException.class)
            .satisfies(e -> assertThat(((KafkaExplorerException) e).getTechnicalCode()).isEqualTo(TechnicalCode.CONNECTION_FAILED));
    }
}
