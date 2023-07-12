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
package io.gravitee.plugin.endpoint.kafka.vertx.admin;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.lang.rx.RxGen;
import io.vertx.lang.rx.TypeArg;
import io.vertx.reactivex.impl.AsyncResultCompletable;
import io.vertx.reactivex.impl.AsyncResultSingle;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Generated;
import lombok.Lombok;

/**
 * Vert.x Kafka Admin client implementation
 *
 * <p/>
 * NOTE: This class has been automatically generated from the {@link io.vertx.kafka.admin.KafkaAdminClient original} non RX-ified interface using Vert.x codegen.
 */

@RxGen(io.vertx.kafka.admin.KafkaAdminClient.class)
@Generated
public class KafkaAdminClient {

    public static final TypeArg<KafkaAdminClient> __TYPE_ARG = new TypeArg<>(
        obj -> new KafkaAdminClient((io.vertx.kafka.admin.KafkaAdminClient) obj),
        KafkaAdminClient::getDelegate
    );
    private final io.vertx.kafka.admin.KafkaAdminClient delegate;

    public KafkaAdminClient(io.vertx.kafka.admin.KafkaAdminClient delegate) {
        this.delegate = delegate;
    }

    public KafkaAdminClient(Object delegate) {
        this.delegate = (io.vertx.kafka.admin.KafkaAdminClient) delegate;
    }

    /**
     * Create a new KafkaAdminClient instance
     * @param vertx Vert.x instance to use
     * @param config Kafka admin client configuration
     * @return an instance of the KafkaAdminClient
     */
    public static KafkaAdminClient create(io.vertx.reactivex.core.Vertx vertx, Map<String, String> config) {
        KafkaAdminClient ret = KafkaAdminClient.newInstance(io.vertx.kafka.admin.KafkaAdminClient.create(vertx.getDelegate(), config));
        return ret;
    }

