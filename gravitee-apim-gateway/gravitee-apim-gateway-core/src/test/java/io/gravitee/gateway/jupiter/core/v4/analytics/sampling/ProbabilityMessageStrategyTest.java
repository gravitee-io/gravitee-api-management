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
package io.gravitee.gateway.jupiter.core.v4.analytics.sampling;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ProbabilityMessageStrategyTest {

    @Test
    void should_set_min_probability_when_sampling_value_is_higher_than_max() {
        // Given
        ProbabilityMessageStrategy probabilityMessageStrategy = new ProbabilityMessageStrategy("1");

        // Then
        assertThat(probabilityMessageStrategy.getProbability()).isEqualTo(0.5);
    }

    @ParameterizedTest(name = "{index} => samplingValue={0}")
    @ValueSource(strings = { "bad_value" })
    @NullAndEmptySource
    void should_set_default_probability_when_sampling_value_is_wrong(final String samplingValue) {
        // Given
        ProbabilityMessageStrategy probabilityMessageStrategy = new ProbabilityMessageStrategy(samplingValue);

        // Then
        assertThat(probabilityMessageStrategy.getProbability()).isEqualTo(0.01);
    }

    @ParameterizedTest(name = "{index} => samplingValue={0}")
    @ValueSource(strings = { "bad_value" })
    @NullAndEmptySource
    void should_matches_probability_when_sampling_value_is_wrong(final String samplingValue) {
        // Given
        ProbabilityMessageStrategy probabilityMessageStrategy = new ProbabilityMessageStrategy(samplingValue);

        // When
        boolean matchesProbability = probabilityMessageStrategy.matchesProbability(0.001);

        // Then
        assertThat(matchesProbability).isTrue();
    }

    @ParameterizedTest(name = "{index} => samplingValue={0}, random={1}")
    @CsvSource({ "0.5, 0.1", "0.5, 0.2", "0.5, 0.3", "0.5, 0.4" })
    void should_matches_probability_when_random_is_lower(final String samplingValue, final Double random) {
        // Given
        ProbabilityMessageStrategy probabilityMessageStrategy = new ProbabilityMessageStrategy(samplingValue);

        // When
        boolean matchesProbability = probabilityMessageStrategy.matchesProbability(random);

        // Then
        assertThat(matchesProbability).isTrue();
    }

    @ParameterizedTest(name = "{index} => samplingValue={0}, random={1}")
    @CsvSource({ "0.5, 0.5", "0.5, 0.6", "0.5, 0.7", "0.5, 0.8", "0.5, 0.9", "0.5, 1" })
    void should_not_matches_probability_when_random_is_higher(final String samplingValue, final Double random) {
        // Given
        ProbabilityMessageStrategy probabilityMessageStrategy = new ProbabilityMessageStrategy(samplingValue);

        // When
        boolean matchesProbability = probabilityMessageStrategy.matchesProbability(random);

        // Then
        assertThat(matchesProbability).isFalse();
    }

    @Test
    void should_not_be_recordable_with_0_percent_probability() {
        // Given
        ProbabilityMessageStrategy probabilityMessageStrategy = new ProbabilityMessageStrategy("0");

        // When
        boolean recordable = probabilityMessageStrategy.isRecordable(DefaultMessage.builder().build(), -1, -1);

        // Then
        assertThat(recordable).isFalse();
    }

    @Test
    void should_be_recordable_when_first_message() {
        // Given
        ProbabilityMessageStrategy probabilityMessageStrategy = new ProbabilityMessageStrategy(null);

        // When
        boolean recordable = probabilityMessageStrategy.isRecordable(DefaultMessage.builder().build(), 1, -1);

        // Then
        assertThat(recordable).isTrue();
    }
}
