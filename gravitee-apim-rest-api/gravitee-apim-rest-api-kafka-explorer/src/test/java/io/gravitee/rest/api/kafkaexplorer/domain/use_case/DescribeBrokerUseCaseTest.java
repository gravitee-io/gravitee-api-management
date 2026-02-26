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
import io.gravitee.rest.api.kafkaexplorer.domain.model.BrokerInfo;
import io.gravitee.rest.api.kafkaexplorer.domain.model.BrokerLogDirEntry;
import io.gravitee.rest.api.kafkaexplorer.domain.model.TopicConfigEntry;
import io.gravitee.rest.api.kafkaexplorer.infrastructure.domain_service.KafkaClusterDomainServiceInMemory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DescribeBrokerUseCaseTest {

    private static final String CLUSTER_ID = "cluster-1";
    private static final String ENVIRONMENT_ID = "env-1";

    private final ClusterCrudServiceInMemory clusterCrudService = new ClusterCrudServiceInMemory();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KafkaClusterDomainServiceInMemory clusterDomainService = new KafkaClusterDomainServiceInMemory();

    private DescribeBrokerUseCase useCase;

    @BeforeEach
    void setUp() {
        clusterCrudService.reset();
        useCase = new DescribeBrokerUseCase(clusterCrudService, clusterDomainService, objectMapper);
    }

    @Test
    void should_return_broker_info() {
        var clusterConfig = Map.of("bootstrapServers", "localhost:9092", "security", Map.of("protocol", "PLAINTEXT"));
        clusterCrudService.create(Cluster.builder().id(CLUSTER_ID).environmentId(ENVIRONMENT_ID).configuration(clusterConfig).build());

        var expectedBrokerInfo = new BrokerInfo(
            0,
            "broker-host",
            9092,
            null,
            true,
            10,
            20,
            1073741824L,
            List.of(new BrokerLogDirEntry("/bitnami/kafka/data", null, 5, 12, 524288L)),
            List.of(new TopicConfigEntry("log.retention.hours", "168", "STATIC_BROKER_CONFIG", true, false))
        );
        clusterDomainService.givenBrokerInfo(expectedBrokerInfo);

        var result = useCase.execute(new DescribeBrokerUseCase.Input(CLUSTER_ID, ENVIRONMENT_ID, 0));

        assertThat(result.brokerInfo()).isEqualTo(expectedBrokerInfo);
    }

    @Test
    void should_propagate_kafka_explorer_exception() {
        var clusterConfig = Map.of("bootstrapServers", "localhost:9092", "security", Map.of("protocol", "PLAINTEXT"));
        clusterCrudService.create(Cluster.builder().id(CLUSTER_ID).environmentId(ENVIRONMENT_ID).configuration(clusterConfig).build());

        clusterDomainService.givenException(new KafkaExplorerException("Connection failed", TechnicalCode.CONNECTION_FAILED));

        assertThatThrownBy(() -> useCase.execute(new DescribeBrokerUseCase.Input(CLUSTER_ID, ENVIRONMENT_ID, 0)))
            .isInstanceOf(KafkaExplorerException.class)
            .satisfies(e -> assertThat(((KafkaExplorerException) e).getTechnicalCode()).isEqualTo(TechnicalCode.CONNECTION_FAILED));
    }
}
