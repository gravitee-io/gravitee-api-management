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

import io.gravitee.plugin.endpoint.kafka.vertx.client.producer.KafkaHeader;
import io.vertx.lang.rx.RxGen;
import io.vertx.lang.rx.TypeArg;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Generated;

/**
 * Vert.x Kafka consumer record
 *
 * <p/>
 * NOTE: This class has been automatically generated from the {@link io.vertx.kafka.client.consumer.KafkaConsumerRecord original} non RX-ified interface using Vert.x codegen.
 */

@RxGen(io.vertx.kafka.client.consumer.KafkaConsumerRecord.class)
@Generated
public class KafkaConsumerRecord<K, V> {

    public static final TypeArg<KafkaConsumerRecord> __TYPE_ARG = new TypeArg<>(
        obj -> new KafkaConsumerRecord((io.vertx.kafka.client.consumer.KafkaConsumerRecord) obj),
        KafkaConsumerRecord::getDelegate
    );
    private static final TypeArg<KafkaHeader> TYPE_ARG_0 = new TypeArg<KafkaHeader>(
        o1 -> KafkaHeader.newInstance((io.vertx.kafka.client.producer.KafkaHeader) o1),
        o1 -> o1.getDelegate()
    );
    public final TypeArg<K> __typeArg_0;
    public final TypeArg<V> __typeArg_1;
    private final io.vertx.kafka.client.consumer.KafkaConsumerRecord<K, V> delegate;

    public KafkaConsumerRecord(io.vertx.kafka.client.consumer.KafkaConsumerRecord delegate) {
        this.delegate = delegate;
        this.__typeArg_0 = TypeArg.unknown();
        this.__typeArg_1 = TypeArg.unknown();
    }

    public KafkaConsumerRecord(Object delegate, TypeArg<K> typeArg_0, TypeArg<V> typeArg_1) {
        this.delegate = (io.vertx.kafka.client.consumer.KafkaConsumerRecord) delegate;
        this.__typeArg_0 = typeArg_0;
        this.__typeArg_1 = typeArg_1;
    }

    public static <K, V> KafkaConsumerRecord<K, V> newInstance(io.vertx.kafka.client.consumer.KafkaConsumerRecord arg) {
        return arg != null ? new KafkaConsumerRecord<K, V>(arg) : null;
    }

    public static <K, V> KafkaConsumerRecord<K, V> newInstance(
        io.vertx.kafka.client.consumer.KafkaConsumerRecord arg,
        TypeArg<K> __typeArg_K,
        TypeArg<V> __typeArg_V
    ) {
        return arg != null ? new KafkaConsumerRecord<K, V>(arg, __typeArg_K, __typeArg_V) : null;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaConsumerRecord that = (KafkaConsumerRecord) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    public io.vertx.kafka.client.consumer.KafkaConsumerRecord getDelegate() {
        return delegate;
    }

    /**
     * @return the topic this record is received from
     */
    public String topic() {
        String ret = delegate.topic();
        return ret;
    }

    /**
     * @return the partition from which this record is received
     */
    public int partition() {
        int ret = delegate.partition();
        return ret;
    }

    /**
     * @return the position of this record in the corresponding Kafka partition.
     */
    public long offset() {
        long ret = delegate.offset();
        return ret;
    }

    /**
     * @return the timestamp of this record
     */
    public long timestamp() {
        long ret = delegate.timestamp();
        return ret;
    }

    /**
     * @return the timestamp type of this record
     */
    public org.apache.kafka.common.record.TimestampType timestampType() {
        org.apache.kafka.common.record.TimestampType ret = delegate.timestampType();
        return ret;
    }

    /**
     * @return the checksum (CRC32) of the record.
     */
    @Deprecated
    public long checksum() {
        long ret = delegate.checksum();
        return ret;
    }

    /**
     * @return the key (or null if no key is specified)
     */
    public K key() {
        K ret = __typeArg_0.wrap(delegate.key());
        return ret;
    }

    /**
     * @return the value
     */
    public V value() {
        V ret = __typeArg_1.wrap(delegate.value());
        return ret;
    }

    /**
     * @return the list of consumer record headers
     */
    public List<KafkaHeader> headers() {
        List<KafkaHeader> ret = delegate.headers().stream().map(elt -> KafkaHeader.newInstance(elt)).collect(Collectors.toList());
        return ret;
    }
}
