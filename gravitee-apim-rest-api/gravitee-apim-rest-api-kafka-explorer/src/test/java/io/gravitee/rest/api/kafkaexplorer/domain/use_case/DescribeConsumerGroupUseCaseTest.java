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
import io.gravitee.rest.api.kafkaexplorer.domain.model.ConsumerGroupDetail;
import io.gravitee.rest.api.kafkaexplorer.domain.model.ConsumerGroupMember;
import io.gravitee.rest.api.kafkaexplorer.domain.model.ConsumerGroupOffset;
import io.gravitee.rest.api.kafkaexplorer.domain.model.KafkaNode;
import io.gravitee.rest.api.kafkaexplorer.domain.model.MemberAssignment;
import io.gravitee.rest.api.kafkaexplorer.infrastructure.domain_service.KafkaClusterDomainServiceInMemory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DescribeConsumerGroupUseCaseTest {

    private static final String CLUSTER_ID = "cluster-1";
    private static final String ENVIRONMENT_ID = "env-1";

    private final ClusterCrudServiceInMemory clusterCrudService = new ClusterCrudServiceInMemory();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KafkaClusterDomainServiceInMemory clusterDomainService = new KafkaClusterDomainServiceInMemory();

    private DescribeConsumerGroupUseCase useCase;

    @BeforeEach
    void setUp() {
        clusterCrudService.reset();
        useCase = new DescribeConsumerGroupUseCase(clusterCrudService, clusterDomainService, objectMapper);
    }

    @Test
    void should_return_consumer_group_detail() {
        var clusterConfig = Map.of("bootstrapServers", "localhost:9092", "security", Map.of("protocol", "PLAINTEXT"));
        clusterCrudService.create(Cluster.builder().id(CLUSTER_ID).environmentId(ENVIRONMENT_ID).configuration(clusterConfig).build());

        var expectedDetail = new ConsumerGroupDetail(
            "my-group",
            "STABLE",
            new KafkaNode(0, "broker-host", 9092),
            "range",
            List.of(
                new ConsumerGroupMember("member-1", "client-1", "/127.0.0.1", List.of(new MemberAssignment("my-topic", List.of(0, 1))))
            ),
            List.of(new ConsumerGroupOffset("my-topic", 0, 50, 100, 50), new ConsumerGroupOffset("my-topic", 1, 80, 100, 20))
        );
        clusterDomainService.givenConsumerGroupDetail(expectedDetail);

        var result = useCase.execute(new DescribeConsumerGroupUseCase.Input(CLUSTER_ID, ENVIRONMENT_ID, "my-group"));

        assertThat(result.consumerGroupDetail()).isEqualTo(expectedDetail);
    }

    @Test
    void should_propagate_kafka_explorer_exception() {
        var clusterConfig = Map.of("bootstrapServers", "localhost:9092", "security", Map.of("protocol", "PLAINTEXT"));
        clusterCrudService.create(Cluster.builder().id(CLUSTER_ID).environmentId(ENVIRONMENT_ID).configuration(clusterConfig).build());

        clusterDomainService.givenException(new KafkaExplorerException("Connection failed", TechnicalCode.CONNECTION_FAILED));

        assertThatThrownBy(() -> useCase.execute(new DescribeConsumerGroupUseCase.Input(CLUSTER_ID, ENVIRONMENT_ID, "my-group")))
            .isInstanceOf(KafkaExplorerException.class)
            .satisfies(e -> assertThat(((KafkaExplorerException) e).getTechnicalCode()).isEqualTo(TechnicalCode.CONNECTION_FAILED));
    }
}
