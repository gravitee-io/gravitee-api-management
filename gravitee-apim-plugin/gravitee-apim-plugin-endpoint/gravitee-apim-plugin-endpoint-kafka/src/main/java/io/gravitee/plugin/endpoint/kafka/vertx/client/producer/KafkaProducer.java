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
package io.gravitee.plugin.endpoint.kafka.vertx.client.producer;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.lang.rx.RxGen;
import io.vertx.lang.rx.TypeArg;
import io.vertx.reactivex.RxHelper;
import io.vertx.reactivex.WriteStreamObserver;
import io.vertx.reactivex.WriteStreamSubscriber;
import io.vertx.reactivex.impl.AsyncResultCompletable;
import io.vertx.reactivex.impl.AsyncResultSingle;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.Generated;

/**
 * Vert.x Kafka producer.
 * <p>
 * The {@link io.vertx.reactivex.core.streams.WriteStream#write} provides global control over writing a record.
 *
 * <p/>
 * NOTE: This class has been automatically generated from the {@link io.vertx.kafka.client.producer.KafkaProducer original} non RX-ified interface using Vert.x codegen.
 */

@RxGen(io.vertx.kafka.client.producer.KafkaProducer.class)
@Generated
public class KafkaProducer<K, V> implements io.vertx.reactivex.core.streams.WriteStream<KafkaProducerRecord<K, V>> {

    public static final TypeArg<KafkaProducer> __TYPE_ARG = new TypeArg<>(
        obj -> new KafkaProducer((io.vertx.kafka.client.producer.KafkaProducer) obj),
        KafkaProducer::getDelegate
    );
    public final TypeArg<K> __typeArg_0;
    public final TypeArg<V> __typeArg_1;
    private final io.vertx.kafka.client.producer.KafkaProducer<K, V> delegate;
    private WriteStreamObserver<KafkaProducerRecord<K, V>> observer;
    private WriteStreamSubscriber<KafkaProducerRecord<K, V>> subscriber;

    public KafkaProducer(io.vertx.kafka.client.producer.KafkaProducer delegate) {
        this.delegate = delegate;
        this.__typeArg_0 = TypeArg.unknown();
        this.__typeArg_1 = TypeArg.unknown();
    }

    public KafkaProducer(Object delegate, TypeArg<K> typeArg_0, TypeArg<V> typeArg_1) {
        this.delegate = (io.vertx.kafka.client.producer.KafkaProducer) delegate;
        this.__typeArg_0 = typeArg_0;
        this.__typeArg_1 = typeArg_1;
    }

    /**
     * Get or create a KafkaProducer instance which shares its stream with any other KafkaProducer created with the same <code>name</code>
     * @param vertx Vert.x instance to use
     * @param name the producer name to identify it
     * @param config Kafka producer configuration
     * @return an instance of the KafkaProducer
     */
    public static <K, V> KafkaProducer<K, V> createShared(io.vertx.reactivex.core.Vertx vertx, String name, Map<String, String> config) {
        KafkaProducer<K, V> ret = KafkaProducer.newInstance(
            io.vertx.kafka.client.producer.KafkaProducer.createShared(vertx.getDelegate(), name, config),
            TypeArg.unknown(),
            TypeArg.unknown()
        );
        return ret;
    }

    /**
     * Get or create a KafkaProducer instance which shares its stream with any other KafkaProducer created with the same <code>name</code>
     * @param vertx Vert.x instance to use
     * @param name the producer name to identify it
     * @param options Kafka producer options
     * @return an instance of the KafkaProducer
     */
    public static <K, V> KafkaProducer<K, V> createShared(
        io.vertx.reactivex.core.Vertx vertx,
        String name,
        io.vertx.kafka.client.common.KafkaClientOptions options
    ) {
        KafkaProducer<K, V> ret = KafkaProducer.newInstance(
            io.vertx.kafka.client.producer.KafkaProducer.createShared(vertx.getDelegate(), name, options),
            TypeArg.unknown(),
            TypeArg.unknown()
        );
        return ret;
    }

