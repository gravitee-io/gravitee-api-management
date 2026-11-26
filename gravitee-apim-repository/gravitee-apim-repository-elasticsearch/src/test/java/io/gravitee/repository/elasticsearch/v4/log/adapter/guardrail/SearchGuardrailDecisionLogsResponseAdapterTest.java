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
package io.gravitee.repository.elasticsearch.v4.log.adapter.guardrail;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.model.TotalHits;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchGuardrailDecisionLogsResponseAdapterTest {

    private static final int MAX_RESULT_WINDOW = 10_000;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void returns_an_empty_page_when_the_search_had_no_hits() {
        var response = SearchGuardrailDecisionLogsResponseAdapter.adapt(new SearchResponse(), MAX_RESULT_WINDOW);

        assertThat(response.data()).isEmpty();
        assertThat(response.total()).isZero();
    }

    @Test
    void counts_nothing_when_the_hits_carry_no_total() {
        var response = responseOf(null);

        var result = SearchGuardrailDecisionLogsResponseAdapter.adapt(response, MAX_RESULT_WINDOW);

        assertThat(result.total()).isZero();
        assertThat(result.data())
            .singleElement()
            .satisfies(decision -> assertThat(decision.eventId()).isEqualTo("evt-1"));
    }

    @Test
    void carries_the_total_of_a_search_within_the_result_window() {
        var result = SearchGuardrailDecisionLogsResponseAdapter.adapt(responseOf(3L), MAX_RESULT_WINDOW);

        assertThat(result.total()).isEqualTo(3L);
    }

    @Test
    void caps_the_total_at_the_result_window_so_no_page_past_it_is_offered() {
        var result = SearchGuardrailDecisionLogsResponseAdapter.adapt(responseOf(25_000L), MAX_RESULT_WINDOW);

        assertThat(result.total()).isEqualTo(MAX_RESULT_WINDOW);
        assertThat(result.data()).hasSize(1);
    }

    @SneakyThrows
    private SearchResponse responseOf(Long total) {
        var hit = new SearchHit();
        hit.setSource(objectMapper.readTree("{ \"event-id\": \"evt-1\", \"api-id\": \"api-1\", \"outcome\": \"DENY\" }"));
        var hits = new SearchHits();
        hits.setHits(List.of(hit));
        if (total != null) {
            hits.setTotal(new TotalHits(total));
        }
        var response = new SearchResponse();
        response.setSearchHits(hits);
        return response;
    }
}
