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

import io.gravitee.definition.model.v4.analytics.sampling.Sampling;
import io.gravitee.definition.model.v4.analytics.sampling.SamplingType;
import io.gravitee.gateway.reactive.api.message.DefaultMessage;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class TemporalMessageSamplingStrategyTest {

    @Test
    void should_set_min_duration_when_sampling_value_is_higher_than_max() {
        // Given
        TemporalMessageSamplingStrategy temporalMessageSamplingStrategy = new TemporalMessageSamplingStrategy("PT0.5S");

        // Then
        assertThat(temporalMessageSamplingStrategy.getPeriodInMs()).isEqualTo(Duration.ofSeconds(1).toMillis());
    }

    @ParameterizedTest(name = "{index} => samplingValue={0}")
    @ValueSource(strings = { "bad_value" })
    @NullAndEmptySource
    void should_set_default_duration_when_sampling_value_is_wrong(final String samplingValue) {
        // Given
        TemporalMessageSamplingStrategy temporalMessageSamplingStrategy = new TemporalMessageSamplingStrategy(samplingValue);

        // Then
        assertThat(temporalMessageSamplingStrategy.getPeriodInMs()).isEqualTo(Duration.ofSeconds(10).toMillis());
    }

    @ParameterizedTest(name = "{index} => samplingValue={0}")
    @ValueSource(strings = { "bad_value" })
    @NullAndEmptySource
    void should_be_recordable_when_message_comes_after_default_delay_with_wrong_sampling_value(final String samplingValue) {
        // Given
        TemporalMessageSamplingStrategy temporalMessageSamplingStrategy = new TemporalMessageSamplingStrategy(samplingValue);
        long lastMessageTimestamp = 1674121270000L;
        long messageTimestamp = lastMessageTimestamp + Duration.of(5, ChronoUnit.MINUTES).toMillis();
        // When
        boolean recordable = temporalMessageSamplingStrategy.isRecordable(
            DefaultMessage.builder().timestamp(messageTimestamp).build(),
            -1,
            lastMessageTimestamp
        );

        // Then
        assertThat(recordable).isTrue();
    }

    @ParameterizedTest(name = "{index} => samplingValue={0}, messageTimestamp={1}, lastTimestamp={2}")
    @CsvSource({ "PT1S, 1674121271000, 1674121270000", "PT2S, 1674121272000, 1674121270000", "PT5S, 1674121275000, 1674121270000" })
    void should_be_recordable_when_message_comes_after_delay(
        final String samplingValue,
        final long messageTimestamp,
        final long lastTimestamp
    ) {
        // Given
        TemporalMessageSamplingStrategy temporalMessageSamplingStrategy = new TemporalMessageSamplingStrategy(samplingValue);

        // When
        boolean recordable = temporalMessageSamplingStrategy.isRecordable(
            DefaultMessage.builder().timestamp(messageTimestamp).build(),
            -1,
            lastTimestamp
        );

        // Then
        assertThat(recordable).isTrue();
    }

    @ParameterizedTest(name = "{index} => samplingValue={0}, messageTimestamp={1}, lastTimestamp={2}")
    @CsvSource({ "PT1S, 1674121270500, 1674121270000", "PT2S, 1674121271500, 1674121270000", "PT5S, 1674121274000, 1674121270000" })
    void should_not_be_recordable_when_message_comes_before_delay(
        final String samplingValue,
        final long messageTimestamp,
        final long lastTimestamp
    ) {
        // Given
        TemporalMessageSamplingStrategy temporalMessageSamplingStrategy = new TemporalMessageSamplingStrategy(samplingValue);

        // When
        boolean recordable = temporalMessageSamplingStrategy.isRecordable(
            DefaultMessage.builder().timestamp(messageTimestamp).build(),
            -1,
            lastTimestamp
        );

        // Then
        assertThat(recordable).isFalse();
    }

    @Test
    void should_be_recordable_when_first_message() {
        // Given
        TemporalMessageSamplingStrategy temporalMessageSamplingStrategy = new TemporalMessageSamplingStrategy(null);

        // When
        boolean recordable = temporalMessageSamplingStrategy.isRecordable(DefaultMessage.builder().build(), 1, System.currentTimeMillis());

        // Then
        assertThat(recordable).isTrue();
    }

    @Test
    void should_be_recordable_when_last_timestamp_undefined() {
        // Given
        TemporalMessageSamplingStrategy temporalMessageSamplingStrategy = new TemporalMessageSamplingStrategy(null);

        // When
        boolean recordable = temporalMessageSamplingStrategy.isRecordable(DefaultMessage.builder().build(), 46587, -1);

        // Then
        assertThat(recordable).isTrue();
    }
}
