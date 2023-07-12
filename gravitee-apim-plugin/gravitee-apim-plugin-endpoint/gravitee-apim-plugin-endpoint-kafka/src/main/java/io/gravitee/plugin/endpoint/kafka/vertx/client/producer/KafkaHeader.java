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
import lombok.Generated;

/**
 * Vert.x Kafka producer record header.
 *
 * <p/>
 * NOTE: This class has been automatically generated from the {@link io.vertx.kafka.client.producer.KafkaHeader original} non RX-ified interface using Vert.x codegen.
 */

@RxGen(io.vertx.kafka.client.producer.KafkaHeader.class)
@Generated
public class KafkaHeader {

    public static final TypeArg<KafkaHeader> __TYPE_ARG = new TypeArg<>(
        obj -> new KafkaHeader((io.vertx.kafka.client.producer.KafkaHeader) obj),
        KafkaHeader::getDelegate
    );
    private final io.vertx.kafka.client.producer.KafkaHeader delegate;
    private String cached_0;
    private io.vertx.reactivex.core.buffer.Buffer cached_1;

    public KafkaHeader(io.vertx.kafka.client.producer.KafkaHeader delegate) {
        this.delegate = delegate;
    }

    public KafkaHeader(Object delegate) {
        this.delegate = (io.vertx.kafka.client.producer.KafkaHeader) delegate;
    }

    public static KafkaHeader header(String key, io.vertx.reactivex.core.buffer.Buffer value) {
        KafkaHeader ret = KafkaHeader.newInstance(io.vertx.kafka.client.producer.KafkaHeader.header(key, value.getDelegate()));
        return ret;
    }

    public static KafkaHeader header(String key, String value) {
        KafkaHeader ret = KafkaHeader.newInstance(io.vertx.kafka.client.producer.KafkaHeader.header(key, value));
        return ret;
    }

    public static KafkaHeader newInstance(io.vertx.kafka.client.producer.KafkaHeader arg) {
        return arg != null ? new KafkaHeader(arg) : null;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaHeader that = (KafkaHeader) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    public io.vertx.kafka.client.producer.KafkaHeader getDelegate() {
        return delegate;
    }

    /**
     * @return the buffer key
     */
    public String key() {
        if (cached_0 != null) {
            return cached_0;
        }
        String ret = delegate.key();
        cached_0 = ret;
        return ret;
    }

    /**
     * @return the buffer value
     */
    public io.vertx.reactivex.core.buffer.Buffer value() {
        if (cached_1 != null) {
            return cached_1;
        }
        io.vertx.reactivex.core.buffer.Buffer ret = io.vertx.reactivex.core.buffer.Buffer.newInstance(delegate.value());
        cached_1 = ret;
        return ret;
    }
}
