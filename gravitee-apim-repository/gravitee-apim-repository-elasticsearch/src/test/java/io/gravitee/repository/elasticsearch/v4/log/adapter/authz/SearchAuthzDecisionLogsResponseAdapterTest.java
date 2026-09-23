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
package io.gravitee.repository.elasticsearch.v4.log.adapter.authz;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.model.TotalHits;
import java.util.List;
import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchAuthzDecisionLogsResponseAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void returns_an_empty_response_when_the_search_had_no_hits() {
        var response = SearchAuthzDecisionLogsResponseAdapter.adapt(new SearchResponse());

        assertThat(response.data()).isEmpty();
        assertThat(response.total()).isZero();
    }

    @Test
    @SneakyThrows
    void carries_the_total_and_every_decision_of_the_page() {
        var response = responseOf(
            """
            { "event-id": "evt-1", "api-id": "api-1", "verdict": "PERMIT" }
            """,
            3L
        );

        var result = SearchAuthzDecisionLogsResponseAdapter.adapt(response);

        assertThat(result.total()).isEqualTo(3L);
        assertThat(result.data())
            .singleElement()
            .satisfies(decision -> {
                assertThat(decision.eventId()).isEqualTo("evt-1");
                assertThat(decision.decision()).isEqualTo("PERMIT");
            });
    }

    @Test
    void finds_no_decision_when_the_lookup_came_back_without_hits() {
        assertThat(SearchAuthzDecisionLogsResponseAdapter.adaptFirst(new SearchResponse())).isEmpty();
    }

    @Test
    void finds_no_decision_when_the_hit_list_is_absent_or_empty() {
        var withoutHitList = new SearchResponse();
        withoutHitList.setSearchHits(new SearchHits());

        var withEmptyHitList = new SearchResponse();
        var emptyHits = new SearchHits();
        emptyHits.setHits(List.of());
        emptyHits.setTotal(new TotalHits(0));
        withEmptyHitList.setSearchHits(emptyHits);

        assertThat(SearchAuthzDecisionLogsResponseAdapter.adaptFirst(withoutHitList)).isEmpty();
        assertThat(SearchAuthzDecisionLogsResponseAdapter.adaptFirst(withEmptyHitList)).isEmpty();
    }

    @Test
    @SneakyThrows
    void reads_the_one_decision_a_by_id_lookup_returns() {
        var response = responseOf(
            """
            {
              "event-id": "evt-9",
              "api-id": "api-9",
              "request-id": "req-9",
              "verdict": "FORBID",
              "reasons": ["No policy matched"]
            }
            """,
            1L
        );

        var decision = SearchAuthzDecisionLogsResponseAdapter.adaptFirst(response);

        assertThat(decision).isPresent();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(decision.get().eventId()).isEqualTo("evt-9");
            soft.assertThat(decision.get().apiId()).isEqualTo("api-9");
            soft.assertThat(decision.get().requestId()).isEqualTo("req-9");
            soft.assertThat(decision.get().decision()).isEqualTo("FORBID");
            soft.assertThat(decision.get().reasons()).containsExactly("No policy matched");
        });
    }

    @SneakyThrows
    private SearchResponse responseOf(String source, long total) {
        var response = new SearchResponse();
        var hits = new SearchHits();
        var hit = new SearchHit();
        hit.setSource(objectMapper.readTree(source));
        hits.setHits(List.of(hit));
        hits.setTotal(new TotalHits(total));
        response.setSearchHits(hits);
        return response;
    }
}
