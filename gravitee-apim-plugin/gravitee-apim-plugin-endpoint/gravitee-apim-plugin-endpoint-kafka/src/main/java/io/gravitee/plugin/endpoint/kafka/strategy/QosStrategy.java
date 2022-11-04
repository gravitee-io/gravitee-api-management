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
package io.gravitee.plugin.endpoint.kafka.strategy;

import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.plugin.endpoint.kafka.configuration.KafkaEndpointConnectorConfiguration;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface QosStrategy<K, V> {
    Runnable doNothing = () -> {};

    default void addCustomConfig(final Map<String, Object> config) {}

    KafkaReceiver<K, V> kafkaReceiver();

    Flux<ConsumerRecord<K, V>> receive(
        final ExecutionContext executionContext,
        final KafkaEndpointConnectorConfiguration configuration,
        final ReceiverOptions<K, V> receiverOptions
    );

    Runnable buildAckRunnable(final ConsumerRecord<K, V> consumerRecord);

    String generateId(final ConsumerRecord<K, V> consumerRecord);
}
