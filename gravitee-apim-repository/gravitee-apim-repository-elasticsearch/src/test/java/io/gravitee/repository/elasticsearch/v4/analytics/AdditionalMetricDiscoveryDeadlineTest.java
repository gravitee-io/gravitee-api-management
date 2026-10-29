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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.index.IndexNameGenerator;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import io.gravitee.repository.log.v4.model.analytics.AdditionalMetricDiscoveryQuery;
import io.gravitee.repository.log.v4.model.analytics.AdditionalMetricDiscoveryResult;
import io.gravitee.repository.log.v4.model.analytics.AdditionalMetricLastSeen;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * What a discovery answers when Elasticsearch is slow. Mocked rather than run against the container, which always
 * answers in time.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AdditionalMetricDiscoveryDeadlineTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final QueryContext QUERY_CONTEXT = new QueryContext("DEFAULT", "env-1");
    private static final Instant SEEN = Instant.parse("2026-10-28T08:05:00Z");
    private static final Instant NOW = Instant.parse("2026-10-29T00:00:00Z");

    private Client client;
    private AnalyticsElasticsearchRepository cut;

    @BeforeEach
    void setUp() {
        client = Mockito.mock(Client.class);
        var indexNames = Mockito.mock(IndexNameGenerator.class);
        when(indexNames.getWildcardIndexName(any(), any(), any())).thenReturn("gravitee-v4-metrics-*");
        var configuration = Mockito.mock(RepositoryConfiguration.class);
        when(configuration.hasCrossClusterMapping()).thenReturn(false);
        var mockClient = client;
        // One key per search, so a pass takes one search per key.
        cut = new AnalyticsElasticsearchRepository(configuration) {
            {
                this.client = mockClient;
                this.indexNameGenerator = indexNames;
                this.discoveryBatchSize = 1;
            }
        };
        when(client.getFieldCapabilities(anyString(), anyString())).thenReturn(
            Single.just(
                Map.of(
                    "additional-metrics.long_a",
                    Set.of("long"),
                    "additional-metrics.long_b",
                    Set.of("long"),
                    "additional-metrics.long_c",
                    Set.of("long")
                )
            )
        );
    }

    @Test
    void should_keep_what_it_found_and_say_it_is_truncated_when_a_search_outlives_the_deadline() {
        when(client.search(anyString(), any(), anyString())).thenReturn(Single.just(seen(false)), Single.never());

        var result = cut.discoverAdditionalMetrics(QUERY_CONTEXT, query(Duration.ofMillis(500)));

        assertThat(result).isEqualTo(new AdditionalMetricDiscoveryResult(List.of(new AdditionalMetricLastSeen("long_a", SEEN)), true));
        verify(client, times(2)).search(anyString(), any(), anyString());
    }

    @Test
    void should_keep_a_partial_answer_and_stop_when_elasticsearch_runs_out_of_time() {
        when(client.search(anyString(), any(), anyString())).thenReturn(Single.just(seen(true)));

        var result = cut.discoverAdditionalMetrics(QUERY_CONTEXT, query(Duration.ofSeconds(30)));

        assertThat(result).isEqualTo(new AdditionalMetricDiscoveryResult(List.of(new AdditionalMetricLastSeen("long_a", SEEN)), true));
        verify(client, times(1)).search(anyString(), any(), anyString());
    }

    @Test
    void should_give_elasticsearch_less_time_than_the_client_waits() {
        when(client.search(anyString(), any(), anyString())).thenReturn(
            Single.just(seen(false)),
            Single.just(seen(false)),
            Single.just(seen(false))
        );

        cut.discoverAdditionalMetrics(QUERY_CONTEXT, query(Duration.ofSeconds(10)));

        var queries = ArgumentCaptor.forClass(String.class);
        verify(client, times(3)).search(anyString(), any(), queries.capture());
        assertThat(queries.getAllValues())
            .map(esQuery -> Long.parseLong(new JsonObject(esQuery).getString("timeout").replace("ms", "")))
            .allSatisfy(timeoutMs -> assertThat(timeoutMs).isPositive().isLessThanOrEqualTo(8_000L));
    }

    private static AdditionalMetricDiscoveryQuery query(Duration timeout) {
        return new AdditionalMetricDiscoveryQuery(NOW.minus(Duration.ofDays(30)), NOW, timeout);
    }

    /** A one-key search answering the key as seen. */
    private static SearchResponse seen(boolean timedOut) {
        try {
            return MAPPER.readValue(
                """
                { "timed_out": %s, "aggregations": { "keys": { "buckets": [ { "doc_count": 1, "last_seen": { "value": %d } } ] } } }
                """.formatted(timedOut, SEEN.toEpochMilli()),
                SearchResponse.class
            );
        } catch (Exception e) {
            throw new IllegalStateException("Unreadable test response", e);
        }
    }
}
