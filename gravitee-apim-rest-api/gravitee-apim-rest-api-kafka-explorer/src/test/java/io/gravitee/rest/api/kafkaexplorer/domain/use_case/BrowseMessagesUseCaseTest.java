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
import io.gravitee.rest.api.kafkaexplorer.domain.model.BrowseMessagesResult;
import io.gravitee.rest.api.kafkaexplorer.domain.model.KafkaHeader;
import io.gravitee.rest.api.kafkaexplorer.domain.model.KafkaMessage;
import io.gravitee.rest.api.kafkaexplorer.infrastructure.domain_service.KafkaClusterDomainServiceInMemory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class BrowseMessagesUseCaseTest {

    private static final String CLUSTER_ID = "cluster-1";
    private static final String ENVIRONMENT_ID = "env-1";

    private final ClusterCrudServiceInMemory clusterCrudService = new ClusterCrudServiceInMemory();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KafkaClusterDomainServiceInMemory clusterDomainService = new KafkaClusterDomainServiceInMemory();

    private BrowseMessagesUseCase useCase;

    @BeforeEach
    void setUp() {
        clusterCrudService.reset();
        useCase = new BrowseMessagesUseCase(clusterCrudService, clusterDomainService, objectMapper);
    }

    @Test
    void should_return_browse_messages_result() {
        var clusterConfig = Map.of("bootstrapServers", "localhost:9092", "security", Map.of("protocol", "PLAINTEXT"));
        clusterCrudService.create(Cluster.builder().id(CLUSTER_ID).environmentId(ENVIRONMENT_ID).configuration(clusterConfig).build());

        var expectedResult = new BrowseMessagesResult(
            List.of(
                new KafkaMessage(0, 42L, 1700000000000L, "key-1", "{\"hello\":\"world\"}", List.of(new KafkaHeader("h1", "v1"))),
                new KafkaMessage(1, 10L, 1700000001000L, "key-2", "plain text", List.of())
            ),
            2
        );
        clusterDomainService.givenBrowseMessagesResult(expectedResult);

        var result = useCase.execute(
            new BrowseMessagesUseCase.Input(CLUSTER_ID, ENVIRONMENT_ID, "my-topic", null, "NEWEST", null, null, 50)
        );

        assertThat(result.browseMessagesResult()).isEqualTo(expectedResult);
        assertThat(result.browseMessagesResult().messages()).hasSize(2);
        assertThat(result.browseMessagesResult().totalFetched()).isEqualTo(2);
    }

    @Test
    void should_propagate_kafka_explorer_exception() {
        var clusterConfig = Map.of("bootstrapServers", "localhost:9092", "security", Map.of("protocol", "PLAINTEXT"));
        clusterCrudService.create(Cluster.builder().id(CLUSTER_ID).environmentId(ENVIRONMENT_ID).configuration(clusterConfig).build());

        clusterDomainService.givenException(new KafkaExplorerException("Topic not found", TechnicalCode.TOPIC_NOT_FOUND));

        assertThatThrownBy(() ->
            useCase.execute(new BrowseMessagesUseCase.Input(CLUSTER_ID, ENVIRONMENT_ID, "missing-topic", null, "NEWEST", null, null, 50))
        )
            .isInstanceOf(KafkaExplorerException.class)
            .satisfies(e -> assertThat(((KafkaExplorerException) e).getTechnicalCode()).isEqualTo(TechnicalCode.TOPIC_NOT_FOUND));
    }
}
