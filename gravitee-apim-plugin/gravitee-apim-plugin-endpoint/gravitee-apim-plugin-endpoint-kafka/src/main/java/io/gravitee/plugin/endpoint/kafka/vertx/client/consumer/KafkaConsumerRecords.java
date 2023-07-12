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

import io.vertx.lang.rx.RxGen;
import io.vertx.lang.rx.TypeArg;
import lombok.Generated;

/**
 * Vert.x Kafka consumer records
 *
 * <p/>
 * NOTE: This class has been automatically generated from the {@link io.vertx.kafka.client.consumer.KafkaConsumerRecords original} non RX-ified interface using Vert.x codegen.
 */

@RxGen(io.vertx.kafka.client.consumer.KafkaConsumerRecords.class)
@Generated
public class KafkaConsumerRecords<K, V> {

    public static final TypeArg<KafkaConsumerRecords> __TYPE_ARG = new TypeArg<>(
        obj -> new KafkaConsumerRecords((io.vertx.kafka.client.consumer.KafkaConsumerRecords) obj),
        KafkaConsumerRecords::getDelegate
    );
    public final TypeArg<K> __typeArg_0;
    public final TypeArg<V> __typeArg_1;
    private final io.vertx.kafka.client.consumer.KafkaConsumerRecords<K, V> delegate;

    public KafkaConsumerRecords(io.vertx.kafka.client.consumer.KafkaConsumerRecords delegate) {
        this.delegate = delegate;
        this.__typeArg_0 = TypeArg.unknown();
        this.__typeArg_1 = TypeArg.unknown();
    }

    public KafkaConsumerRecords(Object delegate, TypeArg<K> typeArg_0, TypeArg<V> typeArg_1) {
        this.delegate = (io.vertx.kafka.client.consumer.KafkaConsumerRecords) delegate;
        this.__typeArg_0 = typeArg_0;
        this.__typeArg_1 = typeArg_1;
    }

    public static <K, V> KafkaConsumerRecords<K, V> newInstance(io.vertx.kafka.client.consumer.KafkaConsumerRecords arg) {
        return arg != null ? new KafkaConsumerRecords<K, V>(arg) : null;
    }

    public static <K, V> KafkaConsumerRecords<K, V> newInstance(
        io.vertx.kafka.client.consumer.KafkaConsumerRecords arg,
        TypeArg<K> __typeArg_K,
        TypeArg<V> __typeArg_V
    ) {
        return arg != null ? new KafkaConsumerRecords<K, V>(arg, __typeArg_K, __typeArg_V) : null;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaConsumerRecords that = (KafkaConsumerRecords) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    public io.vertx.kafka.client.consumer.KafkaConsumerRecords getDelegate() {
        return delegate;
    }

    /**
     * @return the total number of records in this batch
     */
    public int size() {
        int ret = delegate.size();
        return ret;
    }

    /**
     * @return whether this batch contains any records
     */
    public boolean isEmpty() {
        boolean ret = delegate.isEmpty();
        return ret;
    }

    /**
     * Get the record at the given index
     * @param index the index of the record to get
     * @return
     */
    public KafkaConsumerRecord<K, V> recordAt(int index) {
        KafkaConsumerRecord<K, V> ret = KafkaConsumerRecord.newInstance(delegate.recordAt(index), __typeArg_0, __typeArg_1);
        return ret;
    }
}
