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

import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.elasticsearch.TimeProvider;
import io.gravitee.repository.log.v4.model.guardrail.GuardrailDecisionLog;
import io.gravitee.repository.log.v4.model.guardrail.GuardrailDecisionLogQuery;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Runs the Guardrail decision reads against a real Elasticsearch, so the queries are checked against the mapping the
 * index template actually produces.
 */
public class MetricsElasticsearchRepositoryGuardrailDecisionLogsTest extends AbstractElasticsearchRepositoryTest {

    private static final String API_ID = "guardrail-api-001";
    private static final String AUTHZ_API_ID = "authz-api-001";
    private static final long FROM_MILLIS = TimeProvider.now().minusSeconds(600).toEpochMilli();
    private static final long TO_MILLIS = TimeProvider.now().plusSeconds(600).toEpochMilli();

    private final QueryContext queryContext = new QueryContext("org#1", "env#1");

    @Autowired
    private MetricsElasticsearchRepository metricsV4Repository;

    @Test
    void should_return_every_decision_of_the_api_newest_first() throws AnalyticsException {
        var result = metricsV4Repository.searchGuardrailDecisionLogs(queryContext, baseQuery().build());

        assertThat(result.total()).isEqualTo(5);
        // grd-004 and grd-005 share a timestamp: the event id settles their order.
        assertThat(result.data())
            .extracting(GuardrailDecisionLog::eventId)
            .containsExactly("grd-004", "grd-005", "grd-003", "grd-002", "grd-001");
    }

    @Test
    void should_page_through_a_tie_without_repeating_or_skipping_a_row() throws AnalyticsException {
        var firstPage = metricsV4Repository.searchGuardrailDecisionLogs(queryContext, baseQuery().page(1).size(1).build());
        var secondPage = metricsV4Repository.searchGuardrailDecisionLogs(queryContext, baseQuery().page(2).size(1).build());

        assertThat(firstPage.total()).isEqualTo(5);
        assertThat(firstPage.data()).extracting(GuardrailDecisionLog::eventId).containsExactly("grd-004");
        assertThat(secondPage.data()).extracting(GuardrailDecisionLog::eventId).containsExactly("grd-005");
    }

    @Test
    void should_match_the_guardrail() throws AnalyticsException {
        var result = metricsV4Repository.searchGuardrailDecisionLogs(queryContext, baseQuery().guardrailIds(Set.of("guardrail-b")).build());

        assertThat(result.data()).extracting(GuardrailDecisionLog::eventId).containsExactly("grd-004", "grd-003");
    }

    @Test
    void should_match_the_outcome() throws AnalyticsException {
        var result = metricsV4Repository.searchGuardrailDecisionLogs(
            queryContext,
            baseQuery().outcomes(Set.of("DENY", "TRANSFORM")).build()
        );

        assertThat(result.data()).extracting(GuardrailDecisionLog::eventId).containsExactly("grd-002", "grd-001");
    }

    @Test
    void should_find_a_decision_by_the_start_of_its_request_id_in_any_case() throws AnalyticsException {
        var result = metricsV4Repository.searchGuardrailDecisionLogs(queryContext, baseQuery().text("REQ-G-3").build());

        assertThat(result.data()).extracting(GuardrailDecisionLog::eventId).containsExactly("grd-003");
    }

    /** A request id is one value per call: matching a fragment anywhere in it would walk every term of the index. */
    @Test
    void should_not_find_a_decision_by_a_fragment_inside_its_request_id() throws AnalyticsException {
        var result = metricsV4Repository.searchGuardrailDecisionLogs(queryContext, baseQuery().text("g-3").build());

        assertThat(result.total()).isZero();
    }

    @Test
    void should_find_a_decision_by_its_guardrail_policy_in_any_case() throws AnalyticsException {
        var result = metricsV4Repository.searchGuardrailDecisionLogs(queryContext, baseQuery().text("BLOCK CREDENTIALS").build());

        assertThat(result.data()).extracting(GuardrailDecisionLog::eventId).containsExactly("grd-005", "grd-001");
    }

    @Test
    void should_find_a_decision_by_the_name_of_a_rule_it_found() throws AnalyticsException {
        var result = metricsV4Repository.searchGuardrailDecisionLogs(queryContext, baseQuery().text("phone").build());

        assertThat(result.data()).extracting(GuardrailDecisionLog::eventId).containsExactly("grd-002");
    }

    @Test
    void should_find_the_decisions_of_the_apis_the_text_names() throws AnalyticsException {
        var result = metricsV4Repository.searchGuardrailDecisionLogs(
            queryContext,
            baseQuery().text("Support assistant").textApiIds(Set.of(API_ID)).build()
        );

        assertThat(result.total()).isEqualTo(5);
    }

