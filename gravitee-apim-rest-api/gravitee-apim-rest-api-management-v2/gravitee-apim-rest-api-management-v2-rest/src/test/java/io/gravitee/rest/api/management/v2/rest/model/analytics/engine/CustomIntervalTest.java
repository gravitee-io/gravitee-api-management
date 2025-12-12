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
package io.gravitee.rest.api.management.v2.rest.model.analytics.engine;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class CustomIntervalTest {

    ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    @ParameterizedTest
    @MethodSource("validIntervals")
    void should_parse_valid_interval_values(Object interval, Long expectedMillis) {
        var intervaL = mapper.convertValue(interval, CustomInterval.class);

        assertThat(intervaL.toMillis()).isEqualTo(expectedMillis);
    }

    @ParameterizedTest
    @MethodSource("invalidIntervals")
    void should_not_parse_invalid_interval_values(Object interval, Class<? extends Exception> expectedException) {
        assertThrows(expectedException, () -> {
            var i = mapper.convertValue(interval, CustomInterval.class);
            i.toMillis();
        });
    }

    static Stream<Arguments> validIntervals() {
        return Stream.of(
            arguments(-1, -1L),
            arguments(0, 0L),
            arguments(10, 10L),
            arguments(123456789L, 123456789L),
            arguments("1", 1L),
            arguments("10s", 10000L),
            arguments("1m", 60000L),
            arguments("1h", 3600000L),
            arguments("1d", 86400000L)
        );
    }

    static Stream<Arguments> invalidIntervals() {
        return Stream.of(
            arguments(null, IllegalArgumentException.class),
            arguments("", IllegalArgumentException.class),
            arguments("invalid string", DateTimeParseException.class),
            arguments(true, IllegalArgumentException.class)
        );
    }
}
