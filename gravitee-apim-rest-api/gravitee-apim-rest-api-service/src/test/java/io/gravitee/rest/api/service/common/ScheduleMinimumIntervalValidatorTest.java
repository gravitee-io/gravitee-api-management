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
package io.gravitee.rest.api.service.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.rest.api.service.exceptions.ScheduleMinimumIntervalExceededException;
import io.gravitee.rest.api.service.spring.ScheduleLimitsConfiguration;
import org.junit.jupiter.api.Test;

class ScheduleMinimumIntervalValidatorTest {

    @Test
    void should_validate_each_schedule_type_against_its_configured_limit() {
        var validator = new ScheduleMinimumIntervalValidator(new ScheduleLimitsConfiguration(60_000, 120_000, 180_000, 300_000));

        assertThat(validator.isDynamicPropertiesLimitEnabled()).isTrue();
        assertThat(validator.isHealthcheckLimitEnabled()).isTrue();
        assertThatCode(() -> validator.validateAutoFetch("autoFetch", "0 * * * * *")).doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.validateDynamicProperties("dynamicProperties", "0 * * * * *")).isInstanceOf(
            ScheduleMinimumIntervalExceededException.class
        );
        assertThatThrownBy(() -> validator.validateDictionary("dictionary", 120_000)).isInstanceOf(
            ScheduleMinimumIntervalExceededException.class
        );
        assertThatThrownBy(() -> validator.validateHealthcheck("healthcheck", "0 * * * * *")).isInstanceOf(
            ScheduleMinimumIntervalExceededException.class
        );
    }

    @Test
    void should_preserve_existing_behavior_when_limits_are_disabled() {
        var validator = new ScheduleMinimumIntervalValidator(new ScheduleLimitsConfiguration(0, 0, 0, 0));

        assertThat(validator.isDynamicPropertiesLimitEnabled()).isFalse();
        assertThat(validator.isHealthcheckLimitEnabled()).isFalse();
        assertThatCode(() -> validator.validateAutoFetch("autoFetch", "* * * * * *")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validateDynamicProperties("dynamicProperties", "* * * * * *")).doesNotThrowAnyException();
        assertThatCode(() -> validator.validateDictionary("dictionary", 1)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validateHealthcheck("healthcheck", "* * * * * *")).doesNotThrowAnyException();
    }
}
