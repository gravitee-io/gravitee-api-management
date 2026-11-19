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
package io.gravitee.apim.reporter.elasticsearch.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.elasticsearch.utils.Type;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class ServerlessRetentionTest {

    private final ReporterConfiguration configuration = new ReporterConfiguration();

    @ParameterizedTest(name = "{0}")
    @EnumSource(Type.class)
    void should_keep_every_type_45_days_by_default(Type type) {
        assertThat(ServerlessRetention.of(configuration).of(type)).isEqualTo("45d");
    }

    @Test
    void should_apply_the_retention_of_the_v2_type_to_its_v4_counterparts() {
        configuration.setDataRetentionRequest("30d");
        configuration.setDataRetentionLog("7d");

        var retention = ServerlessRetention.of(configuration);

        assertThat(retention.of(Type.REQUEST)).isEqualTo("30d");
        assertThat(retention.of(Type.V4_METRICS)).isEqualTo("30d");
        assertThat(retention.of(Type.V4_MESSAGE_METRICS)).isEqualTo("30d");
        assertThat(retention.of(Type.LOG)).isEqualTo("7d");
        assertThat(retention.of(Type.V4_LOG)).isEqualTo("7d");
        assertThat(retention.of(Type.V4_MESSAGE_LOG)).isEqualTo("7d");
        assertThat(retention.of(Type.MONITOR)).isEqualTo("45d");
    }

    @Test
    void should_not_decrease_the_retention_of_existing_data_streams_by_default() {
        assertThat(ServerlessRetention.of(configuration).allowDecrease()).isFalse();
    }

    @Test
    void should_decrease_the_retention_of_existing_data_streams_when_allowed() {
        configuration.setDataRetentionAllowDecrease(true);

        assertThat(ServerlessRetention.of(configuration).allowDecrease()).isTrue();
    }

    @Test
    void should_summarize_the_retention_of_each_setting() {
        configuration.setDataRetentionLog("7d");

        assertThat(ServerlessRetention.of(configuration).summary()).isEqualTo(
            "request=45d, log=7d, monitor=45d, health=45d, event_metrics=45d, decisions=45d"
        );
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "45d", "12h", "90m", "3600s", "1000ms", "1d" })
    void should_accept_the_durations_elasticsearch_accepts(String retention) {
        configuration.setDataRetentionHealth(retention);

        assertThat(ServerlessRetention.violation(configuration)).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "7 days", "7", "d", "-1d", "1.5d", "7w", "0d" })
    void should_refuse_a_retention_that_is_not_a_positive_elasticsearch_duration(String retention) {
        configuration.setDataRetentionHealth(retention);

        assertThat(ServerlessRetention.violation(configuration)).hasValueSatisfying(message ->
            assertThat(message)
                .contains("'reporters.elasticsearch.lifecycle.retention.health'")
                .contains("'" + retention + "'")
                .contains("45d")
        );
    }

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource({ "45d, PT1080H", "12h, PT12H", "90m, PT1H30M", "30s, PT30S", "1500ms, PT1.5S" })
    void should_read_the_duration(String retention, Duration expected) {
        assertThat(ServerlessRetention.duration(retention)).isEqualTo(expected);
    }
}
