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
package io.gravitee.plugin.endpoint.kafka.strategy.impl;

import io.gravitee.plugin.endpoint.kafka.factory.CustomConsumerFactory;
import io.gravitee.plugin.endpoint.kafka.factory.KafkaReceiverFactory;
import io.gravitee.plugin.endpoint.kafka.strategy.QosStrategy;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.internals.DefaultKafkaReceiver;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public abstract class AbstractQosStrategy<K, V> implements QosStrategy<K, V> {

    private final KafkaReceiverFactory kafkaReceiverFactory;
    private DefaultKafkaReceiver<K, V> kafkaReceiver;

    protected KafkaReceiver<K, V> initReceiver(final ReceiverOptions<K, V> options) {
        kafkaReceiver = new DefaultKafkaReceiver<>(CustomConsumerFactory.INSTANCE, options);
        return kafkaReceiver;
    }
}
