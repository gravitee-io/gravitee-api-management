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
package io.gravitee.plugin.endpoint.kafka.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.gateway.jupiter.api.qos.Qos;
import io.gravitee.gateway.jupiter.api.qos.QosRequirement;
import io.gravitee.plugin.endpoint.kafka.factory.KafkaReceiverFactory;
import io.gravitee.plugin.endpoint.kafka.strategy.impl.AutoStrategy;
import io.gravitee.plugin.endpoint.kafka.strategy.impl.NoneStrategy;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class DefaultQosStrategyFactoryTest {

    private DefaultQosStrategyFactory cut;

    private static Stream<Arguments> provideQosFactory() {
        return Stream.of(Arguments.of(Qos.NONE, NoneStrategy.class, Qos.AUTO, AutoStrategy.class));
    }

    @BeforeEach
    public void beforeEach() {
        cut = new DefaultQosStrategyFactory();
    }

    @ParameterizedTest
    @MethodSource("provideQosFactory")
    void shouldReturnValidStrategy(Qos qos, final Class<? extends QosStrategy> qosStrategyClass) {
        QosStrategy qosStrategy = cut.createQosStrategy(new KafkaReceiverFactory(), QosRequirement.builder().qos(qos).build());
        assertThat(qosStrategy).isInstanceOf(qosStrategyClass);
    }

    @ParameterizedTest
    @EnumSource(value = Qos.class, names = { "NONE", "AUTO" }, mode = EnumSource.Mode.EXCLUDE)
    void shouldThrowExceptionOnUnsupportedQos() {
        QosRequirement qosOptions = QosRequirement.builder().qos(null).build();
        assertThatThrownBy(() -> cut.createQosStrategy(new KafkaReceiverFactory(), qosOptions))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
