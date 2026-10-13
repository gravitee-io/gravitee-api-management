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
package io.gravitee.apim.rest.api.mcp.core.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.rest.api.mcp.core.TriageException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class IntervalTest {

    @ParameterizedTest
    @CsvSource({ "30s, 30", "5m, 300", "1h, 3600", "1d, 86400" })
    void should_parse_a_duration_string(String interval, long seconds) {
        assertThat(Interval.parse(interval)).isEqualTo(Duration.ofSeconds(seconds));
    }

    @Test
    void should_take_a_number_as_milliseconds() {
        assertThat(Interval.parse(60000)).isEqualTo(Duration.ofMinutes(1));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "5", "5 minutes", "0m", "1w" })
    void should_reject_a_malformed_interval_and_name_the_format(String interval) {
        assertThatThrownBy(() -> Interval.parse(interval))
            .isInstanceOf(TriageException.class)
            .hasMessageContaining("for example 30s or 5m");
    }
}
