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

import java.time.Duration;
import java.time.LocalDateTime;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.support.CronExpression;

public final class CronScheduleLimits {

    private static final LocalDateTime REFERENCE_TIME = LocalDateTime.of(2026, 1, 1, 0, 0);

    private CronScheduleLimits() {}

    public static String limitFrequency(String userCron, String cronLimit) {
        if (StringUtils.isBlank(cronLimit)) {
            return userCron;
        }

        return frequency(userCron).compareTo(frequency(cronLimit)) < 0 ? cronLimit : userCron;
    }

    public static long limitFrequency(long userDelayMillis, long delayLimitMillis) {
        return delayLimitMillis > 0 ? Math.max(userDelayMillis, delayLimitMillis) : userDelayMillis;
    }

    private static Duration frequency(String cron) {
        var cronExpression = CronExpression.parse(cron);
        var firstExecution = cronExpression.next(REFERENCE_TIME);
        if (firstExecution == null) {
            throw new IllegalArgumentException("Cron expression has no future execution: " + cron);
        }
        var secondExecution = cronExpression.next(firstExecution);

        if (secondExecution == null) {
            throw new IllegalArgumentException("Cron expression has no future execution: " + cron);
        }

        return Duration.between(firstExecution, secondExecution);
    }
}
