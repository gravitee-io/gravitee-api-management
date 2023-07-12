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
package io.gravitee.plugin.endpoint.kafka.vertx.client.consumer;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.lang.rx.RxGen;
import io.vertx.lang.rx.TypeArg;
import io.vertx.reactivex.FlowableHelper;
import io.vertx.reactivex.ObservableHelper;
import io.vertx.reactivex.impl.AsyncResultCompletable;
import io.vertx.reactivex.impl.AsyncResultSingle;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.Generated;

/**
 * Vert.x Kafka consumer.
 * <p>
 * You receive Kafka records by providing a {@link KafkaConsumer#handler}. As messages arrive the handler
 * will be called with the records.
 * <p>
 * The {@link KafkaConsumer#pause} and {@link KafkaConsumer#resume} provides global control over reading the records from the consumer.
 * <p>
 * The {@link KafkaConsumer#pause} and {@link KafkaConsumer#resume} provides finer grained control over reading records
 * for specific Topic/Partition, these are Kafka's specific operations.
 *
 * <p/>
 * NOTE: This class has been automatically generated from the {@link io.vertx.kafka.client.consumer.KafkaConsumer original} non RX-ified interface using Vert.x codegen.
 */

@RxGen(io.vertx.kafka.client.consumer.KafkaConsumer.class)
@Generated
public class KafkaConsumer<K, V> implements io.vertx.reactivex.core.streams.ReadStream<KafkaConsumerRecord<K, V>> {

    public static final TypeArg<KafkaConsumer> __TYPE_ARG = new TypeArg<>(
        obj -> new KafkaConsumer((io.vertx.kafka.client.consumer.KafkaConsumer) obj),
        KafkaConsumer::getDelegate
    );
    public final TypeArg<K> __typeArg_0;
    public final TypeArg<V> __typeArg_1;
    private final io.vertx.kafka.client.consumer.KafkaConsumer<K, V> delegate;
    private io.reactivex.Observable<KafkaConsumerRecord<K, V>> observable;
    private io.reactivex.Flowable<KafkaConsumerRecord<K, V>> flowable;

    public KafkaConsumer(io.vertx.kafka.client.consumer.KafkaConsumer delegate) {
        this.delegate = delegate;
        this.__typeArg_0 = TypeArg.unknown();
        this.__typeArg_1 = TypeArg.unknown();
    }

    public KafkaConsumer(Object delegate, TypeArg<K> typeArg_0, TypeArg<V> typeArg_1) {
        this.delegate = (io.vertx.kafka.client.consumer.KafkaConsumer) delegate;
        this.__typeArg_0 = typeArg_0;
        this.__typeArg_1 = typeArg_1;
    }

    /**
     * Create a new KafkaConsumer instance
     * @param vertx Vert.x instance to use
     * @param config Kafka consumer configuration
     * @return an instance of the KafkaConsumer
     */
    public static <K, V> KafkaConsumer<K, V> create(io.vertx.reactivex.core.Vertx vertx, Map<String, String> config) {
        KafkaConsumer<K, V> ret = KafkaConsumer.newInstance(
            io.vertx.kafka.client.consumer.KafkaConsumer.create(vertx.getDelegate(), config),
            TypeArg.unknown(),
            TypeArg.unknown()
        );
        return ret;
    }

    /**
     * Create a new KafkaConsumer instance
     * @param vertx Vert.x instance to use
     * @param config Kafka consumer configuration
     * @param keyType class type for the key deserialization
     * @param valueType class type for the value deserialization
     * @return an instance of the KafkaConsumer
     */
    public static <K, V> KafkaConsumer<K, V> create(
        io.vertx.reactivex.core.Vertx vertx,
        Map<String, String> config,
        Class<K> keyType,
        Class<V> valueType
    ) {
        KafkaConsumer<K, V> ret = KafkaConsumer.newInstance(
            io.vertx.kafka.client.consumer.KafkaConsumer.create(
                vertx.getDelegate(),
                config,
                io.vertx.lang.reactivex.Helper.unwrap(keyType),
                io.vertx.lang.reactivex.Helper.unwrap(valueType)
            ),
            TypeArg.of(keyType),
            TypeArg.of(valueType)
        );
        return ret;
    }

    /**
     * Create a new KafkaConsumer instance
     * @param vertx Vert.x instance to use
     * @param options Kafka consumer options
     * @return an instance of the KafkaConsumer
     */
    public static <K, V> KafkaConsumer<K, V> create(
        io.vertx.reactivex.core.Vertx vertx,
        io.vertx.kafka.client.common.KafkaClientOptions options
    ) {
        KafkaConsumer<K, V> ret = KafkaConsumer.newInstance(
            io.vertx.kafka.client.consumer.KafkaConsumer.create(vertx.getDelegate(), options),
            TypeArg.unknown(),
            TypeArg.unknown()
        );
        return ret;
    }

    /**
     * Create a new KafkaConsumer instance
     * @param vertx Vert.x instance to use
     * @param options Kafka consumer options
     * @param keyType class type for the key deserialization
     * @param valueType class type for the value deserialization
     * @return an instance of the KafkaConsumer
     */
    public static <K, V> KafkaConsumer<K, V> create(
        io.vertx.reactivex.core.Vertx vertx,
        io.vertx.kafka.client.common.KafkaClientOptions options,
        Class<K> keyType,
        Class<V> valueType
    ) {
        KafkaConsumer<K, V> ret = KafkaConsumer.newInstance(
            io.vertx.kafka.client.consumer.KafkaConsumer.create(
                vertx.getDelegate(),
                options,
                io.vertx.lang.reactivex.Helper.unwrap(keyType),
                io.vertx.lang.reactivex.Helper.unwrap(valueType)
            ),
            TypeArg.of(keyType),
            TypeArg.of(valueType)
        );
        return ret;
    }

