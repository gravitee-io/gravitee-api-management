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
import io.gravitee.repository.log.v4.model.guardian.GuardianDecisionLog;
import io.gravitee.repository.log.v4.model.guardian.GuardianDecisionLogQuery;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Runs the Guardian decision search against a real Elasticsearch, so the query is checked against the mapping the
 * index template produces: every field it filters or sorts on must be a keyword or a date.
 */
public class MetricsElasticsearchRepositoryGuardianDecisionLogsTest extends AbstractElasticsearchRepositoryTest {

    private static final String API_ID = "guardian-api-001";
    private static final long FROM_MILLIS = TimeProvider.now().minusSeconds(600).toEpochMilli();
    private static final long TO_MILLIS = TimeProvider.now().plusSeconds(600).toEpochMilli();

    private final QueryContext queryContext = new QueryContext("org#1", "env#1");

    @Autowired
    private MetricsElasticsearchRepository metricsV4Repository;

    @Test
    void should_return_every_decision_of_the_api_newest_first_with_the_total() throws AnalyticsException {
        var result = metricsV4Repository.searchGuardianDecisionLogs(queryContext, apiQuery().build());

        assertThat(result.total()).isEqualTo(5);
        // gd-004 and gd-005 share a timestamp: the event id orders them.
        assertThat(result.data())
            .extracting(GuardianDecisionLog::eventId)
            .containsExactly("gd-004", "gd-005", "gd-003", "gd-002", "gd-001");
    }

    @Test
    void should_page_through_the_decisions_without_repeating_or_skipping_one() throws AnalyticsException {
        var firstPage = metricsV4Repository.searchGuardianDecisionLogs(queryContext, apiQuery().page(1).size(2).build());
        var secondPage = metricsV4Repository.searchGuardianDecisionLogs(queryContext, apiQuery().page(2).size(2).build());

        assertThat(firstPage.total()).isEqualTo(5);
        assertThat(firstPage.data()).extracting(GuardianDecisionLog::eventId).containsExactly("gd-004", "gd-005");
        assertThat(secondPage.data()).extracting(GuardianDecisionLog::eventId).containsExactly("gd-003", "gd-002");
    }

    @Test
    void should_read_every_api_of_the_environment_when_no_api_is_given() throws AnalyticsException {
        var result = metricsV4Repository.searchGuardianDecisionLogs(
            queryContext,
            GuardianDecisionLogQuery.builder().guardianIds(Set.of("guardian-a")).from(FROM_MILLIS).to(TO_MILLIS).build()
        );

        assertThat(result.data()).extracting(GuardianDecisionLog::eventId).containsExactly("gd-005", "gd-other-api", "gd-002", "gd-001");
    }

    @Test
    void should_read_no_decision_when_the_given_api_set_is_empty() throws AnalyticsException {
        var result = metricsV4Repository.searchGuardianDecisionLogs(
            queryContext,
            GuardianDecisionLogQuery.builder().apiIds(Set.of()).build()
        );

        assertThat(result.total()).isZero();
        assertThat(result.data()).isEmpty();
    }

    @Test
    void should_read_only_the_decisions_of_a_guardian() throws AnalyticsException {
        var result = metricsV4Repository.searchGuardianDecisionLogs(
            queryContext,
            GuardianDecisionLogQuery.builder().apiIds(Set.of("authz-api-001")).build()
        );

        assertThat(result.data()).extracting(GuardianDecisionLog::eventId).containsExactly("guardian-001");
    }

    @Test
    void should_match_the_guardians() throws AnalyticsException {
        var result = metricsV4Repository.searchGuardianDecisionLogs(queryContext, apiQuery().guardianIds(Set.of("guardian-b")).build());

        assertThat(result.data()).extracting(GuardianDecisionLog::eventId).containsExactly("gd-004", "gd-003");
    }

    @Test
    void should_match_the_outcomes() throws AnalyticsException {
        var result = metricsV4Repository.searchGuardianDecisionLogs(queryContext, apiQuery().outcomes(Set.of("DENY", "TRANSFORM")).build());

        assertThat(result.data()).extracting(GuardianDecisionLog::eventId).containsExactly("gd-003", "gd-002");
    }

    @Test
    void should_match_the_decisions_of_a_call_by_its_exact_request_id() throws AnalyticsException {
        var result = metricsV4Repository.searchGuardianDecisionLogs(queryContext, apiQuery().requestIds(Set.of("req-g-002")).build());

        assertThat(result.data()).extracting(GuardianDecisionLog::eventId).containsExactly("gd-003", "gd-002");
    }

    @Test
    void should_match_a_fragment_of_the_request_id_ignoring_case() throws AnalyticsException {
        var result = metricsV4Repository.searchGuardianDecisionLogs(queryContext, apiQuery().requestIdContains("g-005-mIX").build());

        assertThat(result.data()).extracting(GuardianDecisionLog::eventId).containsExactly("gd-005");
    }

