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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ScheduleLimitsConfigurationTest {

    @Test
    void should_accept_disabled_and_positive_limits() {
        var configuration = new ScheduleLimitsConfiguration(0, 1_000, 2_000, 3_000);

        assertThat(configuration.getAutoFetchMinimumInterval()).isZero();
        assertThat(configuration.getDynamicPropertiesMinimumInterval()).isEqualTo(1_000);
        assertThat(configuration.getDictionaryMinimumInterval()).isEqualTo(2_000);
        assertThat(configuration.getHealthcheckMinimumInterval()).isEqualTo(3_000);
    }

    @Test
    void should_reject_negative_limit() {
        assertThatThrownBy(() -> new ScheduleLimitsConfiguration(0, -1, 0, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("services.dynamic_properties.minimum_interval must be greater than or equal to 0");
    }
}
