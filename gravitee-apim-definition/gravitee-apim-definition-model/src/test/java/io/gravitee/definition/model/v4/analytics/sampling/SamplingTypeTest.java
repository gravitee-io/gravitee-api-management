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
package io.gravitee.definition.model.v4.analytics.sampling;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.util.StringUtils;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class SamplingTypeTest {

    @Nested
    class CountType {

        @Test
        void should_validate_value() {
            assertThat(SamplingType.COUNT.validate("1")).isTrue();
        }

        @ParameterizedTest(name = "{index} => value={0}")
        @ValueSource(strings = { "bad", "-1", "0" })
        @NullAndEmptySource
        void should_not_validate_value(final String value) {
            assertThat(SamplingType.COUNT.validate(value)).isFalse();
        }
    }

    @Nested
    class TemporalType {

        @ParameterizedTest
        @ValueSource(strings = { "PT1S", "1s" })
        void should_validate_value(final String value) {
            assertThat(SamplingType.TEMPORAL.validate(value)).isTrue();
        }

        @ParameterizedTest(name = "{index} => value={0}")
        @ValueSource(strings = { "bad" })
        @NullAndEmptySource
        void should_not_validate_value(final String value) {
            assertThat(SamplingType.TEMPORAL.validate(value)).isFalse();
        }
    }

    @Nested
    class ProbabilityType {

        @Test
        void should_validate_value() {
            assertThat(SamplingType.PROBABILITY.validate("0.2")).isTrue();
        }

        @Test
        void should_not_validate_value() {
            assertThat(SamplingType.PROBABILITY.validate("bad")).isFalse();
        }
    }
}
