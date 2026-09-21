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

import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.TimeRange;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.elasticsearch.TimeProvider;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * The connection lookback, turned off.
 *
 * <p>Its own context because the setting is read once when the repository is built. Worth the extra
 * startup: without this, nothing proves the property is wired — the convergence asserted in
 * {@code AnalyticsElasticsearchRepositoryTest} would read the same whether the value came from
 * configuration or from a constant someone hardcoded, and a typo in the property name would look
 * exactly like a feature that works.
 *
 * <p>It also pins what {@code 0} means. The javadoc on {@code messageConnectionLookbackSeconds} offers
 * it as the way back to the previous behaviour, and an operator who takes that offer after a bad
 * upgrade needs it to be true.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@TestPropertySource(
    properties = {
        "reporters.elasticsearch.template_mapping.path=src/test/resources/freemarker-v4-analytics",
        "analytics.elasticsearch.message_connection_lookback_seconds=0",
    }
)
class MessageConnectionLookbackDisabledTest extends AbstractElasticsearchRepositoryTest {

    private static final QueryContext QUERY_CONTEXT = new QueryContext("DEFAULT", "DEFAULT");
    private static final String MESSAGE_API = "f1608475-dd77-4603-a084-75dd775603e9";
    private static final String PUBLISH_API = "4a6895d5-a1bc-4041-a895-d5a1bce041ae";

    private static final Instant NOW = TimeProvider.now().truncatedTo(ChronoUnit.DAYS);
    private static final Instant TOMORROW = NOW.plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.DAYS);

    @Autowired
    private AnalyticsElasticsearchRepository cut;

    /**
     * The same window and fixture as the convergence test: connection {@code 5fc3b3e5} opens at
     * 06:54:30 and carries 31 messages after 06:55:00. With no lookback the join cannot see it, so the
     * joined path reports 374 against the direct path's 405 — the discrepancy this setting exists to
     * close, reproduced on demand.
     */
    @Test
    void should_drop_straddling_connections_again_when_the_lookback_is_zero() {
        var from = NOW.plus(Duration.ofHours(6)).plus(Duration.ofMinutes(55));
        var window = new TimeRange(from, TOMORROW);
        var metrics = List.of(new MetricMeasuresQuery(Metric.MESSAGES, Set.of(Measure.COUNT)));
        var api = new Filter(Filter.Name.API, Filter.Operator.IN, List.of(MESSAGE_API, PUBLISH_API));
        var everyEntrypoint = new Filter(Filter.Name.ENTRYPOINT, Filter.Operator.IN, List.of("http-post", "http-get", "websocket", "sse"));

        var direct = cut.searchMessageMeasures(QUERY_CONTEXT, new MeasuresQuery(window, List.of(api), metrics));
        var joined = cut.searchMessageMeasures(QUERY_CONTEXT, new MeasuresQuery(window, List.of(api, everyEntrypoint), metrics));

        assertThat(messagesOf(direct)).isEqualTo(405L);
        assertThat(messagesOf(joined)).isEqualTo(374L);
    }

    private static long messagesOf(io.gravitee.repository.analytics.engine.api.result.MeasuresResult result) {
        return result
            .measures()
            .stream()
            .filter(measure -> measure.metric() == Metric.MESSAGES)
            .findFirst()
            .orElseThrow()
            .measures()
            .get(Measure.COUNT)
            .longValue();
    }
}
