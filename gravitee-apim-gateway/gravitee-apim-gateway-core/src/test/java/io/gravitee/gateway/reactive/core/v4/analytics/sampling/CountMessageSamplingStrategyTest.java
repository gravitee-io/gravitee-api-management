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
package io.gravitee.gateway.reactive.core.v4.analytics.sampling;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import io.gravitee.definition.model.v4.analytics.sampling.Sampling;
import io.gravitee.definition.model.v4.analytics.sampling.SamplingType;
import io.gravitee.gateway.reactive.api.message.DefaultMessage;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
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
class CountMessageSamplingStrategyTest {

    @Test
    void should_set_min_count_when_sampling_value_is_lower_than_max() {
        // Given
        CountMessageSamplingStrategy countMessageSamplingStrategy = new CountMessageSamplingStrategy("1");

        // Then
        assertThat(countMessageSamplingStrategy.getCount()).isEqualTo(10);
    }

    @ParameterizedTest(name = "{index} => samplingValue={0}")
    @ValueSource(strings = { "bad_value" })
    @NullAndEmptySource
    void should_set_default_count_when_sampling_value_is_wrong(final String samplingValue) {
        // Given
        CountMessageSamplingStrategy countMessageSamplingStrategy = new CountMessageSamplingStrategy(samplingValue);

        // Then
        assertThat(countMessageSamplingStrategy.getCount()).isEqualTo(100);
    }

    @ParameterizedTest(name = "{index} => samplingValue={0}")
    @ValueSource(strings = { "bad_value" })
    @NullAndEmptySource
    void should_be_recordable_when_message_count_matches_default_value_with_wrong_sampling_value(final String samplingValue) {
        // Given
        CountMessageSamplingStrategy countMessageSamplingStrategy = new CountMessageSamplingStrategy(samplingValue);

        // When
        boolean recordable = countMessageSamplingStrategy.isRecordable(DefaultMessage.builder().build(), 1, -1);

        // Then
        assertThat(recordable).isTrue();
    }

    @ParameterizedTest(name = "{index} => samplingValue={0}, count={1}")
    @CsvSource({ "100, 1", "100, 101", "100, 201" })
    void should_be_recordable(final String samplingValue, final int count) {
        // Given
        CountMessageSamplingStrategy countMessageSamplingStrategy = new CountMessageSamplingStrategy(samplingValue);

        // When
        boolean recordable = countMessageSamplingStrategy.isRecordable(DefaultMessage.builder().build(), count, -1);

        // Then
        assertThat(recordable).isTrue();
    }

    @ParameterizedTest(name = "{index} => samplingValue={0}, count={1}")
    @CsvSource({ "100, 2", "100, 102", "100, 202" })
    void should_not_be_recordable(final String samplingValue, final int count) {
        // Given
        CountMessageSamplingStrategy countMessageSamplingStrategy = new CountMessageSamplingStrategy(samplingValue);

        // When
        boolean recordable = countMessageSamplingStrategy.isRecordable(DefaultMessage.builder().build(), count, -1);

        // Then
        assertThat(recordable).isFalse();
    }

    @Test
    void should_be_recordable_when_first_message() {
        // Given
        CountMessageSamplingStrategy countMessageSamplingStrategy = new CountMessageSamplingStrategy(null);

        // When
        boolean recordable = countMessageSamplingStrategy.isRecordable(DefaultMessage.builder().build(), 1, -1);

        // Then
        assertThat(recordable).isTrue();
    }
}
