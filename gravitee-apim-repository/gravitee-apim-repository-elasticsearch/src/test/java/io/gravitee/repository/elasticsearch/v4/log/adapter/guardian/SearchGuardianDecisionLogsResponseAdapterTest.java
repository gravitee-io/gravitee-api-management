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
package io.gravitee.repository.elasticsearch.v4.log.adapter.guardian;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.model.TotalHits;
import io.gravitee.repository.log.v4.model.guardian.GuardianDecisionLog;
import java.util.Arrays;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchGuardianDecisionLogsResponseAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void returns_an_empty_response_when_the_search_had_no_hits() {
        var response = SearchGuardianDecisionLogsResponseAdapter.adapt(new SearchResponse());

        assertThat(response.data()).isEmpty();
        assertThat(response.total()).isZero();
    }

    @Test
    void carries_the_total_and_every_decision_of_the_page() {
        var response = responseOf(5L, "{ \"event-id\": \"evt-1\", \"outcome\": \"DENY\" }", "{ \"event-id\": \"evt-2\" }");

        var result = SearchGuardianDecisionLogsResponseAdapter.adapt(response);

        assertThat(result.total()).isEqualTo(5L);
        assertThat(result.data()).extracting(GuardianDecisionLog::eventId).containsExactly("evt-1", "evt-2");
        assertThat(result.data().getFirst().outcome()).isEqualTo("DENY");
    }

    @Test
    void finds_no_decision_when_the_lookup_came_back_without_hits() {
        assertThat(SearchGuardianDecisionLogsResponseAdapter.adaptFirst(new SearchResponse())).isEmpty();
    }

    @Test
    void reads_the_one_decision_a_by_id_lookup_returns() {
        var response = responseOf(1L, "{ \"event-id\": \"evt-9\", \"decision-point-id\": \"guardian-9\" }");

        assertThat(SearchGuardianDecisionLogsResponseAdapter.adaptFirst(response))
            .get()
            .extracting(GuardianDecisionLog::guardianId)
            .isEqualTo("guardian-9");
    }

    private SearchResponse responseOf(long total, String... sources) {
        var response = new SearchResponse();
        var hits = new SearchHits();
        hits.setHits(Arrays.stream(sources).map(this::hitOf).toList());
        hits.setTotal(new TotalHits(total));
        response.setSearchHits(hits);
        return response;
    }

    @SneakyThrows
    private SearchHit hitOf(String source) {
        var hit = new SearchHit();
        hit.setSource(objectMapper.readTree(source));
        return hit;
    }
}