    /** A proxy name widens the text match, never the scope: an API named by the text but outside the search is not read. */
    @Test
    void should_not_read_an_api_the_text_names_outside_the_apis_searched() throws AnalyticsException {
        var result = metricsV4Repository.searchGuardrailDecisionLogs(
            queryContext,
            baseQuery().text("nothing-holds-this").textApiIds(Set.of(AUTHZ_API_ID)).build()
        );

        assertThat(result.total()).isZero();
    }

    @Test
    void should_find_nothing_for_a_text_nothing_holds() throws AnalyticsException {
        var result = metricsV4Repository.searchGuardrailDecisionLogs(queryContext, baseQuery().text("nothing-holds-this").build());

        assertThat(result.total()).isZero();
    }

    @Test
    void should_leave_out_a_decision_outside_the_time_range() throws AnalyticsException {
        var result = metricsV4Repository.searchGuardrailDecisionLogs(
            queryContext,
            baseQuery().from(TimeProvider.now().plusSeconds(60).toEpochMilli()).build()
        );

        assertThat(result.total()).isZero();
        assertThat(result.data()).isEmpty();
    }

    @Test
    void should_return_only_the_guardrail_decisions_of_an_api_that_also_has_authz_decisions() throws AnalyticsException {
        var result = metricsV4Repository.searchGuardrailDecisionLogs(queryContext, baseQuery().apiIds(Set.of(AUTHZ_API_ID)).build());

        assertThat(result.data()).extracting(GuardrailDecisionLog::eventId).containsExactly("guardrail-001");
    }

    @Test
    void should_return_only_the_decisions_of_the_calling_environment() throws AnalyticsException {
        var result = metricsV4Repository.searchGuardrailDecisionLogs(new QueryContext("org#1", "env#2"), baseQuery().build());

        assertThat(result.data()).extracting(GuardrailDecisionLog::eventId).containsExactly("grd-envB");
    }

    @Test
    void should_return_an_empty_page_for_an_api_without_decisions() throws AnalyticsException {
        var result = metricsV4Repository.searchGuardrailDecisionLogs(queryContext, baseQuery().apiIds(Set.of("no-such-api")).build());

        assertThat(result.total()).isZero();
        assertThat(result.data()).isEmpty();
    }

    @Test
    void should_read_what_the_guardrail_policies_found_and_where() throws AnalyticsException {
        var decision = decision("grd-001");

        assertThat(decision.apiId()).isEqualTo(API_ID);
        assertThat(decision.guardrailId()).isEqualTo("guardrail-a");
        assertThat(decision.revision()).isEqualTo("3");
        assertThat(decision.checkpoint()).isEqualTo("request.llm-proxy");
        assertThat(decision.outcome()).isEqualTo("DENY");
        assertThat(decision.enforced()).isEqualTo("DENY");
        assertThat(decision.verdict()).isEqualTo("BLOCK");
        assertThat(decision.requestId()).isEqualTo("req-g-1");
        assertThat(decision.durationNanos()).isEqualTo(4_200_000L);
        assertThat(decision.steps()).containsExactly(
            new GuardrailDecisionLog.Step("Block credentials", "BLOCK", "1 credential found"),
            new GuardrailDecisionLog.Step("Topic check", "PASS", null)
        );
        assertThat(decision.findings()).containsExactly(
            new GuardrailDecisionLog.Finding("Block credentials", "aws-access-key", "$.messages[0].content", "DENY")
        );
    }

    @Test
    void should_read_what_was_applied_to_a_transformed_response() throws AnalyticsException {
        var decision = decision("grd-002");

        assertThat(decision.enforced()).isEqualTo("TRANSFORM");
        assertThat(decision.transformationType()).isEqualTo("REDACT");
        assertThat(decision.findings()).extracting(GuardrailDecisionLog.Finding::rule).containsExactly("EMAIL", "PHONE");
    }

    private GuardrailDecisionLog decision(String eventId) throws AnalyticsException {
        return metricsV4Repository
            .searchGuardrailDecisionLogs(queryContext, baseQuery().build())
            .data()
            .stream()
            .filter(decision -> eventId.equals(decision.eventId()))
            .findFirst()
            .orElseThrow();
    }

    private GuardrailDecisionLogQuery.GuardrailDecisionLogQueryBuilder baseQuery() {
        return GuardrailDecisionLogQuery.builder().apiIds(Set.of(API_ID)).from(FROM_MILLIS).to(TO_MILLIS).page(1).size(20);
    }
}
