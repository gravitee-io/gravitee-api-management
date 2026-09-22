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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.index.IndexNameGenerator;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.TimeRange;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import io.reactivex.rxjava3.core.Single;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * What the direct-path gate does when it cannot answer its own question.
 *
 * <p>Mocked rather than run against the container, because the interesting input is a *failing*
 * Elasticsearch: the integration fixtures always map the dimensions correctly, so the fallback and
 * its caching cannot be reached there. Follows the mocking shape already used by
 * {@code ElasticsearchTracingRepositoryTest} in this module.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MessageDirectPathReadinessTest {

    private static final QueryContext QUERY_CONTEXT = new QueryContext("DEFAULT", "DEFAULT");

    private Client client;
    private IndexNameGenerator mockIndexNameGenerator;
    private AnalyticsElasticsearchRepository cut;

    @BeforeEach
    void setUp() {
        client = Mockito.mock(Client.class);
        mockIndexNameGenerator = Mockito.mock(IndexNameGenerator.class);
        when(mockIndexNameGenerator.getWildcardIndexName(any(), any(), any())).thenReturn("gravitee-v4-any-*");
        // The gate asks the mapping about the indices the window reads, not the wildcard.
        when(mockIndexNameGenerator.getIndexName(any(), any(), anyLong(), anyLong(), any())).thenReturn("gravitee-v4-any-2026.09.28");

        cut = repositoryWithWatermarkTtl(Duration.ofMinutes(1));

        // Every search answers an empty response: the join runs and finds nothing, which is enough to
        // observe *which* path was taken without asserting numbers that belong to the fixture tests.
        var emptyResponse = new SearchResponse();
        // The response adapters read `timedOut` unguarded; an all-defaults response would NPE there
        // and hide what this test is about.
        emptyResponse.setTimedOut(false);
        when(client.search(anyString(), any(), anyString())).thenReturn(Single.just(emptyResponse));
    }

    /**
     * An anonymous subclass because the collaborators are protected fields of
     * {@code AbstractElasticsearchRepository}, injected by Spring in production and unreachable from
     * here otherwise. The watermark interval goes in the same way, so a test can watch an entry expire
     * without waiting out the production one.
     */
    private AnalyticsElasticsearchRepository repositoryWithWatermarkTtl(Duration ttl) {
        var configuration = Mockito.mock(RepositoryConfiguration.class);
        when(configuration.hasCrossClusterMapping()).thenReturn(false);

        var mockClient = client;
        var indexNames = mockIndexNameGenerator;
        return new AnalyticsElasticsearchRepository(configuration) {
            {
                this.client = mockClient;
                this.indexNameGenerator = indexNames;
                this.enrichmentWatermarkTtl = ttl;
            }
        };
    }

    /** The query the gate let through, as it was handed to Elasticsearch. */
    private String lastQuery() {
        var queries = ArgumentCaptor.forClass(String.class);
        verify(client, Mockito.atLeastOnce()).search(anyString(), any(), queries.capture());
        return queries.getAllValues().getLast();
    }

    /** A response carrying a watermark: the newest message document that carries no marker. */
    private static SearchResponse watermarkOf(Instant newestUnstamped) {
        var aggregation = new Aggregation();
        aggregation.setValue((double) newestUnstamped.toEpochMilli());
        var response = new SearchResponse();
        response.setTimedOut(false);
        response.setAggregations(Map.of("newest_unenriched", aggregation));
        return response;
    }

    /** A query on a dimension the documents carry only once stamped, so the gate has to be consulted. */
    private static MeasuresQuery planFilteredQuery() {
        return planFilteredQueryFrom(Instant.now().minus(1, ChronoUnit.HOURS));
    }

    private static MeasuresQuery planFilteredQueryFrom(Instant from) {
        return new MeasuresQuery(
            new TimeRange(from, from.plus(1, ChronoUnit.HOURS)),
            List.of(new Filter(Filter.Name.PLAN, Filter.Operator.IN, List.of("gold"))),
            List.of(new MetricMeasuresQuery(Metric.MESSAGES, Set.of(Measure.COUNT)))
        );
    }

    /**
     * The join reaches the connection documents through a composite aggregation, and the direct path
     * has nothing to page through — so the last query says which one ran, without depending on counts
     * that belong to the fixture tests.
     */
    private static final String JOIN_MARKER = "composite";

    /**
     * A failing probe must not fail the request. The join answers every document whatever wrote it,
     * so it is always the safe reply — the gate is an optimisation, not a precondition.
     */
    @Test
    void should_still_answer_when_the_probe_fails() {
        when(client.getFieldTypes(anyString(), anyString())).thenReturn(Single.error(new RuntimeException("elasticsearch is down")));

        assertThatCode(() -> cut.searchMessageMeasures(QUERY_CONTEXT, planFilteredQuery())).doesNotThrowAnyException();
    }

    /**
     * And it must not be re-run by every request. An uncached failure means each caller pays the
     * client timeout before falling back, turning one slow probe into a slow dashboard.
     */
    @Test
    void should_not_probe_again_within_the_interval_after_a_failure() {
        when(client.getFieldTypes(anyString(), anyString())).thenReturn(Single.error(new RuntimeException("elasticsearch is down")));

        cut.searchMessageMeasures(QUERY_CONTEXT, planFilteredQuery());
        Mockito.clearInvocations(client);
        cut.searchMessageMeasures(QUERY_CONTEXT, planFilteredQuery());

        verify(client, never()).getFieldTypes(anyString(), anyString());
    }

    /**
     * A query naming only dimensions a message has always carried skips the gate entirely — no probe,
     * no watermark, nothing to pay for a question that does not arise.
     */
    @Test
    void should_not_probe_at_all_for_a_dimension_the_documents_always_carried() {
        var window = new TimeRange(Instant.now().minus(1, ChronoUnit.HOURS), Instant.now());
        var query = new MeasuresQuery(
            window,
            List.of(new Filter(Filter.Name.MESSAGE_OPERATION_TYPE, Filter.Operator.IN, List.of("publish"))),
            List.of(new MetricMeasuresQuery(Metric.MESSAGES, Set.of(Measure.COUNT)))
        );

        cut.searchMessageMeasures(QUERY_CONTEXT, query);

        verify(client, never()).getFieldTypes(anyString(), anyString());
        verify(client, times(1)).search(anyString(), any(), anyString());
    }

    /**
     * The margin is what covers a watermark going stale between probes, so a window that starts inside
     * it has to keep the join even though it starts after the watermark itself.
     */
    @Test
    void should_keep_the_join_for_a_window_starting_inside_the_watermark_margin() {
        var watermark = Instant.now().minus(10, ChronoUnit.MINUTES);
        when(client.getFieldTypes(anyString(), anyString())).thenReturn(Single.just(List.of("keyword")));
        when(client.search(anyString(), any(), anyString())).thenReturn(Single.just(watermarkOf(watermark)));

        cut.searchMessageMeasures(QUERY_CONTEXT, planFilteredQueryFrom(watermark.plusSeconds(30)));

        assertThat(lastQuery()).contains(JOIN_MARKER);
    }

    /** And the other side of the same boundary, or the gate would be shut rather than conservative. */
    @Test
    void should_read_the_messages_directly_for_a_window_starting_past_the_watermark_margin() {
        var watermark = Instant.now().minus(10, ChronoUnit.MINUTES);
        when(client.getFieldTypes(anyString(), anyString())).thenReturn(Single.just(List.of("keyword")));
        when(client.search(anyString(), any(), anyString())).thenReturn(Single.just(watermarkOf(watermark)));

        cut.searchMessageMeasures(QUERY_CONTEXT, planFilteredQueryFrom(watermark.plus(2, ChronoUnit.MINUTES)));

        assertThat(lastQuery()).doesNotContain(JOIN_MARKER);
    }

    /**
     * The cached answer is a lease, not a verdict: a fleet finishes upgrading and an index rolls over,
     * and the gate has to notice. A zero interval means every call re-probes, which is what pins the
     * expiry without waiting one out.
     */
    @Test
    void should_probe_again_once_the_cached_answer_has_expired() {
        when(client.getFieldTypes(anyString(), anyString())).thenReturn(Single.error(new RuntimeException("elasticsearch is down")));
        var alwaysExpired = repositoryWithWatermarkTtl(Duration.ZERO);

        alwaysExpired.searchMessageMeasures(QUERY_CONTEXT, planFilteredQuery());
        Mockito.clearInvocations(client);
        alwaysExpired.searchMessageMeasures(QUERY_CONTEXT, planFilteredQuery());

        verify(client, times(1)).getFieldTypes(anyString(), anyString());
    }

    /**
     * The index that was live when the gateway was upgraded has no template for the new fields, so
     * Elasticsearch maps them dynamically: a {@code text} field with a {@code keyword} sub-field. A
     * term filter on the analysed one matches nothing, which is the case the mapping guard exists for —
     * and the one where opening the direct path would answer zero instead of the truth.
     */
    @Test
    void should_keep_the_join_when_a_dimension_is_mapped_as_text() {
        when(client.getFieldTypes(anyString(), anyString())).thenReturn(Single.just(List.of("text", "keyword")));

        cut.searchMessageMeasures(QUERY_CONTEXT, planFilteredQuery());

        assertThat(lastQuery()).contains(JOIN_MARKER);
    }

    /**
     * Asked about the wildcard, the one index that predates the template would answer for every window
     * — and keep the join until retention deleted it, which is about as long as it takes for the join to
     * stop being needed at all. The question belongs to the indices the window actually reads.
     */
    @Test
    void should_ask_the_mapping_about_the_windows_indices_rather_than_the_wildcard() {
        when(client.getFieldTypes(anyString(), anyString())).thenReturn(Single.just(List.of("keyword")));

        cut.searchMessageMeasures(QUERY_CONTEXT, planFilteredQuery());

        // Widened into a pattern: a period with no index would otherwise answer index_not_found for the
        // whole call, and the period's per-leg indices carry a suffix the generator does not produce.
        verify(client, Mockito.atLeastOnce()).getFieldTypes(eq("gravitee-v4-any-2026.09.28*"), anyString());
        verify(client, never()).getFieldTypes(eq("gravitee-v4-any-*"), anyString());
    }

    /**
     * Unbounded, the watermark search scans every unstamped document in the retention on each probe. It
     * only has to look at what is newer than the last answer, because the answer is clamped never to
     * move backwards.
     */
    @Test
    void should_bound_the_watermark_search_at_the_previous_answer() {
        var watermark = Instant.now().minus(10, ChronoUnit.MINUTES);
        when(client.getFieldTypes(anyString(), anyString())).thenReturn(Single.just(List.of("keyword")));
        when(client.search(anyString(), any(), anyString())).thenReturn(Single.just(watermarkOf(watermark)));
        var alwaysExpired = repositoryWithWatermarkTtl(Duration.ZERO);

        alwaysExpired.searchMessageMeasures(QUERY_CONTEXT, planFilteredQuery());
        Mockito.clearInvocations(client);
        alwaysExpired.searchMessageMeasures(QUERY_CONTEXT, planFilteredQuery());

        var queries = ArgumentCaptor.forClass(String.class);
        verify(client, Mockito.atLeastOnce()).search(anyString(), any(), queries.capture());
        assertThat(queries.getAllValues().getFirst()).contains("\"gte\":" + watermark.toEpochMilli());
    }

    /**
     * What makes the bound safe. Once retention deletes the documents that set the watermark, the
     * bounded search answers nothing — and reading that as "everything is stamped" would open the direct
     * path over the windows those documents were in. The answer is clamped instead, so it degrades
     * toward the join.
     */
    @Test
    void should_not_let_the_watermark_move_backwards_when_the_bounded_search_finds_nothing() {
        var watermark = Instant.now().minus(10, ChronoUnit.MINUTES);
        when(client.getFieldTypes(anyString(), anyString())).thenReturn(Single.just(List.of("keyword")));

        var watermarkSearches = new AtomicInteger();
        var stamped = new SearchResponse();
        stamped.setTimedOut(false);
        when(client.search(anyString(), any(), anyString())).thenAnswer(invocation -> {
            String query = invocation.getArgument(2);
            if (!query.contains("newest_unenriched")) {
                return Single.just(stamped);
            }
            return Single.just(watermarkSearches.getAndIncrement() == 0 ? watermarkOf(watermark) : stamped);
        });

        var alwaysExpired = repositoryWithWatermarkTtl(Duration.ZERO);
        alwaysExpired.searchMessageMeasures(QUERY_CONTEXT, planFilteredQueryFrom(watermark.minus(5, ChronoUnit.MINUTES)));
        Mockito.clearInvocations(client);
        alwaysExpired.searchMessageMeasures(QUERY_CONTEXT, planFilteredQueryFrom(watermark.minus(5, ChronoUnit.MINUTES)));

        assertThat(lastQuery()).contains(JOIN_MARKER);
    }

    /**
     * An unmapped dimension answers an empty list rather than failing, which a plain "are they all
     * keywords" reads as yes — {@code allMatch} is vacuously true on nothing. It is the one case where
     * the direct path is guaranteed to answer zero instead of the truth. A cross-cluster prefix lands
     * here on every query: the mapping endpoint behind {@code getFieldTypes} does not resolve remote
     * indices.
     */
    @Test
    void should_keep_the_join_when_a_dimension_is_not_mapped_at_all() {
        when(client.getFieldTypes(anyString(), anyString())).thenReturn(Single.just(List.of()));

        cut.searchMessageMeasures(QUERY_CONTEXT, planFilteredQuery());

        assertThat(lastQuery()).contains(JOIN_MARKER);
    }
}
