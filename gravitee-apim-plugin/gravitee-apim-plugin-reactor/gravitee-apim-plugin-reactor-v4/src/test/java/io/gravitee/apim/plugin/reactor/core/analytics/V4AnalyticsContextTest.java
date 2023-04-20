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
package io.gravitee.apim.plugin.reactor.core.analytics;

import static org.mockito.Mockito.when;

import io.gravitee.apim.plugin.reactor.core.analytics.sampling.CountMessageSamplingStrategy;
import io.gravitee.apim.plugin.reactor.core.analytics.sampling.MessageSamplingStrategy;
import io.gravitee.apim.plugin.reactor.core.analytics.sampling.ProbabilityMessageStrategy;
import io.gravitee.apim.plugin.reactor.core.analytics.sampling.TemporalMessageSamplingStrategy;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.sampling.Sampling;
import io.gravitee.definition.model.v4.analytics.sampling.SamplingType;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class V4AnalyticsContextTest {

    @Mock
    private Analytics analytics;

    private V4AnalyticsContext cut;

    @BeforeEach
    void setUp() {
        when(analytics.isEnabled()).thenReturn(true);
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    void should_choose_right_sampling(SamplingType samplingType, Class<? extends MessageSamplingStrategy> samplingStrategyClass) {
        when(analytics.isEnabled()).thenReturn(true);
        final Sampling sampling = new Sampling();
        sampling.setValue("value");
        sampling.setType(samplingType);
        when(analytics.getMessageSampling()).thenReturn(sampling);
        cut = new V4AnalyticsContext(analytics, true, "", "");
        Assertions.assertThat(cut.getMessageSamplingStrategy()).isInstanceOf(samplingStrategyClass);
    }

    @Test
    void should_choose_probability_sampling_when_nothing_defined() {
        when(analytics.isEnabled()).thenReturn(true);
        when(analytics.getMessageSampling()).thenReturn(null);
        cut = new V4AnalyticsContext(analytics, true, "", "");
        Assertions.assertThat(cut.getMessageSamplingStrategy()).isInstanceOf(ProbabilityMessageStrategy.class);
    }

    private static Stream<Arguments> provideParameters() {
        return Stream.of(
            Arguments.of(SamplingType.PROBABILITY, ProbabilityMessageStrategy.class),
            Arguments.of(SamplingType.TEMPORAL, TemporalMessageSamplingStrategy.class),
            Arguments.of(SamplingType.COUNT, CountMessageSamplingStrategy.class)
        );
    }
}
