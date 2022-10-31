/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.plugin.endpoint.kafka.factory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KafkaReceiverFactory {

    public static final KafkaReceiverFactory INSTANCE = new KafkaReceiverFactory();
    private final Map<Integer, KafkaReceiver<?, ?>> consumers = new ConcurrentHashMap<>();

    public <K, V> KafkaReceiver<K, V> createReceiver(final ReceiverOptions<K, V> receiverOptions) {
        return (KafkaReceiver<K, V>) consumers.computeIfAbsent(
            receiverOptions.hashCode(),
            hashCode -> KafkaReceiver.create(CustomConsumerFactory.INSTANCE, receiverOptions)
        );
    }

    public void clear() {
        consumers.clear();
    }
}
