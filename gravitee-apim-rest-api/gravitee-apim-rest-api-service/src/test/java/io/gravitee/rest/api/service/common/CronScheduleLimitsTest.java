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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CronScheduleLimitsTest {

    @Test
    void should_ignore_cron_limit_when_no_minimum_interval_is_configured() {
        assertThat(CronScheduleLimits.isMoreFrequentThanLimit("* * * * * *", 0)).isFalse();
    }

    @Test
    void should_detect_regular_cron_more_frequent_than_limit() {
        assertThat(CronScheduleLimits.isMoreFrequentThanLimit("* * * * * *", Duration.ofMinutes(5).toMillis())).isTrue();
    }

    @Test
    void should_accept_cron_equal_to_limit() {
        assertThat(CronScheduleLimits.isMoreFrequentThanLimit("0 */5 * * * *", Duration.ofMinutes(5).toMillis())).isFalse();
    }

    @Test
    void should_detect_clustered_cron_more_frequent_across_day_boundary() {
        assertThat(CronScheduleLimits.isMoreFrequentThanLimit("0 0 1,23 * * *", Duration.ofHours(3).toMillis())).isTrue();
    }

    @Test
    void should_accept_clustered_cron_equal_to_limit_across_day_boundary() {
        assertThat(CronScheduleLimits.isMoreFrequentThanLimit("0 0 1,23 * * *", Duration.ofHours(2).toMillis())).isFalse();
    }

    @Test
    void should_compute_month_end_interval() {
        assertThat(CronScheduleLimits.minimumInterval("0 0 0 L * *")).isEqualTo(Duration.ofDays(28));
    }

    @Test
    void should_compute_leap_day_interval_over_complete_gregorian_cycle() {
        assertThat(CronScheduleLimits.minimumInterval("0 0 0 29 2 *")).isEqualTo(Duration.ofDays(1_461));
    }

    @Test
    void should_compute_nth_weekday_interval() {
        assertThat(CronScheduleLimits.minimumInterval("0 0 0 ? * 2#5")).isEqualTo(Duration.ofDays(63));
    }

    @Test
    void should_support_spring_cron_macros() {
        assertThat(CronScheduleLimits.minimumInterval("@hourly")).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void should_reject_cron_without_execution() {
        assertThatThrownBy(() -> CronScheduleLimits.minimumInterval("0 0 0 31 2 *"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no execution");
    }

    @Test
    void should_limit_delay_when_user_delay_is_shorter() {
        assertThat(CronScheduleLimits.limitFrequency(1_000, 60_000)).isEqualTo(60_000);
    }

    @Test
    void should_detect_delay_more_frequent_than_limit() {
        assertThat(CronScheduleLimits.isMoreFrequentThanLimit(1_000, 60_000)).isTrue();
    }
}
