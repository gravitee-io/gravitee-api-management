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
package io.gravitee.rest.api.service.spring;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class ScheduleLimitsConfiguration {

    private final long autoFetchMinimumInterval;
    private final long dynamicPropertiesMinimumInterval;
    private final long dictionaryMinimumInterval;
    private final long healthcheckMinimumInterval;

    public ScheduleLimitsConfiguration(
        @Value("${services.auto_fetch.minimum_interval:0}") long autoFetchMinimumInterval,
        @Value("${services.dynamic_properties.minimum_interval:0}") long dynamicPropertiesMinimumInterval,
        @Value("${services.dictionary.minimum_interval:0}") long dictionaryMinimumInterval,
        @Value("${services.healthcheck.minimum_interval:0}") long healthcheckMinimumInterval
    ) {
        this.autoFetchMinimumInterval = validate("services.auto_fetch.minimum_interval", autoFetchMinimumInterval);
        this.dynamicPropertiesMinimumInterval = validate("services.dynamic_properties.minimum_interval", dynamicPropertiesMinimumInterval);
        this.dictionaryMinimumInterval = validate("services.dictionary.minimum_interval", dictionaryMinimumInterval);
        this.healthcheckMinimumInterval = validate("services.healthcheck.minimum_interval", healthcheckMinimumInterval);
    }

    private static long validate(String property, long value) {
        if (value < 0) {
            throw new IllegalArgumentException(property + " must be greater than or equal to 0");
        }
        return value;
    }
}
