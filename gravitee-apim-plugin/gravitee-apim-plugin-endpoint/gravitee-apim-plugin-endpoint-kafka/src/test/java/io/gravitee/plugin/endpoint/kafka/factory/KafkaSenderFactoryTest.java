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
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class KafkaSenderFactoryTest {

    private KafkaSenderFactory kafkaSenderFactory;

    @BeforeEach
    public void beforeEach() {
        kafkaSenderFactory = new KafkaSenderFactory();
    }

    @Test
    void shouldReturnSameInstanceWithSameOptions() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        SenderOptions<String, byte[]> senderOptions = SenderOptions.create(config);

        KafkaSender<String, byte[]> sender1 = kafkaSenderFactory.createSender(senderOptions);
        KafkaSender<String, byte[]> sender2 = kafkaSenderFactory.createSender(senderOptions);
        assertThat(sender1).isSameAs(sender2);
    }

    @Test
    void shouldReturnDifferentInstanceWithDifferentOptions() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        SenderOptions<String, byte[]> senderOptions1 = SenderOptions.create(config);
        SenderOptions<String, byte[]> senderOptions2 = SenderOptions.<String, byte[]>create(config).maxInFlight(152);

        KafkaSender<String, byte[]> sender1 = kafkaSenderFactory.createSender(senderOptions1);
        KafkaSender<String, byte[]> sender2 = kafkaSenderFactory.createSender(senderOptions2);
        assertThat(sender1).isNotSameAs(sender2).isNotEqualTo(sender2);
    }

    @Test
    void shouldReturnNewInstanceAfterClear() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        SenderOptions<String, byte[]> senderOptions = SenderOptions.create(config);

        KafkaSender<String, byte[]> sender1 = kafkaSenderFactory.createSender(senderOptions);
        kafkaSenderFactory.clear();
        KafkaSender<String, byte[]> sender2 = kafkaSenderFactory.createSender(senderOptions);
        assertThat(sender1).isNotSameAs(sender2).isNotEqualTo(sender2);
    }
}
