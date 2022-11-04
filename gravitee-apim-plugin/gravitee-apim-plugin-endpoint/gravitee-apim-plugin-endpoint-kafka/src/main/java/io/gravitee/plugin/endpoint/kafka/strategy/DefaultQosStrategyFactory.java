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

import io.gravitee.gateway.jupiter.api.qos.QosOptions;
import io.gravitee.plugin.endpoint.kafka.factory.KafkaReceiverFactory;
import io.gravitee.plugin.endpoint.kafka.strategy.impl.BalancedStrategy;
import io.gravitee.plugin.endpoint.kafka.strategy.impl.NoneStrategy;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultQosStrategyFactory implements QosStrategyFactory {

    public <K, V> QosStrategy<K, V> createQosStrategy(final KafkaReceiverFactory kafkaReceiverFactory, final QosOptions qosOptions) {
        if (qosOptions != null && qosOptions.getQos() != null) {
            switch (qosOptions.getQos()) {
                case NONE:
                    return new NoneStrategy(kafkaReceiverFactory);
                case BALANCED:
                    return new BalancedStrategy(kafkaReceiverFactory);
                case AT_BEST:
                case AT_MOST_ONCE:
                case AT_LEAST_ONCE:
                default:
                    throw new IllegalArgumentException(String.format("Unsupported QoS '%s' for Kafka", qosOptions.getQos()));
            }
        }
        throw new IllegalArgumentException("QoS cannot be null");
    }
}
