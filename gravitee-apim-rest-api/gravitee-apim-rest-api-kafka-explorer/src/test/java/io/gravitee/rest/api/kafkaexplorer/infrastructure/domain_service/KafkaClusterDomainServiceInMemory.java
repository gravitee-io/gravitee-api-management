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
package io.gravitee.rest.api.kafkaexplorer.infrastructure.domain_service;

import io.gravitee.apim.core.cluster.model.KafkaClusterConfiguration;
import io.gravitee.rest.api.kafkaexplorer.domain.domain_service.KafkaClusterDomainService;
import io.gravitee.rest.api.kafkaexplorer.domain.exception.KafkaExplorerException;
import io.gravitee.rest.api.kafkaexplorer.domain.model.BrokerInfo;
import io.gravitee.rest.api.kafkaexplorer.domain.model.KafkaClusterInfo;
import io.gravitee.rest.api.kafkaexplorer.domain.model.KafkaTopic;
import io.gravitee.rest.api.kafkaexplorer.domain.model.TopicDetail;
import io.gravitee.rest.api.kafkaexplorer.domain.model.TopicsPage;
import java.util.Comparator;
import java.util.List;

public class KafkaClusterDomainServiceInMemory implements KafkaClusterDomainService {

    private KafkaClusterInfo result;
    private List<KafkaTopic> topics;
    private TopicDetail topicDetail;
    private BrokerInfo brokerInfo;
    private KafkaExplorerException exception;

    public void givenClusterInfo(KafkaClusterInfo info) {
        this.result = info;
        this.exception = null;
    }

    public void givenTopics(List<KafkaTopic> topics) {
        this.topics = topics;
        this.exception = null;
    }

    public void givenTopicDetail(TopicDetail detail) {
        this.topicDetail = detail;
        this.exception = null;
    }

    public void givenBrokerInfo(BrokerInfo info) {
        this.brokerInfo = info;
        this.exception = null;
    }

    public void givenException(KafkaExplorerException exception) {
        this.exception = exception;
        this.result = null;
        this.topics = null;
        this.topicDetail = null;
        this.brokerInfo = null;
    }

    @Override
    public KafkaClusterInfo describeCluster(KafkaClusterConfiguration config) {
        if (exception != null) {
            throw exception;
        }
        return result;
    }

    @Override
    public TopicsPage listTopics(KafkaClusterConfiguration config, String nameFilter, int page, int perPage) {
        if (exception != null) {
            throw exception;
        }

        List<KafkaTopic> filtered = topics
            .stream()
            .filter(t -> nameFilter == null || nameFilter.isBlank() || t.name().toLowerCase().contains(nameFilter.toLowerCase()))
            .sorted(Comparator.comparing(KafkaTopic::name))
            .toList();

        int totalCount = filtered.size();
        int fromIndex = Math.min(page * perPage, totalCount);
        int toIndex = Math.min(fromIndex + perPage, totalCount);

        return new TopicsPage(filtered.subList(fromIndex, toIndex), totalCount, page, perPage);
    }

    @Override
    public TopicDetail describeTopic(KafkaClusterConfiguration config, String topicName) {
        if (exception != null) {
            throw exception;
        }
        return topicDetail;
    }

    @Override
    public BrokerInfo describeBroker(KafkaClusterConfiguration config, int brokerId) {
        if (exception != null) {
            throw exception;
        }
        return brokerInfo;
    }
}