    /**
     * Get or create a KafkaProducer instance which shares its stream with any other KafkaProducer created with the same <code>name</code>
     * @param vertx Vert.x instance to use
     * @param name the producer name to identify it
     * @param config Kafka producer configuration
     * @param keyType class type for the key serialization
     * @param valueType class type for the value serialization
     * @return an instance of the KafkaProducer
     */
    public static <K, V> KafkaProducer<K, V> createShared(
        io.vertx.reactivex.core.Vertx vertx,
        String name,
        Map<String, String> config,
        Class<K> keyType,
        Class<V> valueType
    ) {
        KafkaProducer<K, V> ret = KafkaProducer.newInstance(
            io.vertx.kafka.client.producer.KafkaProducer.createShared(
                vertx.getDelegate(),
                name,
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
     * Get or create a KafkaProducer instance which shares its stream with any other KafkaProducer created with the same <code>name</code>
     * @param vertx Vert.x instance to use
     * @param name the producer name to identify it
     * @param options Kafka producer options
     * @param keyType class type for the key serialization
     * @param valueType class type for the value serialization
     * @return an instance of the KafkaProducer
     */
    public static <K, V> KafkaProducer<K, V> createShared(
        io.vertx.reactivex.core.Vertx vertx,
        String name,
        io.vertx.kafka.client.common.KafkaClientOptions options,
        Class<K> keyType,
        Class<V> valueType
    ) {
        KafkaProducer<K, V> ret = KafkaProducer.newInstance(
            io.vertx.kafka.client.producer.KafkaProducer.createShared(
                vertx.getDelegate(),
                name,
                options,
                io.vertx.lang.reactivex.Helper.unwrap(keyType),
                io.vertx.lang.reactivex.Helper.unwrap(valueType)
            ),
            TypeArg.of(keyType),
            TypeArg.of(valueType)
        );
        return ret;
    }

    /**
     * Create a new KafkaProducer instance
     * @param vertx Vert.x instance to use
     * @param config Kafka producer configuration
     * @return an instance of the KafkaProducer
     */
    public static <K, V> KafkaProducer<K, V> create(io.vertx.reactivex.core.Vertx vertx, Map<String, String> config) {
        KafkaProducer<K, V> ret = KafkaProducer.newInstance(
            io.vertx.kafka.client.producer.KafkaProducer.create(vertx.getDelegate(), config),
            TypeArg.unknown(),
            TypeArg.unknown()
        );
        return ret;
    }

    /**
     * Create a new KafkaProducer instance
     * @param vertx Vert.x instance to use
     * @param config Kafka producer configuration
     * @param keyType class type for the key serialization
     * @param valueType class type for the value serialization
     * @return an instance of the KafkaProducer
     */
    public static <K, V> KafkaProducer<K, V> create(
        io.vertx.reactivex.core.Vertx vertx,
        Map<String, String> config,
        Class<K> keyType,
        Class<V> valueType
    ) {
        KafkaProducer<K, V> ret = KafkaProducer.newInstance(
            io.vertx.kafka.client.producer.KafkaProducer.create(
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

    public static <K, V> KafkaProducer<K, V> newInstance(io.vertx.kafka.client.producer.KafkaProducer arg) {
        return arg != null ? new KafkaProducer<K, V>(arg) : null;
    }

    public static <K, V> KafkaProducer<K, V> newInstance(
        io.vertx.kafka.client.producer.KafkaProducer arg,
        TypeArg<K> __typeArg_K,
        TypeArg<V> __typeArg_V
    ) {
        return arg != null ? new KafkaProducer<K, V>(arg, __typeArg_K, __typeArg_V) : null;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaProducer that = (KafkaProducer) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    public io.vertx.kafka.client.producer.KafkaProducer getDelegate() {
        return delegate;
    }

    public synchronized WriteStreamObserver<KafkaProducerRecord<K, V>> toObserver() {
        if (observer == null) {
            Function<KafkaProducerRecord, io.vertx.kafka.client.producer.KafkaProducerRecord<K, V>> conv =
                KafkaProducerRecord<K, V>::getDelegate;
            observer = RxHelper.toObserver(getDelegate(), conv);
        }
        return observer;
    }

    public synchronized WriteStreamSubscriber<KafkaProducerRecord<K, V>> toSubscriber() {
        if (subscriber == null) {
            Function<KafkaProducerRecord, io.vertx.kafka.client.producer.KafkaProducerRecord<K, V>> conv =
                KafkaProducerRecord<K, V>::getDelegate;
            subscriber = RxHelper.toSubscriber(getDelegate(), conv);
        }
        return subscriber;
    }

    /**
     * Same as  but with an <code>handler</code> called when the operation completes
     * @param data
     * @param handler
     */
    public void write(KafkaProducerRecord<K, V> data, Handler<AsyncResult<Void>> handler) {
        delegate.write(data.getDelegate(), handler);
    }

    /**
     * Same as  but with an <code>handler</code> called when the operation completes
     * @param data
     */
    public void write(KafkaProducerRecord<K, V> data) {
        write(data, ar -> {});
    }

    /**
     * Same as  but with an <code>handler</code> called when the operation completes
     * @param data
     * @return
     */
    public io.reactivex.Completable rxWrite(KafkaProducerRecord<K, V> data) {
        return AsyncResultCompletable.toCompletable($handler -> {
            write(data, $handler);
        });
    }

    /**
     * Same as {@link io.vertx.reactivex.core.streams.WriteStream#end} but with an <code>handler</code> called when the operation completes
     * @param handler
     */
    public void end(Handler<AsyncResult<Void>> handler) {
        delegate.end(handler);
    }

    /**
     * Same as {@link io.vertx.reactivex.core.streams.WriteStream#end} but with an <code>handler</code> called when the operation completes
     */
    public void end() {
        end(ar -> {});
    }

    /**
     * Same as {@link io.vertx.reactivex.core.streams.WriteStream#end} but with an <code>handler</code> called when the operation completes
     * @return
     */
    public io.reactivex.Completable rxEnd() {
        return AsyncResultCompletable.toCompletable($handler -> {
            end($handler);
        });
    }

    /**
     * Same as  but with an <code>handler</code> called when the operation completes
     * @param data
     * @param handler
     */
    public void end(KafkaProducerRecord<K, V> data, Handler<AsyncResult<Void>> handler) {
        delegate.end(data.getDelegate(), handler);
    }

    /**
     * Same as  but with an <code>handler</code> called when the operation completes
     * @param data
     */
    public void end(KafkaProducerRecord<K, V> data) {
        end(data, ar -> {});
    }

    /**
     * Same as  but with an <code>handler</code> called when the operation completes
     * @param data
     * @return
     */
    public io.reactivex.Completable rxEnd(KafkaProducerRecord<K, V> data) {
        return AsyncResultCompletable.toCompletable($handler -> {
            end(data, $handler);
        });
    }

    /**
     * This will return <code>true</code> if there are more bytes in the write queue than the value set using {@link KafkaProducer#setWriteQueueMaxSize}
     * @return <code>true</code> if write queue is full
     */
    public boolean writeQueueFull() {
        boolean ret = delegate.writeQueueFull();
        return ret;
    }

    /**
     * Initializes the underlying kafka transactional producer. See {@link KafkaProducer#initTransactions} ()}
     * @param handler handler called on operation completed
     * @return current KafkaWriteStream instance
     */
    public KafkaProducer<K, V> initTransactions(Handler<AsyncResult<Void>> handler) {
        delegate.initTransactions(handler);
        return this;
    }

    /**
     * Initializes the underlying kafka transactional producer. See {@link KafkaProducer#initTransactions} ()}
     * @return current KafkaWriteStream instance
     */
    public KafkaProducer<K, V> initTransactions() {
        return initTransactions(ar -> {});
    }

    /**
     * Initializes the underlying kafka transactional producer. See {@link KafkaProducer#initTransactions} ()}
     * @return current KafkaWriteStream instance
     */
    public io.reactivex.Completable rxInitTransactions() {
        return AsyncResultCompletable.toCompletable($handler -> {
            initTransactions($handler);
        });
    }

    /**
     * Starts a new kafka transaction. See {@link KafkaProducer#beginTransaction}
     * @param handler handler called on operation completed
     * @return current KafkaWriteStream instance
     */
    public KafkaProducer<K, V> beginTransaction(Handler<AsyncResult<Void>> handler) {
        delegate.beginTransaction(handler);
        return this;
    }

    /**
     * Starts a new kafka transaction. See {@link KafkaProducer#beginTransaction}
     * @return current KafkaWriteStream instance
     */
    public KafkaProducer<K, V> beginTransaction() {
        return beginTransaction(ar -> {});
    }

    /**
     * Starts a new kafka transaction. See {@link KafkaProducer#beginTransaction}
     * @return current KafkaWriteStream instance
     */
    public io.reactivex.Completable rxBeginTransaction() {
        return AsyncResultCompletable.toCompletable($handler -> {
            beginTransaction($handler);
        });
    }

    /**
     * Commits the ongoing transaction. See {@link KafkaProducer#commitTransaction}
     * @param handler handler called on operation completed
     * @return current KafkaWriteStream instance
     */
    public KafkaProducer<K, V> commitTransaction(Handler<AsyncResult<Void>> handler) {
        delegate.commitTransaction(handler);
        return this;
    }

    /**
     * Commits the ongoing transaction. See {@link KafkaProducer#commitTransaction}
     * @return current KafkaWriteStream instance
     */
    public KafkaProducer<K, V> commitTransaction() {
        return commitTransaction(ar -> {});
    }

    /**
     * Commits the ongoing transaction. See {@link KafkaProducer#commitTransaction}
     * @return current KafkaWriteStream instance
     */
    public io.reactivex.Completable rxCommitTransaction() {
        return AsyncResultCompletable.toCompletable($handler -> {
            commitTransaction($handler);
        });
    }

    /**
     * Aborts the ongoing transaction. See {@link org.apache.kafka.clients.producer.KafkaProducer}
     * @param handler handler called on operation completed
     * @return current KafkaWriteStream instance
     */
    public KafkaProducer<K, V> abortTransaction(Handler<AsyncResult<Void>> handler) {
        delegate.abortTransaction(handler);
        return this;
    }

    /**
     * Aborts the ongoing transaction. See {@link org.apache.kafka.clients.producer.KafkaProducer}
     * @return current KafkaWriteStream instance
     */
    public KafkaProducer<K, V> abortTransaction() {
        return abortTransaction(ar -> {});
    }

    /**
     * Aborts the ongoing transaction. See {@link org.apache.kafka.clients.producer.KafkaProducer}
     * @return current KafkaWriteStream instance
     */
    public io.reactivex.Completable rxAbortTransaction() {
        return AsyncResultCompletable.toCompletable($handler -> {
            abortTransaction($handler);
        });
    }

    public KafkaProducer<K, V> exceptionHandler(Handler<Throwable> handler) {
        delegate.exceptionHandler(handler);
        return this;
    }

    public KafkaProducer<K, V> setWriteQueueMaxSize(int i) {
        delegate.setWriteQueueMaxSize(i);
        return this;
    }

    public KafkaProducer<K, V> drainHandler(Handler<Void> handler) {
        delegate.drainHandler(handler);
        return this;
    }

    /**
     * Asynchronously write a record to a topic
     * @param record record to write
     * @param handler handler called on operation completed
     * @return current KafkaWriteStream instance
     */
    public KafkaProducer<K, V> send(
        KafkaProducerRecord<K, V> record,
        Handler<AsyncResult<io.vertx.kafka.client.producer.RecordMetadata>> handler
    ) {
        delegate.send(record.getDelegate(), handler);
        return this;
    }

    /**
     * Asynchronously write a record to a topic
     * @param record record to write
     * @return current KafkaWriteStream instance
     */
    public KafkaProducer<K, V> send(KafkaProducerRecord<K, V> record) {
        return send(record, ar -> {});
    }

    /**
     * Asynchronously write a record to a topic
     * @param record record to write
     * @return current KafkaWriteStream instance
     */
    public io.reactivex.Single<io.vertx.kafka.client.producer.RecordMetadata> rxSend(KafkaProducerRecord<K, V> record) {
        return AsyncResultSingle.toSingle($handler -> {
            send(record, $handler);
        });
    }

    /**
     * Get the partition metadata for the give topic.
     * @param topic topic partition for which getting partitions info
     * @param handler handler called on operation completed
     * @return current KafkaProducer instance
     */
    public KafkaProducer<K, V> partitionsFor(String topic, Handler<AsyncResult<List<io.vertx.kafka.client.common.PartitionInfo>>> handler) {
        delegate.partitionsFor(topic, handler);
        return this;
    }

    /**
     * Get the partition metadata for the give topic.
     * @param topic topic partition for which getting partitions info
     * @return current KafkaProducer instance
     */
    public KafkaProducer<K, V> partitionsFor(String topic) {
        return partitionsFor(topic, ar -> {});
    }

    /**
     * Get the partition metadata for the give topic.
     * @param topic topic partition for which getting partitions info
     * @return current KafkaProducer instance
     */
    public io.reactivex.Single<List<io.vertx.kafka.client.common.PartitionInfo>> rxPartitionsFor(String topic) {
        return AsyncResultSingle.toSingle($handler -> {
            partitionsFor(topic, $handler);
        });
    }

    /**
     * Invoking this method makes all buffered records immediately available to write
     * @param completionHandler handler called on operation completed
     * @return current KafkaProducer instance
     */
    public KafkaProducer<K, V> flush(Handler<AsyncResult<Void>> completionHandler) {
        delegate.flush(completionHandler);
        return this;
    }

    /**
     * Invoking this method makes all buffered records immediately available to write
     * @return current KafkaProducer instance
     */
    public KafkaProducer<K, V> flush() {
        return flush(ar -> {});
    }

    /**
     * Invoking this method makes all buffered records immediately available to write
     * @return current KafkaProducer instance
     */
    public io.reactivex.Completable rxFlush() {
        return AsyncResultCompletable.toCompletable($handler -> {
            flush($handler);
        });
    }

    /**
     * Close the producer
     * @param completionHandler handler called on operation completed
     */
    public void close(Handler<AsyncResult<Void>> completionHandler) {
        delegate.close(completionHandler);
    }

    /**
     * Close the producer
     */
    public void close() {
        close(ar -> {});
    }

    /**
     * Close the producer
     * @return
     */
    public io.reactivex.Completable rxClose() {
        return AsyncResultCompletable.toCompletable($handler -> {
            close($handler);
        });
    }

    /**
     * Close the producer
     * @param timeout timeout to wait for closing
     * @param completionHandler handler called on operation completed
     */
    public void close(long timeout, Handler<AsyncResult<Void>> completionHandler) {
        delegate.close(timeout, completionHandler);
    }

    /**
     * Close the producer
     * @param timeout timeout to wait for closing
     */
    public void close(long timeout) {
        close(timeout, ar -> {});
    }

    /**
     * Close the producer
     * @param timeout timeout to wait for closing
     * @return
     */
    public io.reactivex.Completable rxClose(long timeout) {
        return AsyncResultCompletable.toCompletable($handler -> {
            close(timeout, $handler);
        });
    }
}
