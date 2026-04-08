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
package io.gravitee.rest.api.kafkaexplorer.domain.domain_service;

import io.gravitee.apim.core.cluster.model.KafkaClusterConnectionConfiguration;
import io.gravitee.rest.api.kafkaexplorer.domain.model.BrokerInfo;
import io.gravitee.rest.api.kafkaexplorer.domain.model.BrowseMessagesResult;
import io.gravitee.rest.api.kafkaexplorer.domain.model.ConsumerGroupDetail;
import io.gravitee.rest.api.kafkaexplorer.domain.model.ConsumerGroupsPage;
import io.gravitee.rest.api.kafkaexplorer.domain.model.KafkaClusterInfo;
import io.gravitee.rest.api.kafkaexplorer.domain.model.TopicDetail;
import io.gravitee.rest.api.kafkaexplorer.domain.model.TopicsPage;

public interface KafkaClusterDomainService {
    KafkaClusterInfo describeCluster(KafkaClusterConnectionConfiguration config);
    TopicsPage listTopics(
        KafkaClusterConnectionConfiguration config,
        String nameFilter,
        int page,
        int perPage,
        String sortBy,
        String sortOrder
    );
    TopicDetail describeTopic(KafkaClusterConnectionConfiguration config, String topicName);
    BrokerInfo describeBroker(KafkaClusterConnectionConfiguration config, int brokerId);
    ConsumerGroupsPage listConsumerGroups(
        KafkaClusterConnectionConfiguration config,
        String nameFilter,
        String topicFilter,
        int page,
        int perPage,
        String sortBy,
        String sortOrder
    );
    ConsumerGroupDetail describeConsumerGroup(KafkaClusterConnectionConfiguration config, String groupId);
    BrowseMessagesResult browseMessages(
        KafkaClusterConnectionConfiguration config,
        String topicName,
        Integer partition,
        String offsetMode,
        Long offsetValue,
        String keyFilter,
        String valueFilter,
        int limit
    );
    void tailMessages(
        KafkaClusterConnectionConfiguration config,
        String topicName,
        Integer partition,
        String keyFilter,
        String valueFilter,
        int maxMessages,
        int durationSeconds,
        MessageConsumer consumer
    );
}