    @Test
    void should_keep_the_decisions_of_the_time_range() throws AnalyticsException {
        var result = metricsV4Repository.searchGuardianDecisionLogs(
            queryContext,
            apiQuery()
                .from(TimeProvider.now().minusSeconds(150).toEpochMilli())
                .to(TimeProvider.now().minusSeconds(30).toEpochMilli())
                .build()
        );

        assertThat(result.data()).extracting(GuardianDecisionLog::eventId).containsExactly("gd-003", "gd-002");
    }

    @Test
    void search_does_not_cross_environments_or_organizations() throws AnalyticsException {
        var result = metricsV4Repository.searchGuardianDecisionLogs(queryContext, apiQuery().guardianIds(Set.of("guardian-a")).build());

        assertThat(result.data()).extracting(GuardianDecisionLog::eventId).doesNotContain("gd-envB", "gd-orgB");
        assertThat(metricsV4Repository.searchGuardianDecisionLogs(new QueryContext("org#1", "env#2"), apiQuery().build()).data())
            .extracting(GuardianDecisionLog::eventId)
            .containsExactly("gd-envB");
    }

    @Test
    void should_read_back_every_field_the_gateway_reported() throws AnalyticsException {
        var decision = metricsV4Repository.findGuardianDecisionLog(queryContext, "gd-002").orElseThrow();

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(decision.eventId()).isEqualTo("gd-002");
            soft.assertThat(decision.organizationId()).isEqualTo("org#1");
            soft.assertThat(decision.environmentId()).isEqualTo("env#1");
            soft.assertThat(decision.gatewayId()).isEqualTo("gw-1");
            soft.assertThat(decision.apiId()).isEqualTo(API_ID);
            soft.assertThat(decision.planId()).isEqualTo("plan-1");
            soft.assertThat(decision.applicationId()).isEqualTo("app-1");
            soft.assertThat(decision.guardianId()).isEqualTo("guardian-a");
            soft.assertThat(decision.guardianRevision()).isEqualTo("1");
            soft.assertThat(decision.checkpoint()).isEqualTo("request.llm-proxy");
            soft.assertThat(decision.caller()).isEqualTo("Guardian A");
            soft.assertThat(decision.requestId()).isEqualTo("req-g-002");
            soft.assertThat(decision.traceId()).isEqualTo("trace-g-002");
            soft.assertThat(decision.outcome()).isEqualTo("DENY");
            soft.assertThat(decision.enforced()).isEqualTo("DENY");
            soft.assertThat(decision.verdict()).isEqualTo("GUARDRAIL_BLOCKED");
            soft.assertThat(decision.status()).isEqualTo("success");
            soft.assertThat(decision.reasons()).containsExactly("zz: BLOCK — Blocked by guardrail; llm-rule matched: messages[0].user");
            soft
                .assertThat(decision.matchedRules())
                .containsExactly(
                    new GuardianDecisionLog.MatchedRule(null, null, null, "DENY", Map.of("guardrail", "zz", "location", "messages[0].user"))
                );
            soft.assertThat(decision.durationNanos()).isEqualTo(7794875L);
            soft.assertThat(decision.subjectType()).isEqualTo("user");
            soft.assertThat(decision.subjectId()).isEqualTo("alice");
            soft.assertThat(decision.actorType()).isEqualTo("agent");
            soft.assertThat(decision.actorId()).isEqualTo("app-1");
        });
    }

    @Test
    void should_read_back_a_transformation_and_an_undecided_run() throws AnalyticsException {
        var transformation = metricsV4Repository.findGuardianDecisionLog(queryContext, "gd-003").orElseThrow();
        var undecided = metricsV4Repository.findGuardianDecisionLog(queryContext, "gd-004").orElseThrow();

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(transformation.transformed()).isTrue();
            soft.assertThat(transformation.transformationType()).isEqualTo("REDACT");
            soft.assertThat(transformation.checkpoint()).isEqualTo("response.llm-proxy");
            soft.assertThat(undecided.outcome()).isEqualTo("INDETERMINATE");
            soft.assertThat(undecided.indeterminateCause()).isEqualTo("NOT_READY");
            soft.assertThat(undecided.errorType()).isEqualTo("GUARDIAN_NOT_DEPLOYED");
            soft.assertThat(undecided.guardianRevision()).isNull();
        });
    }

    @Test
    void find_does_not_cross_environments_or_organizations() throws AnalyticsException {
        assertThat(metricsV4Repository.findGuardianDecisionLog(queryContext, "gd-envB")).isEmpty();
        assertThat(metricsV4Repository.findGuardianDecisionLog(queryContext, "gd-orgB")).isEmpty();
    }

    @Test
    void find_does_not_read_the_decision_of_another_decision_point() throws AnalyticsException {
        assertThat(metricsV4Repository.findGuardianDecisionLog(queryContext, "evt-001")).isEmpty();
    }

    private GuardianDecisionLogQuery.GuardianDecisionLogQueryBuilder apiQuery() {
        return GuardianDecisionLogQuery.builder().apiIds(Set.of(API_ID)).from(FROM_MILLIS).to(TO_MILLIS).page(1).size(20);
    }
}