    public static <K, V> KafkaConsumer<K, V> newInstance(io.vertx.kafka.client.consumer.KafkaConsumer arg) {
        return arg != null ? new KafkaConsumer<K, V>(arg) : null;
    }

    public static <K, V> KafkaConsumer<K, V> newInstance(
        io.vertx.kafka.client.consumer.KafkaConsumer arg,
        TypeArg<K> __typeArg_K,
        TypeArg<V> __typeArg_V
    ) {
        return arg != null ? new KafkaConsumer<K, V>(arg, __typeArg_K, __typeArg_V) : null;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaConsumer that = (KafkaConsumer) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    public io.vertx.kafka.client.consumer.KafkaConsumer getDelegate() {
        return delegate;
    }

    public synchronized io.reactivex.Observable<KafkaConsumerRecord<K, V>> toObservable() {
        if (observable == null) {
            Function<io.vertx.kafka.client.consumer.KafkaConsumerRecord<K, V>, KafkaConsumerRecord<K, V>> conv =
                KafkaConsumerRecord::newInstance;
            observable = ObservableHelper.toObservable(delegate, conv);
        }
        return observable;
    }

    public synchronized io.reactivex.Flowable<KafkaConsumerRecord<K, V>> toFlowable() {
        if (flowable == null) {
            Function<io.vertx.kafka.client.consumer.KafkaConsumerRecord<K, V>, KafkaConsumerRecord<K, V>> conv =
                KafkaConsumerRecord::newInstance;
            flowable = FlowableHelper.toFlowable(delegate, conv);
        }
        return flowable;
    }

    /**
     * Pause this stream and return a  to transfer the elements of this stream to a destination .
     * <p/>
     * The stream will be resumed when the pipe will be wired to a <code>WriteStream</code>.
     * @return a pipe
     */
    public io.vertx.reactivex.core.streams.Pipe<KafkaConsumerRecord<K, V>> pipe() {
        io.vertx.reactivex.core.streams.Pipe<KafkaConsumerRecord<K, V>> ret = io.vertx.reactivex.core.streams.Pipe.newInstance(
            delegate.pipe(),
            new TypeArg<KafkaConsumerRecord<K, V>>(
                o0 -> KafkaConsumerRecord.newInstance((io.vertx.kafka.client.consumer.KafkaConsumerRecord) o0, __typeArg_0, __typeArg_1),
                o0 -> o0.getDelegate()
            )
        );
        return ret;
    }

    /**
     * Pipe this <code>ReadStream</code> to the <code>WriteStream</code>.
     * <p>
     * Elements emitted by this stream will be written to the write stream until this stream ends or fails.
     * <p>
     * Once this stream has ended or failed, the write stream will be ended and the <code>handler</code> will be
     * called with the result.
     * @param dst the destination write stream
     * @param handler
     */
    public void pipeTo(io.vertx.reactivex.core.streams.WriteStream<KafkaConsumerRecord<K, V>> dst, Handler<AsyncResult<Void>> handler) {
        delegate.pipeTo(dst.getDelegate(), handler);
    }

    /**
     * Pipe this <code>ReadStream</code> to the <code>WriteStream</code>.
     * <p>
     * Elements emitted by this stream will be written to the write stream until this stream ends or fails.
     * <p>
     * Once this stream has ended or failed, the write stream will be ended and the <code>handler</code> will be
     * called with the result.
     * @param dst the destination write stream
     */
    public void pipeTo(io.vertx.reactivex.core.streams.WriteStream<KafkaConsumerRecord<K, V>> dst) {
        pipeTo(dst, ar -> {});
    }

    /**
     * Pipe this <code>ReadStream</code> to the <code>WriteStream</code>.
     * <p>
     * Elements emitted by this stream will be written to the write stream until this stream ends or fails.
     * <p>
     * Once this stream has ended or failed, the write stream will be ended and the <code>handler</code> will be
     * called with the result.
     * @param dst the destination write stream
     * @return
     */
    public io.reactivex.Completable rxPipeTo(io.vertx.reactivex.core.streams.WriteStream<KafkaConsumerRecord<K, V>> dst) {
        return AsyncResultCompletable.toCompletable($handler -> {
            pipeTo(dst, $handler);
        });
    }

    public KafkaConsumer<K, V> exceptionHandler(Handler<Throwable> handler) {
        delegate.exceptionHandler(handler);
        return this;
    }

    public KafkaConsumer<K, V> handler(Handler<KafkaConsumerRecord<K, V>> handler) {
        delegate.handler(
            new Handler<io.vertx.kafka.client.consumer.KafkaConsumerRecord<K, V>>() {
                public void handle(io.vertx.kafka.client.consumer.KafkaConsumerRecord<K, V> event) {
                    handler.handle(KafkaConsumerRecord.newInstance(event, __typeArg_0, __typeArg_1));
                }
            }
        );
        return this;
    }

    public KafkaConsumer<K, V> pause() {
        delegate.pause();
        return this;
    }

    public KafkaConsumer<K, V> resume() {
        delegate.resume();
        return this;
    }

    public KafkaConsumer<K, V> fetch(long amount) {
        delegate.fetch(amount);
        return this;
    }

    public KafkaConsumer<K, V> endHandler(Handler<Void> endHandler) {
        delegate.endHandler(endHandler);
        return this;
    }

    /**
     * Returns the current demand.
     *
     * <ul>
     *   <i>If the stream is in <i>flowing</i> mode will return {@link Long}.</i>
     *   <li>If the stream is in <i>fetch</i> mode, will return the current number of elements still to be delivered or 0 if paused.</li>
     * </ul>
     * @return current demand
     */
    public long demand() {
        long ret = delegate.demand();
        return ret;
    }

    /**
     * Subscribe to the given topic to get dynamically assigned partitions.
     * <p>
     * Due to internal buffering of messages, when changing the subscribed topic
     * the old topic may remain in effect
     * (as observed by the  record handler})
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will only see messages
     * consistent with the new topic.
     * @param topic topic to subscribe to
     * @param completionHandler handler called on operation completed
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> subscribe(String topic, Handler<AsyncResult<Void>> completionHandler) {
        delegate.subscribe(topic, completionHandler);
        return this;
    }

    /**
     * Subscribe to the given topic to get dynamically assigned partitions.
     * <p>
     * Due to internal buffering of messages, when changing the subscribed topic
     * the old topic may remain in effect
     * (as observed by the  record handler})
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will only see messages
     * consistent with the new topic.
     * @param topic topic to subscribe to
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> subscribe(String topic) {
        return subscribe(topic, ar -> {});
    }

    /**
     * Subscribe to the given topic to get dynamically assigned partitions.
     * <p>
     * Due to internal buffering of messages, when changing the subscribed topic
     * the old topic may remain in effect
     * (as observed by the  record handler})
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will only see messages
     * consistent with the new topic.
     * @param topic topic to subscribe to
     * @return current KafkaConsumer instance
     */
    public io.reactivex.Completable rxSubscribe(String topic) {
        return AsyncResultCompletable.toCompletable($handler -> {
            subscribe(topic, $handler);
        });
    }

    /**
     * Subscribe to the given list of topics to get dynamically assigned partitions.
     * <p>
     * Due to internal buffering of messages, when changing the subscribed topics
     * the old set of topics may remain in effect
     * (as observed by the  record handler})
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will only see messages
     * consistent with the new set of topics.
     * @param topics topics to subscribe to
     * @param completionHandler handler called on operation completed
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> subscribe(Set<String> topics, Handler<AsyncResult<Void>> completionHandler) {
        delegate.subscribe(topics, completionHandler);
        return this;
    }

    /**
     * Subscribe to the given list of topics to get dynamically assigned partitions.
     * <p>
     * Due to internal buffering of messages, when changing the subscribed topics
     * the old set of topics may remain in effect
     * (as observed by the  record handler})
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will only see messages
     * consistent with the new set of topics.
     * @param topics topics to subscribe to
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> subscribe(Set<String> topics) {
        return subscribe(topics, ar -> {});
    }

    /**
     * Subscribe to the given list of topics to get dynamically assigned partitions.
     * <p>
     * Due to internal buffering of messages, when changing the subscribed topics
     * the old set of topics may remain in effect
     * (as observed by the  record handler})
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will only see messages
     * consistent with the new set of topics.
     * @param topics topics to subscribe to
     * @return current KafkaConsumer instance
     */
    public io.reactivex.Completable rxSubscribe(Set<String> topics) {
        return AsyncResultCompletable.toCompletable($handler -> {
            subscribe(topics, $handler);
        });
    }

    /**
     * Manually assign a partition to this consumer.
     * <p>
     * Due to internal buffering of messages, when reassigning
     * the old partition may remain in effect
     * (as observed by the  record handler)}
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will only see messages
     * consistent with the new partition.
     * @param topicPartition partition which want assigned
     * @param completionHandler handler called on operation completed
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> assign(
        io.vertx.kafka.client.common.TopicPartition topicPartition,
        Handler<AsyncResult<Void>> completionHandler
    ) {
        delegate.assign(topicPartition, completionHandler);
        return this;
    }

    /**
     * Manually assign a partition to this consumer.
     * <p>
     * Due to internal buffering of messages, when reassigning
     * the old partition may remain in effect
     * (as observed by the  record handler)}
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will only see messages
     * consistent with the new partition.
     * @param topicPartition partition which want assigned
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> assign(io.vertx.kafka.client.common.TopicPartition topicPartition) {
        return assign(topicPartition, ar -> {});
    }

    /**
     * Manually assign a partition to this consumer.
     * <p>
     * Due to internal buffering of messages, when reassigning
     * the old partition may remain in effect
     * (as observed by the  record handler)}
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will only see messages
     * consistent with the new partition.
     * @param topicPartition partition which want assigned
     * @return current KafkaConsumer instance
     */
    public io.reactivex.Completable rxAssign(io.vertx.kafka.client.common.TopicPartition topicPartition) {
        return AsyncResultCompletable.toCompletable($handler -> {
            assign(topicPartition, $handler);
        });
    }

    /**
     * Manually assign a list of partition to this consumer.
     * <p>
     * Due to internal buffering of messages, when reassigning
     * the old set of partitions may remain in effect
     * (as observed by the  record handler)}
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will only see messages
     * consistent with the new set of partitions.
     * @param topicPartitions partitions which want assigned
     * @param completionHandler handler called on operation completed
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> assign(
        Set<io.vertx.kafka.client.common.TopicPartition> topicPartitions,
        Handler<AsyncResult<Void>> completionHandler
    ) {
        delegate.assign(topicPartitions, completionHandler);
        return this;
    }

    /**
     * Manually assign a list of partition to this consumer.
     * <p>
     * Due to internal buffering of messages, when reassigning
     * the old set of partitions may remain in effect
     * (as observed by the  record handler)}
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will only see messages
     * consistent with the new set of partitions.
     * @param topicPartitions partitions which want assigned
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> assign(Set<io.vertx.kafka.client.common.TopicPartition> topicPartitions) {
        return assign(topicPartitions, ar -> {});
    }

    /**
     * Manually assign a list of partition to this consumer.
     * <p>
     * Due to internal buffering of messages, when reassigning
     * the old set of partitions may remain in effect
     * (as observed by the  record handler)}
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will only see messages
     * consistent with the new set of partitions.
     * @param topicPartitions partitions which want assigned
     * @return current KafkaConsumer instance
     */
    public io.reactivex.Completable rxAssign(Set<io.vertx.kafka.client.common.TopicPartition> topicPartitions) {
        return AsyncResultCompletable.toCompletable($handler -> {
            assign(topicPartitions, $handler);
        });
    }

    /**
     * Get the set of partitions currently assigned to this consumer.
     * @param handler handler called on operation completed
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> assignment(Handler<AsyncResult<Set<io.vertx.kafka.client.common.TopicPartition>>> handler) {
        delegate.assignment(handler);
        return this;
    }

    /**
     * Get the set of partitions currently assigned to this consumer.
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> assignment() {
        return assignment(ar -> {});
    }

    /**
     * Get the set of partitions currently assigned to this consumer.
     * @return current KafkaConsumer instance
     */
    public io.reactivex.Single<Set<io.vertx.kafka.client.common.TopicPartition>> rxAssignment() {
        return AsyncResultSingle.toSingle($handler -> {
            assignment($handler);
        });
    }

    /**
     * Unsubscribe from topics currently subscribed with subscribe.
     * @param completionHandler handler called on operation completed
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> unsubscribe(Handler<AsyncResult<Void>> completionHandler) {
        delegate.unsubscribe(completionHandler);
        return this;
    }

    /**
     * Unsubscribe from topics currently subscribed with subscribe.
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> unsubscribe() {
        return unsubscribe(ar -> {});
    }

    /**
     * Unsubscribe from topics currently subscribed with subscribe.
     * @return current KafkaConsumer instance
     */
    public io.reactivex.Completable rxUnsubscribe() {
        return AsyncResultCompletable.toCompletable($handler -> {
            unsubscribe($handler);
        });
    }

    /**
     * Get the current subscription.
     * @param handler handler called on operation completed
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> subscription(Handler<AsyncResult<Set<String>>> handler) {
        delegate.subscription(handler);
        return this;
    }

    /**
     * Get the current subscription.
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> subscription() {
        return subscription(ar -> {});
    }

    /**
     * Get the current subscription.
     * @return current KafkaConsumer instance
     */
    public io.reactivex.Single<Set<String>> rxSubscription() {
        return AsyncResultSingle.toSingle($handler -> {
            subscription($handler);
        });
    }

    /**
     * Suspend fetching from the requested partition.
     * <p>
     * Due to internal buffering of messages,
     * the  will
     * continue to observe messages from the given <code>topicPartition</code>
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will not see messages
     * from the given <code>topicPartition</code>.
     * @param topicPartition topic partition from which suspend fetching
     * @param completionHandler handler called on operation completed
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> pause(
        io.vertx.kafka.client.common.TopicPartition topicPartition,
        Handler<AsyncResult<Void>> completionHandler
    ) {
        delegate.pause(topicPartition, completionHandler);
        return this;
    }

    /**
     * Suspend fetching from the requested partition.
     * <p>
     * Due to internal buffering of messages,
     * the  will
     * continue to observe messages from the given <code>topicPartition</code>
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will not see messages
     * from the given <code>topicPartition</code>.
     * @param topicPartition topic partition from which suspend fetching
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> pause(io.vertx.kafka.client.common.TopicPartition topicPartition) {
        return pause(topicPartition, ar -> {});
    }

    /**
     * Suspend fetching from the requested partition.
     * <p>
     * Due to internal buffering of messages,
     * the  will
     * continue to observe messages from the given <code>topicPartition</code>
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will not see messages
     * from the given <code>topicPartition</code>.
     * @param topicPartition topic partition from which suspend fetching
     * @return current KafkaConsumer instance
     */
    public io.reactivex.Completable rxPause(io.vertx.kafka.client.common.TopicPartition topicPartition) {
        return AsyncResultCompletable.toCompletable($handler -> {
            pause(topicPartition, $handler);
        });
    }

    /**
     * Suspend fetching from the requested partitions.
     * <p>
     * Due to internal buffering of messages,
     * the  will
     * continue to observe messages from the given <code>topicPartitions</code>
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will not see messages
     * from the given <code>topicPartitions</code>.
     * @param topicPartitions topic partition from which suspend fetching
     * @param completionHandler handler called on operation completed
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> pause(
        Set<io.vertx.kafka.client.common.TopicPartition> topicPartitions,
        Handler<AsyncResult<Void>> completionHandler
    ) {
        delegate.pause(topicPartitions, completionHandler);
        return this;
    }

    /**
     * Suspend fetching from the requested partitions.
     * <p>
     * Due to internal buffering of messages,
     * the  will
     * continue to observe messages from the given <code>topicPartitions</code>
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will not see messages
     * from the given <code>topicPartitions</code>.
     * @param topicPartitions topic partition from which suspend fetching
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> pause(Set<io.vertx.kafka.client.common.TopicPartition> topicPartitions) {
        return pause(topicPartitions, ar -> {});
    }

    /**
     * Suspend fetching from the requested partitions.
     * <p>
     * Due to internal buffering of messages,
     * the  will
     * continue to observe messages from the given <code>topicPartitions</code>
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will not see messages
     * from the given <code>topicPartitions</code>.
     * @param topicPartitions topic partition from which suspend fetching
     * @return current KafkaConsumer instance
     */
    public io.reactivex.Completable rxPause(Set<io.vertx.kafka.client.common.TopicPartition> topicPartitions) {
        return AsyncResultCompletable.toCompletable($handler -> {
            pause(topicPartitions, $handler);
        });
    }

    /**
     * Get the set of partitions that were previously paused by a call to pause(Set).
     * @param handler handler called on operation completed
     */
    public void paused(Handler<AsyncResult<Set<io.vertx.kafka.client.common.TopicPartition>>> handler) {
        delegate.paused(handler);
    }

    /**
     * Get the set of partitions that were previously paused by a call to pause(Set).
     */
    public void paused() {
        paused(ar -> {});
    }

    /**
     * Get the set of partitions that were previously paused by a call to pause(Set).
     * @return
     */
    public io.reactivex.Single<Set<io.vertx.kafka.client.common.TopicPartition>> rxPaused() {
        return AsyncResultSingle.toSingle($handler -> {
            paused($handler);
        });
    }

    /**
     * Resume specified partition which have been paused with pause.
     * @param topicPartition topic partition from which resume fetching
     * @param completionHandler handler called on operation completed
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> resume(
        io.vertx.kafka.client.common.TopicPartition topicPartition,
        Handler<AsyncResult<Void>> completionHandler
    ) {
        delegate.resume(topicPartition, completionHandler);
        return this;
    }

    /**
     * Resume specified partition which have been paused with pause.
     * @param topicPartition topic partition from which resume fetching
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> resume(io.vertx.kafka.client.common.TopicPartition topicPartition) {
        return resume(topicPartition, ar -> {});
    }

    /**
     * Resume specified partition which have been paused with pause.
     * @param topicPartition topic partition from which resume fetching
     * @return current KafkaConsumer instance
     */
    public io.reactivex.Completable rxResume(io.vertx.kafka.client.common.TopicPartition topicPartition) {
        return AsyncResultCompletable.toCompletable($handler -> {
            resume(topicPartition, $handler);
        });
    }

    /**
     * Resume specified partitions which have been paused with pause.
     * @param topicPartitions topic partition from which resume fetching
     * @param completionHandler handler called on operation completed
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> resume(
        Set<io.vertx.kafka.client.common.TopicPartition> topicPartitions,
        Handler<AsyncResult<Void>> completionHandler
    ) {
        delegate.resume(topicPartitions, completionHandler);
        return this;
    }

    /**
     * Resume specified partitions which have been paused with pause.
     * @param topicPartitions topic partition from which resume fetching
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> resume(Set<io.vertx.kafka.client.common.TopicPartition> topicPartitions) {
        return resume(topicPartitions, ar -> {});
    }

    /**
     * Resume specified partitions which have been paused with pause.
     * @param topicPartitions topic partition from which resume fetching
     * @return current KafkaConsumer instance
     */
    public io.reactivex.Completable rxResume(Set<io.vertx.kafka.client.common.TopicPartition> topicPartitions) {
        return AsyncResultCompletable.toCompletable($handler -> {
            resume(topicPartitions, $handler);
        });
    }

    /**
     * Set the handler called when topic partitions are revoked to the consumer
     * @param handler handler called on revoked topic partitions
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> partitionsRevokedHandler(Handler<Set<io.vertx.kafka.client.common.TopicPartition>> handler) {
        delegate.partitionsRevokedHandler(handler);
        return this;
    }

    /**
     * Set the handler called when topic partitions are assigned to the consumer
     * @param handler handler called on assigned topic partitions
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> partitionsAssignedHandler(Handler<Set<io.vertx.kafka.client.common.TopicPartition>> handler) {
        delegate.partitionsAssignedHandler(handler);
        return this;
    }

    /**
     * Overrides the fetch offsets that the consumer will use on the next poll.
     * <p>
     * Due to internal buffering of messages,
     * the  will
     * continue to observe messages fetched with respect to the old offset
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will only see messages
     * consistent with the new offset.
     * @param topicPartition topic partition for which seek
     * @param offset offset to seek inside the topic partition
     * @param completionHandler handler called on operation completed
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> seek(
        io.vertx.kafka.client.common.TopicPartition topicPartition,
        long offset,
        Handler<AsyncResult<Void>> completionHandler
    ) {
        delegate.seek(topicPartition, offset, completionHandler);
        return this;
    }

    /**
     * Overrides the fetch offsets that the consumer will use on the next poll.
     * <p>
     * Due to internal buffering of messages,
     * the  will
     * continue to observe messages fetched with respect to the old offset
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will only see messages
     * consistent with the new offset.
     * @param topicPartition topic partition for which seek
     * @param offset offset to seek inside the topic partition
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> seek(io.vertx.kafka.client.common.TopicPartition topicPartition, long offset) {
        return seek(topicPartition, offset, ar -> {});
    }

    /**
     * Overrides the fetch offsets that the consumer will use on the next poll.
     * <p>
     * Due to internal buffering of messages,
     * the  will
     * continue to observe messages fetched with respect to the old offset
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will only see messages
     * consistent with the new offset.
     * @param topicPartition topic partition for which seek
     * @param offset offset to seek inside the topic partition
     * @return current KafkaConsumer instance
     */
    public io.reactivex.Completable rxSeek(io.vertx.kafka.client.common.TopicPartition topicPartition, long offset) {
        return AsyncResultCompletable.toCompletable($handler -> {
            seek(topicPartition, offset, $handler);
        });
    }

    /**
     * Seek to the first offset for each of the given partition.
     * <p>
     * Due to internal buffering of messages,
     * the  will
     * continue to observe messages fetched with respect to the old offset
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will only see messages
     * consistent with the new offset.
     * @param topicPartition topic partition for which seek
     * @param completionHandler handler called on operation completed
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> seekToBeginning(
        io.vertx.kafka.client.common.TopicPartition topicPartition,
        Handler<AsyncResult<Void>> completionHandler
    ) {
        delegate.seekToBeginning(topicPartition, completionHandler);
        return this;
    }

    /**
     * Seek to the first offset for each of the given partition.
     * <p>
     * Due to internal buffering of messages,
     * the  will
     * continue to observe messages fetched with respect to the old offset
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will only see messages
     * consistent with the new offset.
     * @param topicPartition topic partition for which seek
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> seekToBeginning(io.vertx.kafka.client.common.TopicPartition topicPartition) {
        return seekToBeginning(topicPartition, ar -> {});
    }

    /**
     * Seek to the first offset for each of the given partition.
     * <p>
     * Due to internal buffering of messages,
     * the  will
     * continue to observe messages fetched with respect to the old offset
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will only see messages
     * consistent with the new offset.
     * @param topicPartition topic partition for which seek
     * @return current KafkaConsumer instance
     */
    public io.reactivex.Completable rxSeekToBeginning(io.vertx.kafka.client.common.TopicPartition topicPartition) {
        return AsyncResultCompletable.toCompletable($handler -> {
            seekToBeginning(topicPartition, $handler);
        });
    }

    /**
     * Seek to the first offset for each of the given partitions.
     * <p>
     * Due to internal buffering of messages,
     * the  will
     * continue to observe messages fetched with respect to the old offset
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will only see messages
     * consistent with the new offset.
     * @param topicPartitions topic partition for which seek
     * @param completionHandler handler called on operation completed
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> seekToBeginning(
        Set<io.vertx.kafka.client.common.TopicPartition> topicPartitions,
        Handler<AsyncResult<Void>> completionHandler
    ) {
        delegate.seekToBeginning(topicPartitions, completionHandler);
        return this;
    }

    /**
     * Seek to the first offset for each of the given partitions.
     * <p>
     * Due to internal buffering of messages,
     * the  will
     * continue to observe messages fetched with respect to the old offset
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will only see messages
     * consistent with the new offset.
     * @param topicPartitions topic partition for which seek
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> seekToBeginning(Set<io.vertx.kafka.client.common.TopicPartition> topicPartitions) {
        return seekToBeginning(topicPartitions, ar -> {});
    }

    /**
     * Seek to the first offset for each of the given partitions.
     * <p>
     * Due to internal buffering of messages,
     * the  will
     * continue to observe messages fetched with respect to the old offset
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will only see messages
     * consistent with the new offset.
     * @param topicPartitions topic partition for which seek
     * @return current KafkaConsumer instance
     */
    public io.reactivex.Completable rxSeekToBeginning(Set<io.vertx.kafka.client.common.TopicPartition> topicPartitions) {
        return AsyncResultCompletable.toCompletable($handler -> {
            seekToBeginning(topicPartitions, $handler);
        });
    }

    /**
     * Seek to the last offset for each of the given partition.
     * <p>
     * Due to internal buffering of messages,
     * the  will
     * continue to observe messages fetched with respect to the old offset
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will only see messages
     * consistent with the new offset.
     * @param topicPartition topic partition for which seek
     * @param completionHandler handler called on operation completed
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> seekToEnd(
        io.vertx.kafka.client.common.TopicPartition topicPartition,
        Handler<AsyncResult<Void>> completionHandler
    ) {
        delegate.seekToEnd(topicPartition, completionHandler);
        return this;
    }

    /**
     * Seek to the last offset for each of the given partition.
     * <p>
     * Due to internal buffering of messages,
     * the  will
     * continue to observe messages fetched with respect to the old offset
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will only see messages
     * consistent with the new offset.
     * @param topicPartition topic partition for which seek
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> seekToEnd(io.vertx.kafka.client.common.TopicPartition topicPartition) {
        return seekToEnd(topicPartition, ar -> {});
    }

    /**
     * Seek to the last offset for each of the given partition.
     * <p>
     * Due to internal buffering of messages,
     * the  will
     * continue to observe messages fetched with respect to the old offset
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will only see messages
     * consistent with the new offset.
     * @param topicPartition topic partition for which seek
     * @return current KafkaConsumer instance
     */
    public io.reactivex.Completable rxSeekToEnd(io.vertx.kafka.client.common.TopicPartition topicPartition) {
        return AsyncResultCompletable.toCompletable($handler -> {
            seekToEnd(topicPartition, $handler);
        });
    }

    /**
     * Seek to the last offset for each of the given partitions.
     * <p>
     * Due to internal buffering of messages,
     * the  will
     * continue to observe messages fetched with respect to the old offset
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will only see messages
     * consistent with the new offset.
     * @param topicPartitions topic partition for which seek
     * @param completionHandler handler called on operation completed
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> seekToEnd(
        Set<io.vertx.kafka.client.common.TopicPartition> topicPartitions,
        Handler<AsyncResult<Void>> completionHandler
    ) {
        delegate.seekToEnd(topicPartitions, completionHandler);
        return this;
    }

    /**
     * Seek to the last offset for each of the given partitions.
     * <p>
     * Due to internal buffering of messages,
     * the  will
     * continue to observe messages fetched with respect to the old offset
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will only see messages
     * consistent with the new offset.
     * @param topicPartitions topic partition for which seek
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> seekToEnd(Set<io.vertx.kafka.client.common.TopicPartition> topicPartitions) {
        return seekToEnd(topicPartitions, ar -> {});
    }

    /**
     * Seek to the last offset for each of the given partitions.
     * <p>
     * Due to internal buffering of messages,
     * the  will
     * continue to observe messages fetched with respect to the old offset
     * until some time <em>after</em> the given <code>completionHandler</code>
     * is called. In contrast, the once the given <code>completionHandler</code>
     * is called the {@link KafkaConsumer#batchHandler} will only see messages
     * consistent with the new offset.
     * @param topicPartitions topic partition for which seek
     * @return current KafkaConsumer instance
     */
    public io.reactivex.Completable rxSeekToEnd(Set<io.vertx.kafka.client.common.TopicPartition> topicPartitions) {
        return AsyncResultCompletable.toCompletable($handler -> {
            seekToEnd(topicPartitions, $handler);
        });
    }

    /**
     * Commit current offsets for all the subscribed list of topics and partition.
     * @param completionHandler handler called on operation completed
     */
    public void commit(Handler<AsyncResult<Void>> completionHandler) {
        delegate.commit(completionHandler);
    }

    /**
     * Commit current offsets for all the subscribed list of topics and partition.
     */
    public void commit() {
        commit(ar -> {});
    }

    /**
     * Commit current offsets for all the subscribed list of topics and partition.
     * @return
     */
    public io.reactivex.Completable rxCommit() {
        return AsyncResultCompletable.toCompletable($handler -> {
            commit($handler);
        });
    }

    /**
     * Get the last committed offset for the given partition (whether the commit happened by this process or another).
     * @param topicPartition topic partition for getting last committed offset
     * @param handler handler called on operation completed
     */
    public void committed(
        io.vertx.kafka.client.common.TopicPartition topicPartition,
        Handler<AsyncResult<io.vertx.kafka.client.consumer.OffsetAndMetadata>> handler
    ) {
        delegate.committed(topicPartition, handler);
    }

    /**
     * Get the last committed offset for the given partition (whether the commit happened by this process or another).
     * @param topicPartition topic partition for getting last committed offset
     */
    public void committed(io.vertx.kafka.client.common.TopicPartition topicPartition) {
        committed(topicPartition, ar -> {});
    }

    /**
     * Get the last committed offset for the given partition (whether the commit happened by this process or another).
     * @param topicPartition topic partition for getting last committed offset
     * @return
     */
    public io.reactivex.Single<io.vertx.kafka.client.consumer.OffsetAndMetadata> rxCommitted(
        io.vertx.kafka.client.common.TopicPartition topicPartition
    ) {
        return AsyncResultSingle.toSingle($handler -> {
            committed(topicPartition, $handler);
        });
    }

    /**
     * Get metadata about the partitions for a given topic.
     * @param topic topic partition for which getting partitions info
     * @param handler handler called on operation completed
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> partitionsFor(String topic, Handler<AsyncResult<List<io.vertx.kafka.client.common.PartitionInfo>>> handler) {
        delegate.partitionsFor(topic, handler);
        return this;
    }

    /**
     * Get metadata about the partitions for a given topic.
     * @param topic topic partition for which getting partitions info
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> partitionsFor(String topic) {
        return partitionsFor(topic, ar -> {});
    }

    /**
     * Get metadata about the partitions for a given topic.
     * @param topic topic partition for which getting partitions info
     * @return current KafkaConsumer instance
     */
    public io.reactivex.Single<List<io.vertx.kafka.client.common.PartitionInfo>> rxPartitionsFor(String topic) {
        return AsyncResultSingle.toSingle($handler -> {
            partitionsFor(topic, $handler);
        });
    }

    /**
     * Set the handler to be used when batches of messages are fetched
     * from the Kafka server. Batch handlers need to take care not to block
     * the event loop when dealing with large batches. It is better to process
     * records individually using the {@link KafkaConsumer #handler(Handler) record handler}.
     * @param handler handler called when batches of messages are fetched
     * @return current KafkaConsumer instance
     */
    public KafkaConsumer<K, V> batchHandler(Handler<KafkaConsumerRecords<K, V>> handler) {
        delegate.batchHandler(
            new Handler<io.vertx.kafka.client.consumer.KafkaConsumerRecords<K, V>>() {
                public void handle(io.vertx.kafka.client.consumer.KafkaConsumerRecords<K, V> event) {
                    handler.handle(KafkaConsumerRecords.newInstance(event, __typeArg_0, __typeArg_1));
                }
            }
        );
        return this;
    }

    /**
     * Close the consumer
     * @param completionHandler handler called on operation completed
     */
    public void close(Handler<AsyncResult<Void>> completionHandler) {
        delegate.close(completionHandler);
    }

    /**
     * Close the consumer
     */
    public void close() {
        close(ar -> {});
    }

    /**
     * Close the consumer
     * @return
     */
    public io.reactivex.Completable rxClose() {
        return AsyncResultCompletable.toCompletable($handler -> {
            close($handler);
        });
    }

    /**
     * Get the offset of the next record that will be fetched (if a record with that offset exists).
     * @param partition The partition to get the position for
     * @param handler handler called on operation completed
     */
    public void position(io.vertx.kafka.client.common.TopicPartition partition, Handler<AsyncResult<Long>> handler) {
        delegate.position(partition, handler);
    }

    /**
     * Get the offset of the next record that will be fetched (if a record with that offset exists).
     * @param partition The partition to get the position for
     */
    public void position(io.vertx.kafka.client.common.TopicPartition partition) {
        position(partition, ar -> {});
    }

    /**
     * Get the offset of the next record that will be fetched (if a record with that offset exists).
     * @param partition The partition to get the position for
     * @return
     */
    public io.reactivex.Single<Long> rxPosition(io.vertx.kafka.client.common.TopicPartition partition) {
        return AsyncResultSingle.toSingle($handler -> {
            position(partition, $handler);
        });
    }

    /**
     * Look up the offset for the given partition by timestamp. Note: the result might be null in case
     * for the given timestamp no offset can be found -- e.g., when the timestamp refers to the future
     * @param topicPartition TopicPartition to query.
     * @param timestamp Timestamp to be used in the query.
     * @param handler handler called on operation completed
     */
    public void offsetsForTimes(
        io.vertx.kafka.client.common.TopicPartition topicPartition,
        Long timestamp,
        Handler<AsyncResult<io.vertx.kafka.client.consumer.OffsetAndTimestamp>> handler
    ) {
        delegate.offsetsForTimes(topicPartition, timestamp, handler);
    }

    /**
     * Look up the offset for the given partition by timestamp. Note: the result might be null in case
     * for the given timestamp no offset can be found -- e.g., when the timestamp refers to the future
     * @param topicPartition TopicPartition to query.
     * @param timestamp Timestamp to be used in the query.
     */
    public void offsetsForTimes(io.vertx.kafka.client.common.TopicPartition topicPartition, Long timestamp) {
        offsetsForTimes(topicPartition, timestamp, ar -> {});
    }

    /**
     * Look up the offset for the given partition by timestamp. Note: the result might be null in case
     * for the given timestamp no offset can be found -- e.g., when the timestamp refers to the future
     * @param topicPartition TopicPartition to query.
     * @param timestamp Timestamp to be used in the query.
     * @return
     */
    public io.reactivex.Single<io.vertx.kafka.client.consumer.OffsetAndTimestamp> rxOffsetsForTimes(
        io.vertx.kafka.client.common.TopicPartition topicPartition,
        Long timestamp
    ) {
        return AsyncResultSingle.toSingle($handler -> {
            offsetsForTimes(topicPartition, timestamp, $handler);
        });
    }

    /**
     * Get the first offset for the given partitions.
     * @param topicPartition the partition to get the earliest offset.
     * @param handler handler called on operation completed. Returns the earliest available offset for the given partition
     */
    public void beginningOffsets(io.vertx.kafka.client.common.TopicPartition topicPartition, Handler<AsyncResult<Long>> handler) {
        delegate.beginningOffsets(topicPartition, handler);
    }

    /**
     * Get the first offset for the given partitions.
     * @param topicPartition the partition to get the earliest offset.
     */
    public void beginningOffsets(io.vertx.kafka.client.common.TopicPartition topicPartition) {
        beginningOffsets(topicPartition, ar -> {});
    }

    /**
     * Get the first offset for the given partitions.
     * @param topicPartition the partition to get the earliest offset.
     * @return
     */
    public io.reactivex.Single<Long> rxBeginningOffsets(io.vertx.kafka.client.common.TopicPartition topicPartition) {
        return AsyncResultSingle.toSingle($handler -> {
            beginningOffsets(topicPartition, $handler);
        });
    }

    /**
     * Get the last offset for the given partition. The last offset of a partition is the offset
     * of the upcoming message, i.e. the offset of the last available message + 1.
     * @param topicPartition the partition to get the end offset.
     * @param handler handler called on operation completed. The end offset for the given partition.
     */
    public void endOffsets(io.vertx.kafka.client.common.TopicPartition topicPartition, Handler<AsyncResult<Long>> handler) {
        delegate.endOffsets(topicPartition, handler);
    }

    /**
     * Get the last offset for the given partition. The last offset of a partition is the offset
     * of the upcoming message, i.e. the offset of the last available message + 1.
     * @param topicPartition the partition to get the end offset.
     */
    public void endOffsets(io.vertx.kafka.client.common.TopicPartition topicPartition) {
        endOffsets(topicPartition, ar -> {});
    }

    /**
     * Get the last offset for the given partition. The last offset of a partition is the offset
     * of the upcoming message, i.e. the offset of the last available message + 1.
     * @param topicPartition the partition to get the end offset.
     * @return
     */
    public io.reactivex.Single<Long> rxEndOffsets(io.vertx.kafka.client.common.TopicPartition topicPartition) {
        return AsyncResultSingle.toSingle($handler -> {
            endOffsets(topicPartition, $handler);
        });
    }

    /**
     * Sets the poll timeout for the underlying native Kafka Consumer. Defaults to 1000ms.
     * Setting timeout to a lower value results in a more 'responsive' client, because it will block for a shorter period
     * if no data is available in the assigned partition and therefore allows subsequent actions to be executed with a shorter
     * delay. At the same time, the client will poll more frequently and thus will potentially create a higher load on the Kafka Broker.
     * @param timeout The time, spent waiting in poll if data is not available in the buffer. If 0, returns immediately with any records that are available currently in the native Kafka consumer's buffer, else returns empty. Must not be negative.
     * @return
     */
    public KafkaConsumer<K, V> pollTimeout(java.time.Duration timeout) {
        delegate.pollTimeout(timeout);
        return this;
    }

    /**
     * Executes a poll for getting messages from Kafka.
     * @param timeout The maximum time to block (must not be greater than {@link Long} milliseconds)
     * @param handler handler called after the poll with batch of records (can be empty).
     */
    public void poll(java.time.Duration timeout, Handler<AsyncResult<KafkaConsumerRecords<K, V>>> handler) {
        delegate.poll(
            timeout,
            new Handler<AsyncResult<io.vertx.kafka.client.consumer.KafkaConsumerRecords<K, V>>>() {
                public void handle(AsyncResult<io.vertx.kafka.client.consumer.KafkaConsumerRecords<K, V>> ar) {
                    if (ar.succeeded()) {
                        handler.handle(
                            io.vertx.core.Future.succeededFuture(KafkaConsumerRecords.newInstance(ar.result(), __typeArg_0, __typeArg_1))
                        );
                    } else {
                        handler.handle(io.vertx.core.Future.failedFuture(ar.cause()));
                    }
                }
            }
        );
    }

    /**
     * Executes a poll for getting messages from Kafka.
     * @param timeout The maximum time to block (must not be greater than {@link Long} milliseconds)
     */
    public void poll(java.time.Duration timeout) {
        poll(timeout, ar -> {});
    }

    /**
     * Executes a poll for getting messages from Kafka.
     * @param timeout The maximum time to block (must not be greater than {@link Long} milliseconds)
     * @return
     */
    public io.reactivex.Single<KafkaConsumerRecords<K, V>> rxPoll(java.time.Duration timeout) {
        return AsyncResultSingle.toSingle($handler -> {
            poll(timeout, $handler);
        });
    }
}
