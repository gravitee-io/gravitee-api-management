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

import io.gravitee.apim.reporter.common.bulk.BulkConfiguration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

class ReporterConfigurationBulkTest {

    @Test
    void should_keep_the_bulk_defaults_when_nothing_is_configured() {
        final BulkConfiguration bulk = bind(Map.of()).getBulkConfiguration();

        assertThat(bulk.maxConcurrentSend()).isEqualTo(100);
        assertThat(bulk.maxRetries()).isEqualTo(6);
        assertThat(bulk.retryInitialDelay()).isEqualTo(3000);
        assertThat(bulk.retryMaxDelay()).isEqualTo(30000);
        assertThat(bulk.maxMemorySize()).isEqualTo(26214400L);
    }

    @Test
    void should_read_the_in_flight_and_retry_settings_of_the_bulks() {
        final BulkConfiguration bulk = bind(
            Map.of(
                "reporters.elasticsearch.bulk.max_concurrent_send",
                "20",
                "reporters.elasticsearch.bulk.max_memory_size",
                "52428800",
                "reporters.elasticsearch.bulk.retry.max_retries",
                "4",
                "reporters.elasticsearch.bulk.retry.initial_delay",
                "1000",
                "reporters.elasticsearch.bulk.retry.max_delay",
                "20000"
            )
        ).getBulkConfiguration();

        assertThat(bulk.maxConcurrentSend()).isEqualTo(20);
        assertThat(bulk.maxMemorySize()).isEqualTo(52428800L);
        assertThat(bulk.maxRetries()).isEqualTo(4);
        assertThat(bulk.retryInitialDelay()).isEqualTo(1000);
        assertThat(bulk.retryMaxDelay()).isEqualTo(20000);
    }

    @Test
    void should_read_whether_the_retention_of_existing_data_streams_may_decrease() {
        assertThat(bind(Map.of()).isDataRetentionAllowDecrease()).isFalse();
        assertThat(
            bind(Map.of("reporters.elasticsearch.lifecycle.retention.allow_decrease", "true")).isDataRetentionAllowDecrease()
        ).isTrue();
    }

    private static ReporterConfiguration bind(Map<String, Object> properties) {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", properties));
            context.registerBean(ReporterConfiguration.class);
            context.refresh();
            return context.getBean(ReporterConfiguration.class);
        }
    }
}
