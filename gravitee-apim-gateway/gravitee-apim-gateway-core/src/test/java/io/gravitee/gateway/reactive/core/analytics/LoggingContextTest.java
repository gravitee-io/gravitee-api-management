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
package io.gravitee.gateway.reactive.core.analytics;

import static org.assertj.core.api.Assertions.*;

import io.gravitee.definition.model.v4.analytics.logging.Logging;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LoggingContextTest {

    private LoggingContext cut;

    @BeforeEach
    void setUp() {
        cut = new LoggingContext(new Logging());
    }

    @ParameterizedTest(name = "Set max size to: {0}, expected result: {1}")
    @MethodSource("provideParameters")
    void should_set_max_size_log_message(String configuredSizeLogMessage, int expectedValue) {
        cut.setMaxSizeLogMessage(configuredSizeLogMessage);
        assertThat(cut.getMaxSizeLogMessage()).isEqualTo(expectedValue);
    }

    private static Stream<Arguments> provideParameters() {
        return Stream.of(
            Arguments.of("10", 10 * 1024 * 1024),
            Arguments.of("10MB", 10 * 1024 * 1024),
            Arguments.of("10M", 10 * 1024 * 1024),
            Arguments.of("10KB", 10 * 1024),
            Arguments.of("10K", 10 * 1024),
            Arguments.of("10B", 10),
            Arguments.of("10TB", -1),
            Arguments.of("FiveHundred", -1)
        );
    }
}
