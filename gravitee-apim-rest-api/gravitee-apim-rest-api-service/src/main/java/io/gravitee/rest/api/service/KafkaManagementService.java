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
package io.gravitee.rest.api.service;

import io.gravitee.rest.api.model.kafka.KafkaConnectionConfig;
import io.gravitee.rest.api.model.kafka.KafkaConnectionResult;
import io.gravitee.rest.api.model.kafka.KafkaTopicEntity;
import io.gravitee.rest.api.model.kafka.NewKafkaTopicEntity;
import java.util.List;

/**
 * Service for managing Kafka clusters and topics through Kafka AdminClient.
 *
 * @author Gravitee Team
 */
public interface KafkaManagementService {
    /**
     * Test connection to a Kafka cluster.
     *
     * @param config the Kafka connection configuration
     * @return connection test result with success status and message
     */
    KafkaConnectionResult testConnection(KafkaConnectionConfig config);

    /**
     * List all topics from a Kafka cluster.
     *
     * @param bootstrapServers Kafka bootstrap servers
     * @param securityProtocol Kafka security protocol (PLAINTEXT, SASL_PLAINTEXT, etc.)
     * @return list of Kafka topics
     */
    List<KafkaTopicEntity> listTopics(String bootstrapServers, String securityProtocol);

    /**
     * Create a new topic in a Kafka cluster.
     *
     * @param bootstrapServers Kafka bootstrap servers
     * @param securityProtocol Kafka security protocol
     * @param newTopic         topic configuration
     * @return the created topic entity
     */
    KafkaTopicEntity createTopic(String bootstrapServers, String securityProtocol, NewKafkaTopicEntity newTopic);

    /**
     * Delete a topic from a Kafka cluster.
     *
     * @param bootstrapServers Kafka bootstrap servers
     * @param securityProtocol Kafka security protocol
     * @param topicName        name of the topic to delete
     */
    void deleteTopic(String bootstrapServers, String securityProtocol, String topicName);
}
