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

import static java.lang.String.format;

import io.gravitee.apim.reporter.common.bulk.BulkConfiguration;
import java.util.Objects;
import java.util.Optional;

/**
 * The bulk settings the reporter refuses: RxJava rejects a concurrency below one with an error that names no setting, and a
 * memory size below one byte drops every bulk.
 */
public final class BulkRules {

    static final String MAX_CONCURRENT_SEND_SETTING = "reporters.elasticsearch.bulk.max_concurrent_send";
    static final String MAX_MEMORY_SIZE_SETTING = "reporters.elasticsearch.bulk.max_memory_size";
    static final String MAX_RETRIES_SETTING = "reporters.elasticsearch.bulk.retry.max_retries";
    static final String INITIAL_DELAY_SETTING = "reporters.elasticsearch.bulk.retry.initial_delay";
    static final String MAX_DELAY_SETTING = "reporters.elasticsearch.bulk.retry.max_delay";

    private BulkRules() {}

    /**
     * @return why a bulk setting cannot be used, naming it, or empty when all can
     */
    public static Optional<String> violation(ReporterConfiguration configuration) {
        if (below(configuration.getBulkMaxConcurrentSend(), 1)) {
            return notA(MAX_CONCURRENT_SEND_SETTING, configuration.getBulkMaxConcurrentSend(), "positive number of bulks");
        }
        if (below(configuration.getBulkMaxMemorySize(), 1)) {
            return notA(MAX_MEMORY_SIZE_SETTING, configuration.getBulkMaxMemorySize(), "positive number of bytes");
        }
        if (below(configuration.getBulkMaxRetries(), 0)) {
            return notA(MAX_RETRIES_SETTING, configuration.getBulkMaxRetries(), "number of retries, 0 or more");
        }
        if (below(configuration.getBulkRetryInitialDelay(), 0)) {
            return notA(INITIAL_DELAY_SETTING, configuration.getBulkRetryInitialDelay(), "delay in milliseconds, 0 or more");
        }
        if (below(configuration.getBulkRetryMaxDelay(), 0)) {
            return notA(MAX_DELAY_SETTING, configuration.getBulkRetryMaxDelay(), "delay in milliseconds, 0 or more");
        }
        final int initialDelay = Objects.requireNonNullElse(
            configuration.getBulkRetryInitialDelay(),
            BulkConfiguration.DEFAULT_RETRY_INITIAL_DELAY
        );
        final int maxDelay = Objects.requireNonNullElse(configuration.getBulkRetryMaxDelay(), BulkConfiguration.DEFAULT_RETRY_MAX_DELAY);
        if (maxDelay < initialDelay) {
            return Optional.of(
                format(
                    "'%s' is '%s', below '%s' ('%s'): the delay between retries grows from the initial delay up to the max delay.",
                    MAX_DELAY_SETTING,
                    maxDelay,
                    INITIAL_DELAY_SETTING,
                    initialDelay
                )
            );
        }
        return Optional.empty();
    }

    private static boolean below(Number value, long minimum) {
        return value != null && value.longValue() < minimum;
    }

    private static Optional<String> notA(String setting, Number value, String expected) {
        return Optional.of(format("'%s' is set to '%s', which is not a %s.", setting, value, expected));
    }
}
