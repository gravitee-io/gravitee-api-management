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

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CronScheduleLimitsTest {

    @Test
    void should_keep_user_cron_when_no_limit_is_configured() {
        assertThat(CronScheduleLimits.limitFrequency("* * * * * *", "")).isEqualTo("* * * * * *");
    }

    @Test
    void should_use_limit_when_user_cron_runs_more_frequently() {
        assertThat(CronScheduleLimits.limitFrequency("* * * * * *", "0 */5 * * * *")).isEqualTo("0 */5 * * * *");
    }

    @Test
    void should_keep_user_cron_when_user_cron_runs_less_frequently_than_limit() {
        assertThat(CronScheduleLimits.limitFrequency("0 */10 * * * *", "0 */5 * * * *")).isEqualTo("0 */10 * * * *");
    }

    @Test
    void should_limit_delay_when_user_delay_is_shorter() {
        assertThat(CronScheduleLimits.limitFrequency(1_000, 60_000)).isEqualTo(60_000);
    }

    @Test
    void should_detect_cron_more_frequent_than_limit() {
        assertThat(CronScheduleLimits.isMoreFrequentThanLimit("* * * * * *", "0 */5 * * * *")).isTrue();
    }

    @Test
    void should_detect_delay_more_frequent_than_limit() {
        assertThat(CronScheduleLimits.isMoreFrequentThanLimit(1_000, 60_000)).isTrue();
    }
}
