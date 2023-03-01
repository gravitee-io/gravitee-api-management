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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.*;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class KafkaSenderFactoryTest {

    private KafkaSenderFactory kafkaSenderFactory;

    @BeforeEach
    public void beforeEach() {
        kafkaSenderFactory = new KafkaSenderFactory();
    }

    @Nested
    class CreateSender {

        @Test
        void should_return_same_instance_with_same_options() {
            SenderOptions<String, byte[]> senderOptions = senderOptions();

            KafkaSender<String, byte[]> sender1 = kafkaSenderFactory.createSender(senderOptions);
            KafkaSender<String, byte[]> sender2 = kafkaSenderFactory.createSender(senderOptions);
            assertThat(sender1).isSameAs(sender2);
        }

        @Test
        void should_return_different_instance_with_different_options() {
            SenderOptions<String, byte[]> senderOptions1 = senderOptions();
            SenderOptions<String, byte[]> senderOptions2 = senderOptions();

            KafkaSender<String, byte[]> sender1 = kafkaSenderFactory.createSender(senderOptions1);
            KafkaSender<String, byte[]> sender2 = kafkaSenderFactory.createSender(senderOptions2.maxInFlight(152));
            assertThat(sender1).isNotSameAs(sender2).isNotEqualTo(sender2);
        }
    }

    @Nested
    class Clear {

        @Test
        void should_close_sender_and_remove_it_from_the_pool() {
            SenderOptions<String, byte[]> senderOptions = senderOptions();
            var sender1 = mock(KafkaSender.class);

            kafkaSenderFactory.senders.put(senderOptions.hashCode(), sender1);
            kafkaSenderFactory.clear(sender1);

            verify(sender1).close();
            KafkaSender<String, byte[]> sender2 = kafkaSenderFactory.createSender(senderOptions);
            assertThat(sender1).isNotSameAs(sender2).isNotEqualTo(sender2);
        }

        @Test
        void should_return_new_instance_after_clear() {
            SenderOptions<String, byte[]> senderOptions = senderOptions();

            KafkaSender<String, byte[]> sender1 = kafkaSenderFactory.createSender(senderOptions);
            kafkaSenderFactory.clear();
            KafkaSender<String, byte[]> sender2 = kafkaSenderFactory.createSender(senderOptions);
            assertThat(sender1).isNotSameAs(sender2).isNotEqualTo(sender2);
        }
    }

    private <K, V> SenderOptions<K, V> senderOptions() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        return SenderOptions.create(config);
    }
}
