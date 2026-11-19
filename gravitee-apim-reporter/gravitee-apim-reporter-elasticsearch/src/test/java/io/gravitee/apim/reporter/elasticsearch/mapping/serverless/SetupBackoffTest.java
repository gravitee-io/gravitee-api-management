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
package io.gravitee.apim.reporter.elasticsearch.mapping.serverless;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SetupBackoffTest {

    @ParameterizedTest(name = "attempt {0} waits {1} s")
    @CsvSource({ "1, 1", "2, 2", "3, 4", "4, 8", "5, 16", "6, 32", "7, 60", "8, 60", "100, 60" })
    void should_double_the_delay_from_one_second_up_to_one_minute(int failedAttempts, long seconds) {
        assertThat(SetupBackoff.delay(failedAttempts)).isEqualTo(Duration.ofSeconds(seconds));
    }
}
