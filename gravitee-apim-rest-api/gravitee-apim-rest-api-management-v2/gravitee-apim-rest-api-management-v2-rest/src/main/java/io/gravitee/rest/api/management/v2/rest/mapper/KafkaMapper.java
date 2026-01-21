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
package io.gravitee.rest.api.management.v2.rest.mapper;

import io.gravitee.rest.api.management.v2.rest.model.KafkaConnectionRequest;
import io.gravitee.rest.api.management.v2.rest.model.KafkaTopicCreateRequest;
import io.gravitee.rest.api.management.v2.rest.model.KafkaTopicResponse;
import io.gravitee.rest.api.model.kafka.KafkaConnectionConfig;
import io.gravitee.rest.api.model.kafka.KafkaTopicEntity;
import io.gravitee.rest.api.model.kafka.NewKafkaTopicEntity;

/**
 * Mapper for converting between Kafka REST models and domain models.
 *
 * @author Gravitee Team
 */
public class KafkaMapper {

    private KafkaMapper() {
        // Utility class
    }

    public static KafkaConnectionConfig toConnectionConfig(KafkaConnectionRequest request) {
        if (request == null) {
            return null;
        }
        KafkaConnectionConfig config = new KafkaConnectionConfig();
        config.setBootstrapServers(request.getBootstrapServers());
        config.setSecurityProtocol(request.getSecurityProtocol());
        config.setSaslMechanism(request.getSaslMechanism());
        config.setSaslUsername(request.getSaslUsername());
        config.setSaslPassword(request.getSaslPassword());
        return config;
    }

    public static NewKafkaTopicEntity toNewTopicEntity(KafkaTopicCreateRequest request) {
        if (request == null) {
            return null;
        }
        NewKafkaTopicEntity entity = new NewKafkaTopicEntity();
        entity.setName(request.getName());
        entity.setPartitions(request.getPartitions());
        entity.setReplicas(request.getReplicas());
        entity.setRetentionMs(request.getRetentionMs());
        entity.setCleanupPolicy(request.getCleanupPolicy());
        return entity;
    }

    public static KafkaTopicResponse toTopicResponse(KafkaTopicEntity entity) {
        if (entity == null) {
            return null;
        }
        KafkaTopicResponse response = new KafkaTopicResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setPartitions(entity.getPartitions());
        response.setReplicas(entity.getReplicas());
        response.setMessages(entity.getMessages());
        return response;
    }
}
