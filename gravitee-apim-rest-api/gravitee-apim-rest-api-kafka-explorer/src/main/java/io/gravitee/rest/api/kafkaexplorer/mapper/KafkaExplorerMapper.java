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
package io.gravitee.rest.api.kafkaexplorer.mapper;

import io.gravitee.rest.api.kafkaexplorer.domain.model.BrokerDetail;
import io.gravitee.rest.api.kafkaexplorer.domain.model.BrokerInfo;
import io.gravitee.rest.api.kafkaexplorer.domain.model.BrokerLogDirEntry;
import io.gravitee.rest.api.kafkaexplorer.domain.model.ConsumerGroup;
import io.gravitee.rest.api.kafkaexplorer.domain.model.ConsumerGroupDetail;
import io.gravitee.rest.api.kafkaexplorer.domain.model.ConsumerGroupsPage;
import io.gravitee.rest.api.kafkaexplorer.domain.model.KafkaClusterInfo;
import io.gravitee.rest.api.kafkaexplorer.domain.model.KafkaNode;
import io.gravitee.rest.api.kafkaexplorer.domain.model.KafkaTopic;
import io.gravitee.rest.api.kafkaexplorer.domain.model.TopicConfigEntry;
import io.gravitee.rest.api.kafkaexplorer.domain.model.TopicDetail;
import io.gravitee.rest.api.kafkaexplorer.domain.model.TopicPartitionDetail;
import io.gravitee.rest.api.kafkaexplorer.domain.model.TopicsPage;
import io.gravitee.rest.api.kafkaexplorer.rest.model.ConsumerGroupSummary;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeBrokerResponse;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeClusterResponse;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeConsumerGroupResponse;
import io.gravitee.rest.api.kafkaexplorer.rest.model.DescribeTopicResponse;
import io.gravitee.rest.api.kafkaexplorer.rest.model.ListConsumerGroupsResponse;
import io.gravitee.rest.api.kafkaexplorer.rest.model.ListTopicsResponse;
import io.gravitee.rest.api.kafkaexplorer.rest.model.Pagination;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface KafkaExplorerMapper {
    KafkaExplorerMapper INSTANCE = Mappers.getMapper(KafkaExplorerMapper.class);

    DescribeClusterResponse map(KafkaClusterInfo clusterInfo);

    io.gravitee.rest.api.kafkaexplorer.rest.model.KafkaNode map(KafkaNode node);

    io.gravitee.rest.api.kafkaexplorer.rest.model.BrokerDetail map(BrokerDetail brokerDetail);

    io.gravitee.rest.api.kafkaexplorer.rest.model.KafkaTopic map(KafkaTopic topic);

    List<io.gravitee.rest.api.kafkaexplorer.rest.model.KafkaTopic> mapTopics(List<KafkaTopic> topics);

    DescribeTopicResponse map(TopicDetail topicDetail);

    io.gravitee.rest.api.kafkaexplorer.rest.model.TopicPartition map(TopicPartitionDetail partition);

    io.gravitee.rest.api.kafkaexplorer.rest.model.TopicConfig map(TopicConfigEntry config);

    DescribeBrokerResponse map(BrokerInfo brokerInfo);

    io.gravitee.rest.api.kafkaexplorer.rest.model.BrokerLogDirEntry map(BrokerLogDirEntry entry);

    default ListTopicsResponse map(TopicsPage topicsPage, int page, int perPage) {
        return new ListTopicsResponse()
            .data(mapTopics(topicsPage.data()))
            .pagination(
                new Pagination()
                    .page(page)
                    .perPage(perPage)
                    .pageCount((int) Math.ceil((double) topicsPage.totalCount() / perPage))
                    .pageItemsCount(topicsPage.data().size())
                    .totalCount(topicsPage.totalCount())
            );
    }

    ConsumerGroupSummary map(ConsumerGroup consumerGroup);

    List<ConsumerGroupSummary> mapConsumerGroups(List<ConsumerGroup> consumerGroups);

    default ListConsumerGroupsResponse map(ConsumerGroupsPage consumerGroupsPage, int page, int perPage) {
        return new ListConsumerGroupsResponse()
            .data(mapConsumerGroups(consumerGroupsPage.data()))
            .pagination(
                new Pagination()
                    .page(page)
                    .perPage(perPage)
                    .pageCount((int) Math.ceil((double) consumerGroupsPage.totalCount() / perPage))
                    .pageItemsCount(consumerGroupsPage.data().size())
                    .totalCount(consumerGroupsPage.totalCount())
            );
    }

    DescribeConsumerGroupResponse map(ConsumerGroupDetail consumerGroupDetail);

    io.gravitee.rest.api.kafkaexplorer.rest.model.ConsumerGroupMember map(
        io.gravitee.rest.api.kafkaexplorer.domain.model.ConsumerGroupMember member
    );

    io.gravitee.rest.api.kafkaexplorer.rest.model.MemberAssignment map(
        io.gravitee.rest.api.kafkaexplorer.domain.model.MemberAssignment assignment
    );

    io.gravitee.rest.api.kafkaexplorer.rest.model.ConsumerGroupOffset map(
        io.gravitee.rest.api.kafkaexplorer.domain.model.ConsumerGroupOffset offset
    );
}
