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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.logging.Logging;
import io.gravitee.definition.model.v4.analytics.logging.LoggingMode;
import io.gravitee.definition.model.v4.analytics.logging.OtelLogs;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AnalyticsUtilsTest {

    // --- isOtelLogsEnabled ---

    @Test
    void isOtelLogsEnabled_should_return_false_when_analytics_is_null() {
        assertThat(AnalyticsUtils.isOtelLogsEnabled(null)).isFalse();
    }

    @Test
    void isOtelLogsEnabled_should_return_false_when_analytics_disabled() {
        Analytics analytics = new Analytics();
        analytics.setEnabled(false);
        OtelLogs otelLogs = new OtelLogs();
        otelLogs.setEnabled(true);
        analytics.setOtelLogs(otelLogs);

        assertThat(AnalyticsUtils.isOtelLogsEnabled(analytics)).isFalse();
    }

    @Test
    void isOtelLogsEnabled_should_return_false_when_otelLogs_is_null() {
        Analytics analytics = new Analytics();
        analytics.setEnabled(true);
        analytics.setOtelLogs(null);

        assertThat(AnalyticsUtils.isOtelLogsEnabled(analytics)).isFalse();
    }

    @Test
    void isOtelLogsEnabled_should_return_false_when_otelLogs_disabled() {
        Analytics analytics = new Analytics();
        analytics.setEnabled(true);
        OtelLogs otelLogs = new OtelLogs();
        otelLogs.setEnabled(false);
        analytics.setOtelLogs(otelLogs);

        assertThat(AnalyticsUtils.isOtelLogsEnabled(analytics)).isFalse();
    }

    @Test
    void isOtelLogsEnabled_should_return_true_when_analytics_enabled_and_otelLogs_enabled() {
        Analytics analytics = new Analytics();
        analytics.setEnabled(true);
        OtelLogs otelLogs = new OtelLogs();
        otelLogs.setEnabled(true);
        analytics.setOtelLogs(otelLogs);

        assertThat(AnalyticsUtils.isOtelLogsEnabled(analytics)).isTrue();
    }

    // --- isLoggingEnabled: otelLogs path ---

    @Test
    void isLoggingEnabled_should_return_true_when_only_otelLogs_enabled() {
        Analytics analytics = new Analytics();
        analytics.setEnabled(true);
        analytics.setLogging(null); // no ES logging configured
        OtelLogs otelLogs = new OtelLogs();
        otelLogs.setEnabled(true);
        analytics.setOtelLogs(otelLogs);

        assertThat(AnalyticsUtils.isLoggingEnabled(analytics)).isTrue();
    }

    @Test
    void isLoggingEnabled_should_return_false_when_no_logging_and_no_otelLogs() {
        Analytics analytics = new Analytics();
        analytics.setEnabled(true);
        analytics.setLogging(null);
        analytics.setOtelLogs(null);

        assertThat(AnalyticsUtils.isLoggingEnabled(analytics)).isFalse();
    }

    @Test
    void isLoggingEnabled_should_return_true_when_es_logging_enabled() {
        Analytics analytics = new Analytics();
        analytics.setEnabled(true);
        Logging logging = new Logging();
        logging.getMode().setEntrypoint(true);
        analytics.setLogging(logging);

        assertThat(AnalyticsUtils.isLoggingEnabled(analytics)).isTrue();
    }
}
