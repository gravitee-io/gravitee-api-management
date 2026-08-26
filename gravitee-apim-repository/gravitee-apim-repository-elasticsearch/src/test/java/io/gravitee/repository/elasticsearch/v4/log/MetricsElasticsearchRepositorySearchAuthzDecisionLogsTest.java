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
import io.gravitee.repository.log.v4.model.authz.AuthzDecisionLog;
import io.gravitee.repository.log.v4.model.authz.AuthzDecisionLogQuery;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Runs the decision search against a real Elasticsearch, so the query is checked against the mapping
 * the index template actually produces. Sorting, exact-match filters and the nested policy filter all
 * need doc_values, which a string field only has when it is mapped as a keyword.
 */
public class MetricsElasticsearchRepositorySearchAuthzDecisionLogsTest extends AbstractElasticsearchRepositoryTest {

    private static final String API_ID = "authz-api-001";
    private static final long FROM_MILLIS = TimeProvider.now().minusSeconds(600).toEpochMilli();
    private static final long TO_MILLIS = TimeProvider.now().plusSeconds(600).toEpochMilli();

    private final QueryContext queryContext = new QueryContext("org#1", "env#1");

    @Autowired
    private MetricsElasticsearchRepository metricsV4Repository;

    @Test
    void should_return_every_decision_of_the_api_newest_first() throws AnalyticsException {
        var result = metricsV4Repository.searchAuthzDecisionLogs(queryContext, baseQuery().build());

        assertThat(result.total()).isEqualTo(6);
        // evt-004..006 share a timestamp: without the event-id tiebreaker their order is undefined and paging repeats rows.
        assertThat(result.data())
            .extracting(AuthzDecisionLog::eventId)
            .containsExactly("evt-004", "evt-005", "evt-006", "evt-003", "evt-002", "evt-001");
    }

    @Test
    void should_page_through_a_tie_without_repeating_or_skipping_a_row() throws AnalyticsException {
        var firstPage = metricsV4Repository.searchAuthzDecisionLogs(queryContext, baseQuery().page(1).size(2).build());
        var secondPage = metricsV4Repository.searchAuthzDecisionLogs(queryContext, baseQuery().page(2).size(2).build());

        assertThat(firstPage.data()).extracting(AuthzDecisionLog::eventId).containsExactly("evt-004", "evt-005");
        assertThat(secondPage.data()).extracting(AuthzDecisionLog::eventId).containsExactly("evt-006", "evt-003");
    }

    @Test
    void should_match_the_exact_decision_value() throws AnalyticsException {
        var result = metricsV4Repository.searchAuthzDecisionLogs(queryContext, baseQuery().decisions(Set.of("PERMIT")).build());

        assertThat(result.data()).extracting(AuthzDecisionLog::eventId).containsExactly("evt-004", "evt-005", "evt-001");
    }

    @Test
    void should_match_the_exact_subject() throws AnalyticsException {
        assertThat(metricsV4Repository.searchAuthzDecisionLogs(queryContext, baseQuery().subjectIds(Set.of("alice")).build()).data())
            .extracting(AuthzDecisionLog::eventId)
            .containsExactly("evt-004", "evt-005", "evt-001");
    }

    @Test
    void should_match_the_exact_action() throws AnalyticsException {
        assertThat(metricsV4Repository.searchAuthzDecisionLogs(queryContext, baseQuery().actions(Set.of("read")).build()).data())
            .extracting(AuthzDecisionLog::eventId)
            .containsExactly("evt-004", "evt-005", "evt-006", "evt-001");
    }

    @Test
    void should_match_the_exact_caller() throws AnalyticsException {
        assertThat(metricsV4Repository.searchAuthzDecisionLogs(queryContext, baseQuery().callers(Set.of("authzen")).build()).data())
            .extracting(AuthzDecisionLog::eventId)
            .containsExactly("evt-003");
    }

    @Test
    void should_match_the_exact_status() throws AnalyticsException {
        assertThat(metricsV4Repository.searchAuthzDecisionLogs(queryContext, baseQuery().statuses(Set.of("error")).build()).data())
            .extracting(AuthzDecisionLog::eventId)
            .containsExactly("evt-006");
    }

    @Test
    void should_match_a_policy_name_inside_the_nested_matched_policies() throws AnalyticsException {
        var result = metricsV4Repository.searchAuthzDecisionLogs(
            queryContext,
            baseQuery().matchedPolicyNames(Set.of("deny-writers")).build()
        );

        assertThat(result.data()).extracting(AuthzDecisionLog::eventId).containsExactly("evt-002");
    }

    @Test
    void should_match_a_fragment_of_a_reason() throws AnalyticsException {
        var result = metricsV4Repository.searchAuthzDecisionLogs(queryContext, baseQuery().reasonContains("applicable").build());

        assertThat(result.data()).extracting(AuthzDecisionLog::eventId).containsExactly("evt-003");
    }

    private AuthzDecisionLogQuery.AuthzDecisionLogQueryBuilder baseQuery() {
        return AuthzDecisionLogQuery.builder().apiIds(Set.of(API_ID)).from(FROM_MILLIS).to(TO_MILLIS).page(1).size(20);
    }
}
