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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class KafkaReceiverFactoryTest {

    private KafkaReceiverFactory kafkaReceiverFactory;

    @BeforeEach
    public void beforeEach() {
        kafkaReceiverFactory = new KafkaReceiverFactory();
    }

    @Test
    void shouldReturnSameInstanceWithSameOptions() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        ReceiverOptions<String, byte[]> receiverOptions = ReceiverOptions.<String, byte[]>create(config).subscription(Set.of("test"));

        KafkaReceiver<String, byte[]> receiver1 = kafkaReceiverFactory.createReceiver(receiverOptions);
        KafkaReceiver<String, byte[]> receiver2 = kafkaReceiverFactory.createReceiver(receiverOptions);
        assertThat(receiver1).isSameAs(receiver2);
    }

    @Test
    void shouldReturnDifferentInstanceWithDifferentOptions() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        ReceiverOptions<String, byte[]> receiverOptions1 = ReceiverOptions.<String, byte[]>create(config).subscription(Set.of("test"));
        ReceiverOptions<String, byte[]> receiverOptions2 = ReceiverOptions.<String, byte[]>create(config).subscription(Set.of("test2"));

        KafkaReceiver<String, byte[]> receiver1 = kafkaReceiverFactory.createReceiver(receiverOptions1);
        KafkaReceiver<String, byte[]> receiver2 = kafkaReceiverFactory.createReceiver(receiverOptions2);
        assertThat(receiver1).isNotSameAs(receiver2).isNotEqualTo(receiver2);
    }

    @Test
    void shouldReturnNewInstanceAfterClear() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        ReceiverOptions<String, byte[]> receiverOptions = ReceiverOptions.<String, byte[]>create(config).subscription(Set.of("test"));

        KafkaReceiver<String, byte[]> receiver1 = kafkaReceiverFactory.createReceiver(receiverOptions);
        kafkaReceiverFactory.clear();
        KafkaReceiver<String, byte[]> receiver2 = kafkaReceiverFactory.createReceiver(receiverOptions);
        assertThat(receiver1).isNotSameAs(receiver2).isNotEqualTo(receiver2);
    }
}
