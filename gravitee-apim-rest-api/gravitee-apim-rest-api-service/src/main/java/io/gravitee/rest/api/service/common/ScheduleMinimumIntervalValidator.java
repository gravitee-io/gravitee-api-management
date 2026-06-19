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

import io.gravitee.rest.api.service.exceptions.ScheduleMinimumIntervalExceededException;
import io.gravitee.rest.api.service.spring.ScheduleLimitsConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleMinimumIntervalValidator {

    private final ScheduleLimitsConfiguration configuration;

    public boolean isDynamicPropertiesLimitEnabled() {
        return configuration.getDynamicPropertiesMinimumInterval() > 0;
    }

    public boolean isHealthcheckLimitEnabled() {
        return configuration.getHealthcheckMinimumInterval() > 0;
    }

    public void validateAutoFetch(String field, String schedule) {
        validate(field, schedule, configuration.getAutoFetchMinimumInterval());
    }

    public void validateDynamicProperties(String field, String schedule) {
        validate(field, schedule, configuration.getDynamicPropertiesMinimumInterval());
    }

    public void validateHealthcheck(String field, String schedule) {
        validate(field, schedule, configuration.getHealthcheckMinimumInterval());
    }

    public void validateDictionary(String field, long delay) {
        var minimumInterval = configuration.getDictionaryMinimumInterval();
        if (CronScheduleLimits.isMoreFrequentThanLimit(delay, minimumInterval)) {
            throw new ScheduleMinimumIntervalExceededException(field, Long.toString(delay), minimumInterval);
        }
    }

    private static void validate(String field, String schedule, long minimumInterval) {
        if (CronScheduleLimits.isMoreFrequentThanLimit(schedule, minimumInterval)) {
            throw new ScheduleMinimumIntervalExceededException(field, schedule, minimumInterval);
        }
    }
}
