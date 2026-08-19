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
    void carries_identity_request_and_outcome_of_an_evaluation() {
        var response = responseOf(
            """
            {
              "@timestamp": "2026-08-05T12:03:00.123+02:00",
              "doc-type": "authz",
              "event-id": "evt-1",
              "api-id": "api-1",
              "org-id": "org-1",
              "env-id": "env-1",
              "gw-id": "gateway-1",
              "request-id": "req-1",
              "operation": "evaluate",
              "status": "success",
              "caller": "pep",
              "target-pdp-id": "pdp-1",
              "policy-generation": 7,
              "decision": "PERMIT",
              "matched-policies": [
                { "id": "p1", "name": "allow-readers", "effect": "PERMIT" },
                { "id": "p2", "name": "allow-admins", "effect": "PERMIT" }
              ],
              "reasons": ["Permitted by policy 'allow-readers'"],
              "subject-type": "User",
              "subject-id": "alice",
              "action": "read",
              "resource-type": "Document",
              "resource-id": "doc-1",
              "duration-nanos": 4200
            }
            """,
            3L
        );

        var result = SearchAuthzDecisionLogsResponseAdapter.adapt(response);

        assertThat(result.total()).isEqualTo(3L);
        assertThat(result.data()).hasSize(1);
        var decision = result.data().getFirst();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(decision.eventId()).isEqualTo("evt-1");
            soft.assertThat(decision.timestamp()).isEqualTo(1785924180123L);
            soft.assertThat(decision.apiId()).isEqualTo("api-1");
            soft.assertThat(decision.organizationId()).isEqualTo("org-1");
            soft.assertThat(decision.environmentId()).isEqualTo("env-1");
            soft.assertThat(decision.gatewayId()).isEqualTo("gateway-1");
            soft.assertThat(decision.requestId()).isEqualTo("req-1");
            soft.assertThat(decision.operation()).isEqualTo("evaluate");
            soft.assertThat(decision.status()).isEqualTo("success");
            soft.assertThat(decision.caller()).isEqualTo("pep");
            soft.assertThat(decision.targetPdpId()).isEqualTo("pdp-1");
            soft.assertThat(decision.policyGeneration()).isEqualTo(7L);
            soft.assertThat(decision.decision()).isEqualTo("PERMIT");
            soft.assertThat(decision.matchedPolicyNames()).containsExactly("allow-readers", "allow-admins");
            soft.assertThat(decision.reasons()).containsExactly("Permitted by policy 'allow-readers'");
            soft.assertThat(decision.subjectType()).isEqualTo("User");
            soft.assertThat(decision.subjectId()).isEqualTo("alice");
            soft.assertThat(decision.action()).isEqualTo("read");
            soft.assertThat(decision.resourceType()).isEqualTo("Document");
            soft.assertThat(decision.resourceId()).isEqualTo("doc-1");
            soft.assertThat(decision.durationNanos()).isEqualTo(4200L);
        });
    }

    @Test
    @SneakyThrows
    void carries_the_batch_position_and_the_search_result_shape() {
        var response = responseOf(
            """
            {
              "@timestamp": "2026-08-05T10:03:00.000Z",
              "event-id": "evt-2",
              "operation": "search",
              "status": "success",
              "batch-id": "batch-1",
              "batch-index": 1,
              "batch-size": 2,
              "search-type": "subject",
              "result-count": 12
            }
            """,
            1L
        );

        var decision = SearchAuthzDecisionLogsResponseAdapter.adapt(response).data().getFirst();

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(decision.batchId()).isEqualTo("batch-1");
            soft.assertThat(decision.batchIndex()).isEqualTo(1);
            soft.assertThat(decision.batchSize()).isEqualTo(2);
            soft.assertThat(decision.searchType()).isEqualTo("subject");
            soft.assertThat(decision.resultCount()).isEqualTo(12);
        });
    }

    @Test
    @SneakyThrows
    void leaves_absent_fields_null_and_absent_lists_empty() {
        var response = responseOf(
            """
            { "event-id": "evt-3" }
            """,
            1L
        );

        var decision = SearchAuthzDecisionLogsResponseAdapter.adapt(response).data().getFirst();

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(decision.timestamp()).isNull();
            soft.assertThat(decision.decision()).isNull();
            soft.assertThat(decision.policyGeneration()).isNull();
            soft.assertThat(decision.batchIndex()).isNull();
            soft.assertThat(decision.matchedPolicyNames()).isEmpty();
            soft.assertThat(decision.reasons()).isEmpty();
        });
    }

    @Test
    @SneakyThrows
    void drops_a_matched_policy_that_carries_no_name_rather_than_reporting_a_null_row() {
        var response = responseOf(
            """
            {
              "event-id": "evt-4",
              "matched-policies": [{ "id": "p1" }, { "id": "p2", "name": "allow-admins" }]
            }
            """,
            1L
        );

        var decision = SearchAuthzDecisionLogsResponseAdapter.adapt(response).data().getFirst();

        assertThat(decision.matchedPolicyNames()).containsExactly("allow-admins");
    }

    @Test
    @SneakyThrows
    void reports_no_timestamp_rather_than_failing_the_page_when_the_stamp_is_unreadable() {
        var response = responseOf(
            """
            { "event-id": "evt-5", "@timestamp": "not-a-date" }
            """,
            1L
        );

        var decision = SearchAuthzDecisionLogsResponseAdapter.adapt(response).data().getFirst();

        assertThat(decision.timestamp()).isNull();
        assertThat(decision.eventId()).isEqualTo("evt-5");
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
              "decision": "DENY",
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
            soft.assertThat(decision.get().decision()).isEqualTo("DENY");
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
