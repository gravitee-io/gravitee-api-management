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

import io.vertx.lang.rx.RxGen;
import io.vertx.lang.rx.TypeArg;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Generated;

/**
 * Vert.x Kafka producer record.
 *
 * <p/>
 * NOTE: This class has been automatically generated from the {@link io.vertx.kafka.client.producer.KafkaProducerRecord original} non RX-ified interface using Vert.x codegen.
 */

@RxGen(io.vertx.kafka.client.producer.KafkaProducerRecord.class)
@Generated
public class KafkaProducerRecord<K, V> {

    public static final TypeArg<KafkaProducerRecord> __TYPE_ARG = new TypeArg<>(
        obj -> new KafkaProducerRecord((io.vertx.kafka.client.producer.KafkaProducerRecord) obj),
        KafkaProducerRecord::getDelegate
    );
    private static final TypeArg<KafkaHeader> TYPE_ARG_0 = new TypeArg<KafkaHeader>(
        o1 -> KafkaHeader.newInstance((io.vertx.kafka.client.producer.KafkaHeader) o1),
        o1 -> o1.getDelegate()
    );
    public final TypeArg<K> __typeArg_0;
    public final TypeArg<V> __typeArg_1;
    private final io.vertx.kafka.client.producer.KafkaProducerRecord<K, V> delegate;
    private List<KafkaHeader> cached_0;

    public KafkaProducerRecord(io.vertx.kafka.client.producer.KafkaProducerRecord delegate) {
        this.delegate = delegate;
        this.__typeArg_0 = TypeArg.unknown();
        this.__typeArg_1 = TypeArg.unknown();
    }

    public KafkaProducerRecord(Object delegate, TypeArg<K> typeArg_0, TypeArg<V> typeArg_1) {
        this.delegate = (io.vertx.kafka.client.producer.KafkaProducerRecord) delegate;
        this.__typeArg_0 = typeArg_0;
        this.__typeArg_1 = typeArg_1;
    }

    /**
     * Create a concrete instance of a Vert.x producer record
     * @param topic the topic this record is being sent to
     * @param key the key (or null if no key is specified)
     * @param value the value
     * @param timestamp the timestamp of this record
     * @param partition the partition to which the record will be sent (or null if no partition was specified)
     * @return Vert.x producer record
     */
    public static <K, V> KafkaProducerRecord<K, V> create(String topic, K key, V value, Long timestamp, Integer partition) {
        KafkaProducerRecord<K, V> ret = KafkaProducerRecord.newInstance(
            io.vertx.kafka.client.producer.KafkaProducerRecord.create(topic, key, value, timestamp, partition),
            TypeArg.unknown(),
            TypeArg.unknown()
        );
        return ret;
    }

    /**
     * Create a concrete instance of a Vert.x producer record
     * @param topic the topic this record is being sent to
     * @param key the key (or null if no key is specified)
     * @param value the value
     * @return Vert.x producer record
     */
    public static <K, V> KafkaProducerRecord<K, V> create(String topic, K key, V value) {
        KafkaProducerRecord<K, V> ret = KafkaProducerRecord.newInstance(
            io.vertx.kafka.client.producer.KafkaProducerRecord.create(topic, key, value),
            TypeArg.unknown(),
            TypeArg.unknown()
        );
        return ret;
    }

    /**
     * Create a concrete instance of a Vert.x producer record
     * @param topic the topic this record is being sent to
     * @param value the value
     * @return Vert.x producer record
     */
    public static <K, V> KafkaProducerRecord<K, V> create(String topic, V value) {
        KafkaProducerRecord<K, V> ret = KafkaProducerRecord.newInstance(
            io.vertx.kafka.client.producer.KafkaProducerRecord.create(topic, value),
            TypeArg.unknown(),
            TypeArg.unknown()
        );
        return ret;
    }

    public static <K, V> KafkaProducerRecord<K, V> newInstance(io.vertx.kafka.client.producer.KafkaProducerRecord arg) {
        return arg != null ? new KafkaProducerRecord<K, V>(arg) : null;
    }

    public static <K, V> KafkaProducerRecord<K, V> newInstance(
        io.vertx.kafka.client.producer.KafkaProducerRecord arg,
        TypeArg<K> __typeArg_K,
        TypeArg<V> __typeArg_V
    ) {
        return arg != null ? new KafkaProducerRecord<K, V>(arg, __typeArg_K, __typeArg_V) : null;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaProducerRecord that = (KafkaProducerRecord) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    public io.vertx.kafka.client.producer.KafkaProducerRecord getDelegate() {
        return delegate;
    }

    /**
     * @return the topic this record is being sent to
     */
    public String topic() {
        String ret = delegate.topic();
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
     * @return the timestamp of this record
     */
    public Long timestamp() {
        Long ret = delegate.timestamp();
        return ret;
    }

    /**
     * @return the partition to which the record will be sent (or null if no partition was specified)
     */
    public Integer partition() {
        Integer ret = delegate.partition();
        return ret;
    }

    /**
     * Like {@link KafkaProducerRecord#addHeader} but with a key/value pair
     * @param key
     * @param value
     * @return
     */
    public KafkaProducerRecord<K, V> addHeader(String key, String value) {
        delegate.addHeader(key, value);
        return this;
    }

    /**
     * Like {@link KafkaProducerRecord#addHeader} but with a key/value pair
     * @param key
     * @param value
     * @return
     */
    public KafkaProducerRecord<K, V> addHeader(String key, io.vertx.reactivex.core.buffer.Buffer value) {
        delegate.addHeader(key, value.getDelegate());
        return this;
    }

    /**
     * Add an header to this record.
     * @param header the header
     * @return current KafkaProducerRecord instance
     */
    public KafkaProducerRecord<K, V> addHeader(KafkaHeader header) {
        delegate.addHeader(header.getDelegate());
        return this;
    }

    /**
     * Add a list of headers to this record.
     * @param headers the headers
     * @return current KafkaProducerRecord instance
     */
    public KafkaProducerRecord<K, V> addHeaders(List<KafkaHeader> headers) {
        delegate.addHeaders(headers.stream().map(elt -> elt.getDelegate()).collect(Collectors.toList()));
        return this;
    }

    /**
     * @return the headers of this record
     */
    public List<KafkaHeader> headers() {
        if (cached_0 != null) {
            return cached_0;
        }
        List<KafkaHeader> ret = delegate.headers().stream().map(elt -> KafkaHeader.newInstance(elt)).collect(Collectors.toList());
        cached_0 = ret;
        return ret;
    }
}
