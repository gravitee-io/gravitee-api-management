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
package io.gravitee.repository.elasticsearch.v4.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.elasticsearch.TimeProvider;
import io.gravitee.repository.log.v4.model.analytics.AdditionalMetricDiscoveryQuery;
import io.gravitee.repository.log.v4.model.analytics.AdditionalMetricDiscoveryResult;
import io.gravitee.repository.log.v4.model.analytics.AdditionalMetricLastSeen;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * Request documents of two environments sharing the same indexes, as every environment of a cluster does: the
 * {@code disc-*} documents of {@code freemarker-v4-analytics/v4-metrics.ftl}. They sit more than ten days back, under
 * environments and an API of their own, so the fixtures of other suites, which carry no environment, are never
 * discovered here.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@TestPropertySource(properties = "reporters.elasticsearch.template_mapping.path=src/test/resources/freemarker-v4-analytics")
class AnalyticsElasticsearchRepositoryAdditionalMetricDiscoveryTest extends AbstractElasticsearchRepositoryTest {

    private static final String ENV_A = "discovery-env-a";
    private static final String ENV_B = "discovery-env-b";
    private static final Instant TODAY = TimeProvider.now().truncatedTo(ChronoUnit.DAYS);
    private static final Instant NOW = TimeProvider.now();
    private static final AdditionalMetricDiscoveryQuery LAST_30_DAYS = new AdditionalMetricDiscoveryQuery(
        NOW.minus(Duration.ofDays(30)),
        NOW,
        Duration.ofSeconds(30)
    );

    @Autowired
    private AnalyticsElasticsearchRepository cut;

    private int defaultBatchSize;

    @BeforeEach
    void probe_two_keys_per_search() {
        // The pass over these documents takes several batches.
        defaultBatchSize = cut.discoveryBatchSize;
        cut.discoveryBatchSize = 2;
    }

    @AfterEach
    void restore_the_batch_size() {
        cut.discoveryBatchSize = defaultBatchSize;
    }

    @Test
    void should_list_the_keys_of_the_environment_most_recently_seen_first() {
        var result = cut.discoverAdditionalMetrics(new QueryContext("DEFAULT", ENV_A), LAST_30_DAYS);

        assertThat(result).isEqualTo(
            new AdditionalMetricDiscoveryResult(
                List.of(
                    new AdditionalMetricLastSeen("long_failover_count", at(11, 5)),
                    new AdditionalMetricLastSeen("keyword_mcp-proxy_tools/call", at(11, 0)),
                    new AdditionalMetricLastSeen("bool_cache_hit", at(13, 0)),
                    new AdditionalMetricLastSeen("keyword_fraud-check_tier", at(13, 0))
                ),
                false
            )
        );
    }

    @Test
    void should_never_list_a_key_seen_only_in_another_environment_nor_take_its_last_seen() {
        var result = cut.discoverAdditionalMetrics(new QueryContext("DEFAULT", ENV_B), LAST_30_DAYS);

        assertThat(result.metrics()).containsExactly(
            new AdditionalMetricLastSeen("keyword_fraud-check_tier", at(12, 0)),
            new AdditionalMetricLastSeen("long_only-in-b", at(12, 0))
        );
    }

    @Test
    void should_list_nothing_for_an_environment_without_documents() {
        var result = cut.discoverAdditionalMetrics(new QueryContext("DEFAULT", "discovery-env-empty"), LAST_30_DAYS);

        assertThat(result).isEqualTo(AdditionalMetricDiscoveryResult.NONE);
    }

    @Test
    void should_answer_what_it_found_flagged_truncated_when_out_of_time() {
        var noTime = new AdditionalMetricDiscoveryQuery(LAST_30_DAYS.from(), LAST_30_DAYS.to(), Duration.ZERO);

        var result = cut.discoverAdditionalMetrics(new QueryContext("DEFAULT", ENV_A), noTime);

        assertThat(result).isEqualTo(new AdditionalMetricDiscoveryResult(List.of(), true));
    }

    @Test
    void should_refuse_to_run_without_an_environment() {
        assertThatThrownBy(() -> cut.discoverAdditionalMetrics(new QueryContext("DEFAULT", null), LAST_30_DAYS)).isInstanceOf(
            IllegalArgumentException.class
        );
        assertThatThrownBy(() -> cut.discoverAdditionalMetrics(new QueryContext("DEFAULT", " "), LAST_30_DAYS)).isInstanceOf(
            IllegalArgumentException.class
        );
    }

    private static Instant at(int daysAgo, int minute) {
        return TODAY.minus(Duration.ofDays(daysAgo)).plus(Duration.ofHours(8).plusMinutes(minute));
    }
}
