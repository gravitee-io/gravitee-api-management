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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.cluster.crud_service.ClusterCrudService;
import io.gravitee.rest.api.kafkaexplorer.domain.UseCase;
import io.gravitee.rest.api.kafkaexplorer.domain.domain_service.KafkaClusterDomainService;
import io.gravitee.rest.api.kafkaexplorer.domain.model.ConsumerGroupDetail;

@UseCase
public class DescribeConsumerGroupUseCase {

    private final ClusterCrudService clusterCrudService;
    private final KafkaClusterDomainService kafkaClusterDomainService;
    private final ObjectMapper objectMapper;

    public DescribeConsumerGroupUseCase(
        ClusterCrudService clusterCrudService,
        KafkaClusterDomainService kafkaClusterDomainService,
        ObjectMapper objectMapper
    ) {
        this.clusterCrudService = clusterCrudService;
        this.kafkaClusterDomainService = kafkaClusterDomainService;
        this.objectMapper = objectMapper;
    }

    public Output execute(Input input) {
        var cluster = clusterCrudService.findByIdAndEnvironmentId(input.clusterId(), input.environmentId());
        var config = cluster.getKafkaClusterConfiguration(objectMapper);
        var consumerGroupDetail = kafkaClusterDomainService.describeConsumerGroup(config, input.groupId());
        return new Output(consumerGroupDetail);
    }

    public record Input(String clusterId, String environmentId, String groupId) {}

    public record Output(ConsumerGroupDetail consumerGroupDetail) {}
}
