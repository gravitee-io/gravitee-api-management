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
package io.gravitee.plugin.endpoint.kafka.strategy.impl;

import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.plugin.endpoint.kafka.configuration.KafkaEndpointConnectorSharedConfiguration;
import io.gravitee.plugin.endpoint.kafka.factory.KafkaReceiverFactory;
import java.time.Duration;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.ReceiverOptions;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NoneStrategy<K, V> extends AbstractQosStrategy<K, V> {

    public NoneStrategy(final KafkaReceiverFactory kafkaReceiverFactory) {
        super(kafkaReceiverFactory);
    }

    @Override
    public void addCustomConfig(final Map<String, Object> config) {
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    }

    @Override
    public Flux<ConsumerRecord<K, V>> receive(
        final ExecutionContext executionContext,
        final KafkaEndpointConnectorSharedConfiguration sharedConfiguration,
        final ReceiverOptions<K, V> receiverOptions
    ) {
        ReceiverOptions<K, V> noCommitReceiverOptions = receiverOptions.commitInterval(Duration.ZERO).commitBatchSize(0);
        return initReceiver(noCommitReceiverOptions).receive().map(receiverRecord -> receiverRecord);
    }

    @Override
    public Runnable buildAckRunnable(final ConsumerRecord<K, V> consumerRecord) {
        return doNothing;
    }

    @Override
    public String generateId(final ConsumerRecord<K, V> consumerRecord) {
        return null;
    }
}
