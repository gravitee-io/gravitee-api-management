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
package io.gravitee.repository.elasticsearch.v4.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.index.IndexNameGenerator;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.model.TotalHits;
import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MetricsElasticsearchRepositoryTest_FindAuthzDecisionLog {

    private static final QueryContext QUERY_CONTEXT = new QueryContext("org#1", "env#1");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Client client;
    private IndexNameGenerator indexNameGenerator;
    private MetricsElasticsearchRepository repository;

    @BeforeEach
    void setUp() {
        client = mock(Client.class);
        indexNameGenerator = mock(IndexNameGenerator.class);
        when(indexNameGenerator.getWildcardIndexName(any(), any(), any())).thenReturn("gravitee-event-metrics-*");

        repository = new MetricsElasticsearchRepository(mock(RepositoryConfiguration.class));
        ReflectionTestUtils.setField(repository, "client", client);
        ReflectionTestUtils.setField(repository, "indexNameGenerator", indexNameGenerator);
    }

    @Test
    @SneakyThrows
    void reads_the_decision_from_the_event_metrics_data_stream() {
        when(client.search(any(), any(), any())).thenReturn(
            Single.just(
                responseWith(
                    """
                    { "event-id": "evt-1", "api-id": "api-1", "request-id": "req-1", "decision": "PERMIT" }
                    """
                )
            )
        );

        var decision = repository.findAuthzDecisionLog(QUERY_CONTEXT, "api-1", "evt-1");

        assertThat(decision).isPresent();
        assertThat(decision.get().eventId()).isEqualTo("evt-1");
        assertThat(decision.get().decision()).isEqualTo("PERMIT");
        // Decisions live in the event-metrics data stream, not in the request-shaped v4 metrics index.
        verify(indexNameGenerator).getWildcardIndexName(any(), eq(Type.EVENT_METRICS), any());
    }

    @Test
    @SneakyThrows
    void looks_the_decision_up_by_event_id_within_the_api() {
        when(client.search(any(), any(), any())).thenReturn(Single.just(new SearchResponse()));

        repository.findAuthzDecisionLog(QUERY_CONTEXT, "api-1", "evt-1");

        var query = ArgumentCaptor.forClass(String.class);
        verify(client).search(eq("gravitee-event-metrics-*"), eq(null), query.capture());
        assertThat(query.getValue()).contains("evt-1").contains("api-1");
    }

    @Test
    @SneakyThrows
    void finds_nothing_when_no_decision_matches() {
        when(client.search(any(), any(), any())).thenReturn(Single.just(new SearchResponse()));

        assertThat(repository.findAuthzDecisionLog(QUERY_CONTEXT, "api-1", "missing")).isEmpty();
    }

    @Test
    void reports_a_failed_lookup_as_an_analytics_exception() {
        when(client.search(any(), any(), any())).thenThrow(new RuntimeException("cluster unreachable"));

        assertThatThrownBy(() -> repository.findAuthzDecisionLog(QUERY_CONTEXT, "api-1", "evt-1"))
            .isInstanceOf(AnalyticsException.class)
            .hasMessageContaining("evt-1")
            .hasMessageContaining("api-1");
    }

    @SneakyThrows
    private SearchResponse responseWith(String source) {
        var response = new SearchResponse();
        var hits = new SearchHits();
        var hit = new SearchHit();
        hit.setSource(MAPPER.readTree(source));
        hits.setHits(List.of(hit));
        hits.setTotal(new TotalHits(1));
        response.setSearchHits(hits);
        return response;
    }
}
