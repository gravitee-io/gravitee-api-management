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
package io.gravitee.gateway.reactive.core.v4.analytics;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.gateway.opentelemetry.TracingContext;
import io.gravitee.node.api.configuration.Configuration;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class AnalyticsContextTest {

    @Test
    void should_not_be_enabled_if_analytics_is_null() {
        AnalyticsContext analyticsContext = new AnalyticsContext(null, "100MB", null, TracingContext.noop());
        assertThat(analyticsContext.isEnabled()).isFalse();
    }

    @Test
    void should_be_enabled_if_analytics_is_enabled() {
        Analytics analytics = new Analytics();
        analytics.setEnabled(true);
        AnalyticsContext analyticsContext = new AnalyticsContext(analytics, "100MB", null, TracingContext.noop());
        assertThat(analyticsContext.isEnabled()).isTrue();
    }
}
