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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BulkRulesTest {

    private final ReporterConfiguration configuration = new ReporterConfiguration();

    @Test
    void should_accept_the_defaults() {
        assertThat(BulkRules.violation(configuration)).isEmpty();
    }

    @Test
    void should_accept_valid_settings() {
        configuration.setBulkMaxConcurrentSend(1);
        configuration.setBulkMaxMemorySize(1L);
        configuration.setBulkMaxRetries(0);
        configuration.setBulkRetryInitialDelay(0);
        configuration.setBulkRetryMaxDelay(0);

        assertThat(BulkRules.violation(configuration)).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(ints = { 0, -1 })
    void should_refuse_a_max_concurrent_send_below_one(int value) {
        configuration.setBulkMaxConcurrentSend(value);

        assertThat(BulkRules.violation(configuration)).hasValueSatisfying(message ->
            assertThat(message).contains("'reporters.elasticsearch.bulk.max_concurrent_send'").contains("'" + value + "'")
        );
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(longs = { 0, -1 })
    void should_refuse_a_max_memory_size_below_one_byte(long value) {
        configuration.setBulkMaxMemorySize(value);

        assertThat(BulkRules.violation(configuration)).hasValueSatisfying(message ->
            assertThat(message).contains("'reporters.elasticsearch.bulk.max_memory_size'").contains("'" + value + "'")
        );
    }

    @Test
    void should_refuse_negative_retries() {
        configuration.setBulkMaxRetries(-1);

        assertThat(BulkRules.violation(configuration)).hasValueSatisfying(message ->
            assertThat(message).contains("'reporters.elasticsearch.bulk.retry.max_retries'").contains("'-1'")
        );
    }

    @Test
    void should_refuse_a_negative_initial_delay() {
        configuration.setBulkRetryInitialDelay(-1);

        assertThat(BulkRules.violation(configuration)).hasValueSatisfying(message ->
            assertThat(message).contains("'reporters.elasticsearch.bulk.retry.initial_delay'").contains("'-1'")
        );
    }

    @Test
    void should_refuse_a_negative_max_delay() {
        configuration.setBulkRetryMaxDelay(-1);

        assertThat(BulkRules.violation(configuration)).hasValueSatisfying(message ->
            assertThat(message).contains("'reporters.elasticsearch.bulk.retry.max_delay'").contains("'-1'")
        );
    }

    @Test
    void should_refuse_a_max_delay_below_the_initial_delay_including_the_default_one() {
        configuration.setBulkRetryMaxDelay(1000);

        assertThat(BulkRules.violation(configuration)).hasValueSatisfying(message ->
            assertThat(message)
                .contains("'reporters.elasticsearch.bulk.retry.max_delay'")
                .contains("'1000'")
                .contains("'reporters.elasticsearch.bulk.retry.initial_delay'")
                .contains("'3000'")
        );
    }
}