    public static KafkaAdminClient newInstance(io.vertx.kafka.admin.KafkaAdminClient arg) {
        return arg != null ? new KafkaAdminClient(arg) : null;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaAdminClient that = (KafkaAdminClient) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    public io.vertx.kafka.admin.KafkaAdminClient getDelegate() {
        return delegate;
    }

    /**
     * List the topics available in the cluster with the default options.
     * @param completionHandler handler called on operation completed with the topics set
     */
    public void listTopics(Handler<AsyncResult<Set<String>>> completionHandler) {
        delegate.listTopics(completionHandler);
    }

    /**
     * List the topics available in the cluster with the default options.
     */
    public void listTopics() {
        listTopics(ar -> {});
    }

    /**
     * List the topics available in the cluster with the default options.
     * @return
     */
    public io.reactivex.Single<Set<String>> rxListTopics() {
        return AsyncResultSingle.toSingle($handler -> {
            listTopics($handler);
        });
    }

    /**
     * Describe some topics in the cluster, with the default options.
     * @param topicNames the names of the topics to describe
     * @param completionHandler handler called on operation completed with the topics descriptions
     */
    public void describeTopics(
        List<String> topicNames,
        Handler<AsyncResult<Map<String, io.vertx.kafka.admin.TopicDescription>>> completionHandler
    ) {
        delegate.describeTopics(topicNames, completionHandler);
    }

    /**
     * Describe some topics in the cluster, with the default options.
     * @param topicNames the names of the topics to describe
     */
    public void describeTopics(List<String> topicNames) {
        describeTopics(topicNames, ar -> {});
    }

    /**
     * Describe some topics in the cluster, with the default options.
     * @param topicNames the names of the topics to describe
     * @return
     */
    public io.reactivex.Single<Map<String, io.vertx.kafka.admin.TopicDescription>> rxDescribeTopics(List<String> topicNames) {
        return AsyncResultSingle.toSingle($handler -> {
            describeTopics(topicNames, $handler);
        });
    }

    /**
     * Creates a batch of new Kafka topics
     * @param topics topics to create
     * @param completionHandler handler called on operation completed
     */
    public void createTopics(List<io.vertx.kafka.admin.NewTopic> topics, Handler<AsyncResult<Void>> completionHandler) {
        delegate.createTopics(topics, completionHandler);
    }

    /**
     * Creates a batch of new Kafka topics
     * @param topics topics to create
     */
    public void createTopics(List<io.vertx.kafka.admin.NewTopic> topics) {
        createTopics(topics, ar -> {});
    }

    /**
     * Creates a batch of new Kafka topics
     * @param topics topics to create
     * @return
     */
    public io.reactivex.Completable rxCreateTopics(List<io.vertx.kafka.admin.NewTopic> topics) {
        return AsyncResultCompletable.toCompletable($handler -> {
            createTopics(topics, $handler);
        });
    }

    /**
     * Deletes a batch of Kafka topics
     * @param topicNames the names of the topics to delete
     * @param completionHandler handler called on operation completed
     */
    public void deleteTopics(List<String> topicNames, Handler<AsyncResult<Void>> completionHandler) {
        delegate.deleteTopics(topicNames, completionHandler);
    }

    /**
     * Deletes a batch of Kafka topics
     * @param topicNames the names of the topics to delete
     */
    public void deleteTopics(List<String> topicNames) {
        deleteTopics(topicNames, ar -> {});
    }

    /**
     * Deletes a batch of Kafka topics
     * @param topicNames the names of the topics to delete
     * @return
     */
    public io.reactivex.Completable rxDeleteTopics(List<String> topicNames) {
        return AsyncResultCompletable.toCompletable($handler -> {
            deleteTopics(topicNames, $handler);
        });
    }

    /**
     * Creates a batch of new partitions in the Kafka topic
     * @param partitions partitions to create
     * @param completionHandler handler called on operation completed
     */
    public void createPartitions(Map<String, io.vertx.kafka.admin.NewPartitions> partitions, Handler<AsyncResult<Void>> completionHandler) {
        delegate.createPartitions(partitions, completionHandler);
    }

    /**
     * Creates a batch of new partitions in the Kafka topic
     * @param partitions partitions to create
     */
    public void createPartitions(Map<String, io.vertx.kafka.admin.NewPartitions> partitions) {
        createPartitions(partitions, ar -> {});
    }

    /**
     * Creates a batch of new partitions in the Kafka topic
     * @param partitions partitions to create
     * @return
     */
    public io.reactivex.Completable rxCreatePartitions(Map<String, io.vertx.kafka.admin.NewPartitions> partitions) {
        return AsyncResultCompletable.toCompletable($handler -> {
            createPartitions(partitions, $handler);
        });
    }

    /**
     * Get the the consumer groups available in the cluster with the default options
     * @param completionHandler handler called on operation completed with the consumer groups ids
     */
    public void listConsumerGroups(Handler<AsyncResult<List<io.vertx.kafka.admin.ConsumerGroupListing>>> completionHandler) {
        delegate.listConsumerGroups(completionHandler);
    }

    /**
     * Get the the consumer groups available in the cluster with the default options
     */
    public void listConsumerGroups() {
        listConsumerGroups(ar -> {});
    }

    /**
     * Get the the consumer groups available in the cluster with the default options
     * @return
     */
    public io.reactivex.Single<List<io.vertx.kafka.admin.ConsumerGroupListing>> rxListConsumerGroups() {
        return AsyncResultSingle.toSingle($handler -> {
            listConsumerGroups($handler);
        });
    }

    /**
     * Describe some group ids in the cluster, with the default options
     * @param groupIds the ids of the groups to describe
     * @param completionHandler handler called on operation completed with the consumer groups descriptions
     */
    public void describeConsumerGroups(
        List<String> groupIds,
        Handler<AsyncResult<Map<String, io.vertx.kafka.admin.ConsumerGroupDescription>>> completionHandler
    ) {
        delegate.describeConsumerGroups(groupIds, completionHandler);
    }

    /**
     * Describe some group ids in the cluster, with the default options
     * @param groupIds the ids of the groups to describe
     */
    public void describeConsumerGroups(List<String> groupIds) {
        describeConsumerGroups(groupIds, ar -> {});
    }

    /**
     * Describe some group ids in the cluster, with the default options
     * @param groupIds the ids of the groups to describe
     * @return
     */
    public io.reactivex.Single<Map<String, io.vertx.kafka.admin.ConsumerGroupDescription>> rxDescribeConsumerGroups(List<String> groupIds) {
        return AsyncResultSingle.toSingle($handler -> {
            describeConsumerGroups(groupIds, $handler);
        });
    }

    /**
     * Describe the nodes in the cluster with the default options
     * @param completionHandler handler called on operation completed with the cluster description
     */
    public void describeCluster(Handler<AsyncResult<io.vertx.kafka.admin.ClusterDescription>> completionHandler) {
        delegate.describeCluster(completionHandler);
    }

    /**
     * Describe the nodes in the cluster with the default options
     */
    public void describeCluster() {
        describeCluster(ar -> {});
    }

    /**
     * Describe the nodes in the cluster with the default options
     * @return
     */
    public io.reactivex.Single<io.vertx.kafka.admin.ClusterDescription> rxDescribeCluster() {
        return AsyncResultSingle.toSingle($handler -> {
            describeCluster($handler);
        });
    }

    /**
     * Delete consumer groups from the cluster.
     * @param groupIds the ids of the groups to delete
     * @param completionHandler handler called on operation completed
     */
    public void deleteConsumerGroups(List<String> groupIds, Handler<AsyncResult<Void>> completionHandler) {
        delegate.deleteConsumerGroups(groupIds, completionHandler);
    }

    /**
     * Delete consumer groups from the cluster.
     * @param groupIds the ids of the groups to delete
     */
    public void deleteConsumerGroups(List<String> groupIds) {
        deleteConsumerGroups(groupIds, ar -> {});
    }

    /**
     * Delete consumer groups from the cluster.
     * @param groupIds the ids of the groups to delete
     * @return
     */
    public io.reactivex.Completable rxDeleteConsumerGroups(List<String> groupIds) {
        return AsyncResultCompletable.toCompletable($handler -> {
            deleteConsumerGroups(groupIds, $handler);
        });
    }

    /**
     * Delete committed offsets for a set of partitions in a consumer group. This will
     * succeed at the partition level only if the group is not actively subscribed
     * to the corresponding topic.
     * @param groupId The group id of the group whose offsets will be deleted
     * @param partitions The set of partitions in the consumer group whose offsets will be deleted
     * @param completionHandler
     */
    public void deleteConsumerGroupOffsets(
        String groupId,
        Set<io.vertx.kafka.client.common.TopicPartition> partitions,
        Handler<AsyncResult<Void>> completionHandler
    ) {
        delegate.deleteConsumerGroupOffsets(groupId, partitions, completionHandler);
    }

    /**
     * Delete committed offsets for a set of partitions in a consumer group. This will
     * succeed at the partition level only if the group is not actively subscribed
     * to the corresponding topic.
     * @param groupId The group id of the group whose offsets will be deleted
     * @param partitions The set of partitions in the consumer group whose offsets will be deleted
     */
    public void deleteConsumerGroupOffsets(String groupId, Set<io.vertx.kafka.client.common.TopicPartition> partitions) {
        deleteConsumerGroupOffsets(groupId, partitions, ar -> {});
    }

    /**
     * Delete committed offsets for a set of partitions in a consumer group. This will
     * succeed at the partition level only if the group is not actively subscribed
     * to the corresponding topic.
     * @param groupId The group id of the group whose offsets will be deleted
     * @param partitions The set of partitions in the consumer group whose offsets will be deleted
     * @return
     */
    public io.reactivex.Completable rxDeleteConsumerGroupOffsets(
        String groupId,
        Set<io.vertx.kafka.client.common.TopicPartition> partitions
    ) {
        return AsyncResultCompletable.toCompletable($handler -> {
            deleteConsumerGroupOffsets(groupId, partitions, $handler);
        });
    }

    /**
     * Close the admin client
     * @param handler a <code>Handler</code> completed with the operation result
     */
    public void close(Handler<AsyncResult<Void>> handler) {
        delegate.close(handler);
    }

    /**
     * Close the admin client
     */
    public void close() {
        close(ar -> {});
    }

    /**
     * Close the admin client
     * @return
     */
    public io.reactivex.Completable rxClose() {
        return AsyncResultCompletable.toCompletable($handler -> {
            close($handler);
        });
    }

    /**
     * Close the admin client
     * @param timeout timeout to wait for closing
     * @param handler a <code>Handler</code> completed with the operation result
     */
    public void close(long timeout, Handler<AsyncResult<Void>> handler) {
        delegate.close(timeout, handler);
    }

    /**
     * Close the admin client
     * @param timeout timeout to wait for closing
     */
    public void close(long timeout) {
        close(timeout, ar -> {});
    }

    /**
     * Close the admin client
     * @param timeout timeout to wait for closing
     * @return
     */
    public io.reactivex.Completable rxClose(long timeout) {
        return AsyncResultCompletable.toCompletable($handler -> {
            close(timeout, $handler);
        });
    }
}
