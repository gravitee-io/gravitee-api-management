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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.index.IndexNameGenerator;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
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
    private AnalyticsElasticsearchRepository cut;

    @BeforeEach
    void setUp() {
        client = Mockito.mock(Client.class);
        var mockIndexNameGenerator = Mockito.mock(IndexNameGenerator.class);
        when(mockIndexNameGenerator.getWildcardIndexName(any(), any(), any())).thenReturn("gravitee-v4-any-*");

        var configuration = Mockito.mock(RepositoryConfiguration.class);
        when(configuration.hasCrossClusterMapping()).thenReturn(false);

        // An anonymous subclass because the collaborators are protected fields of
        // AbstractElasticsearchRepository, injected by Spring in production and unreachable from here
        // otherwise.
        var mockClient = client;
        cut = new AnalyticsElasticsearchRepository(configuration) {
            {
                this.client = mockClient;
                this.indexNameGenerator = mockIndexNameGenerator;
            }
        };

        // Every search answers an empty response: the join runs and finds nothing, which is enough to
        // observe *which* path was taken without asserting numbers that belong to the fixture tests.
        var emptyResponse = new SearchResponse();
        // The response adapters read `timedOut` unguarded; an all-defaults response would NPE there
        // and hide what this test is about.
        emptyResponse.setTimedOut(false);
        when(client.search(anyString(), any(), anyString())).thenReturn(Single.just(emptyResponse));
    }

    /** A query on a dimension the documents carry only once stamped, so the gate has to be consulted. */
    private static MeasuresQuery planFilteredQuery() {
        var window = new TimeRange(Instant.now().minus(1, ChronoUnit.HOURS), Instant.now());
        return new MeasuresQuery(
            window,
            List.of(new Filter(Filter.Name.PLAN, Filter.Operator.IN, List.of("gold"))),
            List.of(new MetricMeasuresQuery(Metric.MESSAGES, Set.of(Measure.COUNT)))
        );
    }

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
}
