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
package io.gravitee.rest.api.service.impl;

import io.gravitee.rest.api.model.kafka.KafkaConnectionConfig;
import io.gravitee.rest.api.model.kafka.KafkaConnectionResult;
import io.gravitee.rest.api.model.kafka.KafkaTopicEntity;
import io.gravitee.rest.api.model.kafka.NewKafkaTopicEntity;
import io.gravitee.rest.api.service.KafkaManagementService;
import io.gravitee.rest.api.service.exceptions.KafkaConnectionException;
import io.gravitee.rest.api.service.exceptions.KafkaTopicOperationException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementation of KafkaManagementService using Kafka AdminClient.
 * Handles connection testing, topic management, and Kafka cluster operations.
 *
 * @author Gravitee Team
 */
@Component
public class KafkaManagementServiceImpl implements KafkaManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaManagementServiceImpl.class);
    private static final int KAFKA_TIMEOUT_SECONDS = 10;

    @Override
    public KafkaConnectionResult testConnection(KafkaConnectionConfig config) {
        try {
            Properties props = createKafkaProperties(config.getBootstrapServers(), config.getSecurityProtocol());
            configureSecurity(props, config);

            try (AdminClient adminClient = AdminClient.create(props)) {
                // Test connection by listing topics
                adminClient.listTopics().names().get(5, TimeUnit.SECONDS);
                return KafkaConnectionResult.success(config.getBootstrapServers());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaConnectionException(config.getBootstrapServers(), e);
        } catch (ExecutionException | TimeoutException e) {
            // This is expected behavior when connection fails - return failure result instead of throwing
            return KafkaConnectionResult.failure("Failed to connect: " + getRootCauseMessage(e));
        }
    }

    @Override
    public List<KafkaTopicEntity> listTopics(String bootstrapServers, String securityProtocol) {
        try {
            Properties props = createKafkaProperties(bootstrapServers, securityProtocol);

            try (AdminClient adminClient = AdminClient.create(props)) {
                Set<String> topicNames = adminClient.listTopics().names().get(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                DescribeTopicsResult describeResult = adminClient.describeTopics(topicNames);
                Map<String, TopicDescription> topicDescriptions = describeResult.all().get(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                return topicDescriptions
                    .entrySet()
                    .stream()
                    .map(entry -> {
                        TopicDescription desc = entry.getValue();
                        KafkaTopicEntity topic = new KafkaTopicEntity();
                        topic.setId("topic-" + desc.name().hashCode());
                        topic.setName(desc.name());
                        topic.setPartitions(desc.partitions().size());
                        topic.setReplicas(desc.partitions().isEmpty() ? 0 : desc.partitions().get(0).replicas().size());
                        topic.setMessages("0"); // Would need consumer API to fetch message count
                        return topic;
                    })
                    .collect(Collectors.toList());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaTopicOperationException("list", "all topics", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new KafkaTopicOperationException("list", "all topics", e);
        }
    }

    @Override
    public KafkaTopicEntity createTopic(String bootstrapServers, String securityProtocol, NewKafkaTopicEntity newTopic) {
        try {
            Properties props = createKafkaProperties(bootstrapServers, securityProtocol);

            try (AdminClient adminClient = AdminClient.create(props)) {
                NewTopic kafkaTopic = new NewTopic(newTopic.getName(), newTopic.getPartitions(), newTopic.getReplicas().shortValue());

                Map<String, String> configs = new HashMap<>();
                if (newTopic.getRetentionMs() != null) {
                    configs.put("retention.ms", String.valueOf(newTopic.getRetentionMs()));
                }
                if (newTopic.getCleanupPolicy() != null) {
                    configs.put("cleanup.policy", newTopic.getCleanupPolicy());
                }
                kafkaTopic.configs(configs);

                CreateTopicsResult result = adminClient.createTopics(Collections.singleton(kafkaTopic));
                result.all().get(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                return new KafkaTopicEntity(newTopic.getName(), newTopic.getPartitions(), newTopic.getReplicas());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaTopicOperationException("create", newTopic.getName(), e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TopicExistsException) {
                throw new KafkaTopicOperationException(
                    "create",
                    newTopic.getName(),
                    new IllegalArgumentException("Topic already exists", e)
                );
            }
            throw new KafkaTopicOperationException("create", newTopic.getName(), e);
        } catch (TimeoutException e) {
            throw new KafkaTopicOperationException("create", newTopic.getName(), e);
        }
    }

    @Override
    public void deleteTopic(String bootstrapServers, String securityProtocol, String topicName) {
        try {
            Properties props = createKafkaProperties(bootstrapServers, securityProtocol);

            try (AdminClient adminClient = AdminClient.create(props)) {
                DeleteTopicsResult result = adminClient.deleteTopics(Collections.singleton(topicName));
                result.all().get(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaTopicOperationException("delete", topicName, e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof UnknownTopicOrPartitionException) {
                throw new KafkaTopicOperationException("delete", topicName, new IllegalArgumentException("Topic does not exist", e));
            }
            throw new KafkaTopicOperationException("delete", topicName, e);
        } catch (TimeoutException e) {
            throw new KafkaTopicOperationException("delete", topicName, e);
        }
    }

    private Properties createKafkaProperties(String bootstrapServers, String securityProtocol) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, String.valueOf(KAFKA_TIMEOUT_SECONDS * 1000));
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, String.valueOf(KAFKA_TIMEOUT_SECONDS * 1000));

        if (securityProtocol != null && !"PLAINTEXT".equals(securityProtocol)) {
            props.put("security.protocol", securityProtocol);
        }

        return props;
    }

    private void configureSecurity(Properties props, KafkaConnectionConfig config) {
        if (config.getSaslMechanism() != null) {
            props.put("sasl.mechanism", config.getSaslMechanism());

            if (config.getSaslUsername() != null && config.getSaslPassword() != null) {
                String jaasConfig = String.format(
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                    config.getSaslUsername(),
                    config.getSaslPassword()
                );
                props.put("sasl.jaas.config", jaasConfig);
            }
        }
    }

    private String getRootCauseMessage(Exception e) {
        Throwable cause = e.getCause();
        while (cause != null && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause != null ? cause.getMessage() : e.getMessage();
    }
}
