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
import io.gravitee.repository.log.v4.model.decision.DecisionLog;
import io.gravitee.repository.log.v4.model.decision.DecisionLogQuery;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Runs the document read against a real Elasticsearch, so the query is checked against the mapping the
 * index template actually produces rather than against an assumption about it.
 *
 * <p>The {@code decisions} data stream is shared by every kind of decision point, and a point may write
 * twice for one decision. Both of those are properties of the index, not of the adapter, so they are
 * pinned here with documents of both families and both phases sitting side by side.
 */
public class MetricsElasticsearchRepositorySearchDecisionLogsTest extends AbstractElasticsearchRepositoryTest {

    private static final String GUARDIAN = "guardian";
    private static final String HUMAN_APPROVAL = "human-approval";
    private static final long FROM_MILLIS = TimeProvider.now().minusSeconds(600).toEpochMilli();
    private static final long TO_MILLIS = TimeProvider.now().plusSeconds(600).toEpochMilli();

    // The fixtures are stamped org-id / env-id DEFAULT, and the decisions data stream is shared by every
    // environment: a context that does not match them must read nothing.
    private final QueryContext queryContext = new QueryContext("DEFAULT", "DEFAULT");

    @Autowired
    private MetricsElasticsearchRepository metricsV4Repository;

    @Test
    void should_read_only_the_requested_decision_point_family_out_of_the_shared_index() throws AnalyticsException {
        var guardian = metricsV4Repository.searchDecisionLogs(queryContext, baseQuery(GUARDIAN).build());

        assertThat(guardian.data()).isNotEmpty();
        assertThat(guardian.data()).extracting(DecisionLog::decisionPointType).containsOnly(GUARDIAN);
        assertThat(guardian.data()).extracting(DecisionLog::eventId).doesNotContain("dec-h-001", "dec-h-002");

        // The other family is in the very same index, and reachable through the same call.
        var humanApproval = metricsV4Repository.searchDecisionLogs(queryContext, baseQuery(HUMAN_APPROVAL).build());

        assertThat(humanApproval.data()).extracting(DecisionLog::eventId).containsExactly("dec-h-001");
    }

    @Test
    void should_read_nothing_for_an_environment_the_decisions_were_not_written_in() throws AnalyticsException {
        var otherEnvironment = metricsV4Repository.searchDecisionLogs(new QueryContext("org#1", "env#1"), baseQuery(GUARDIAN).build());

        assertThat(otherEnvironment.total()).isZero();
        assertThat(otherEnvironment.data()).isEmpty();
    }

    @Test
    void should_count_a_settled_consultation_once_although_it_wrote_twice() throws AnalyticsException {
        var settled = metricsV4Repository.searchDecisionLogs(queryContext, baseQuery(GUARDIAN).caseIds(Set.of("case-g-1")).build());

        // case-g-1 has a REQUESTED record and a RESOLVED one; only the second carries the outcome.
        assertThat(settled.total()).isEqualTo(1);
        assertThat(settled.data()).extracting(DecisionLog::eventId).containsExactly("dec-g-005");
        assertThat(settled.data()).extracting(DecisionLog::phase).containsOnly("RESOLVED");

        var everyGuardianDecision = metricsV4Repository.searchDecisionLogs(queryContext, baseQuery(GUARDIAN).build());

        assertThat(everyGuardianDecision.data()).extracting(DecisionLog::phase).containsOnly("RESOLVED");
        assertThat(everyGuardianDecision.data()).extracting(DecisionLog::eventId).doesNotContain("dec-g-004");
    }

    @Test
    void should_return_the_newest_decision_first_and_page_through_the_rest() throws AnalyticsException {
        var all = metricsV4Repository.searchDecisionLogs(queryContext, baseQuery(GUARDIAN).build());

        assertThat(all.total()).isEqualTo(4);
        assertThat(all.data()).extracting(DecisionLog::eventId).containsExactly("dec-g-005", "dec-g-003", "dec-g-002", "dec-g-001");

        var firstPage = metricsV4Repository.searchDecisionLogs(queryContext, baseQuery(GUARDIAN).page(1).size(2).build());
        var secondPage = metricsV4Repository.searchDecisionLogs(queryContext, baseQuery(GUARDIAN).page(2).size(2).build());

        assertThat(firstPage.data()).extracting(DecisionLog::eventId).containsExactly("dec-g-005", "dec-g-003");
        assertThat(secondPage.data()).extracting(DecisionLog::eventId).containsExactly("dec-g-002", "dec-g-001");
        assertThat(firstPage.total()).isEqualTo(4);
    }

    @Test
    void should_report_a_field_the_document_does_not_carry_as_null() throws AnalyticsException {
        var result = metricsV4Repository.searchDecisionLogs(queryContext, baseQuery(GUARDIAN).build());

        var allowed = result
            .data()
            .stream()
            .filter(d -> "dec-g-001".equals(d.eventId()))
            .findFirst()
            .orElseThrow();
        var indeterminate = result
            .data()
            .stream()
            .filter(d -> "dec-g-003".equals(d.eventId()))
            .findFirst()
            .orElseThrow();

        // An ALLOW reaches a conclusion, so it has no cause for not reaching one.
        assertThat(allowed.outcome()).isEqualTo("ALLOW");
        assertThat(allowed.indeterminateCause()).isNull();
        assertThat(indeterminate.indeterminateCause()).isEqualTo("TIMEOUT");
    }

    private DecisionLogQuery.DecisionLogQueryBuilder baseQuery(String decisionPointType) {
        return DecisionLogQuery.builder().decisionPointType(decisionPointType).from(FROM_MILLIS).to(TO_MILLIS).page(1).size(20);
    }
}
