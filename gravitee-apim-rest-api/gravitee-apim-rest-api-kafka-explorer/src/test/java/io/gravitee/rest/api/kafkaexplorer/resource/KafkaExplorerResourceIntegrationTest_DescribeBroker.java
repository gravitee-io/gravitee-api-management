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
package io.gravitee.rest.api.kafkaexplorer.resource;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeBrokerRequest;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeBrokerResponse;
import io.gravitee.rest.api.kafkaexplorer.rest.model.KafkaExplorerError;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class KafkaExplorerResourceIntegrationTest_DescribeBroker extends AbstractKafkaExplorerResourceIntegrationTest {

    @Test
    void should_return_200_with_broker_detail() {
        givenClusterWithConfig(Map.of("bootstrapServers", plaintextBootstrapServers(), "security", Map.of("protocol", "PLAINTEXT")));

        var request = new DescribeBrokerRequest().clusterId(CLUSTER_ID).brokerId(0);

        var response = resource.describeBroker(request);

        assertThat(response.getStatus()).isEqualTo(200);
        var body = (DescribeBrokerResponse) response.getEntity();
        assertThat(body.getId()).isEqualTo(0);
        assertThat(body.getHost()).isNotBlank();
        assertThat(body.getPort()).isGreaterThan(0);
        assertThat(body.getIsController()).isNotNull();
        assertThat(body.getLeaderPartitions()).isGreaterThanOrEqualTo(0);
        assertThat(body.getReplicaPartitions()).isGreaterThanOrEqualTo(0);
        assertThat(body.getLogDirEntries()).isNotNull();
        assertThat(body.getConfigs()).isNotNull().isNotEmpty();
    }

    @Test
    void should_return_400_when_broker_id_is_null() {
        givenClusterWithConfig(Map.of("bootstrapServers", plaintextBootstrapServers(), "security", Map.of("protocol", "PLAINTEXT")));

        var request = new DescribeBrokerRequest().clusterId(CLUSTER_ID);

        var response = resource.describeBroker(request);

        assertThat(response.getStatus()).isEqualTo(400);
        var error = (KafkaExplorerError) response.getEntity();
        assertThat(error.getTechnicalCode()).isEqualTo("INVALID_PARAMETERS");
    }

    @Test
    void should_return_400_when_broker_id_is_negative() {
        givenClusterWithConfig(Map.of("bootstrapServers", plaintextBootstrapServers(), "security", Map.of("protocol", "PLAINTEXT")));

        var request = new DescribeBrokerRequest().clusterId(CLUSTER_ID).brokerId(-1);

        var response = resource.describeBroker(request);

        assertThat(response.getStatus()).isEqualTo(400);
        var error = (KafkaExplorerError) response.getEntity();
        assertThat(error.getTechnicalCode()).isEqualTo("INVALID_PARAMETERS");
    }

    @Test
    void should_return_502_when_broker_unreachable() {
        givenClusterWithConfig(Map.of("bootstrapServers", "localhost:19092", "security", Map.of("protocol", "PLAINTEXT")));

        var request = new DescribeBrokerRequest().clusterId(CLUSTER_ID).brokerId(0);

        var response = resource.describeBroker(request);

        assertThat(response.getStatus()).isEqualTo(502);
        var error = (KafkaExplorerError) response.getEntity();
        assertThat(error.getTechnicalCode()).isEqualTo("CONNECTION_FAILED");
    }
}
