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
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.elasticsearch.TimeProvider;
import io.gravitee.repository.log.v4.model.authz.AuthzDecisionLog;
import io.gravitee.repository.log.v4.model.authz.AuthzDecisionLogQuery;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Runs the decision search against a real Elasticsearch, so the query is checked against the mapping
 * the index template actually produces. Sorting, exact-match filters and the nested policy filter all
 * need doc_values, which a string field only has when it is mapped as a keyword.
 */
public class MetricsElasticsearchRepositorySearchAuthzDecisionLogsTest extends AbstractElasticsearchRepositoryTest {

    private static final String API_ID = "authz-api-001";
    private static final String ENTITY_REFS_API_ID = "authz-api-entity-refs";
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

    static Stream<Arguments> subject_references() {
        return Stream.of(
            arguments(Set.of("alice"), List.of("ref-001", "ref-002", "ref-003")),
            arguments(Set.of("User::alice"), List.of("ref-001")),
            arguments(Set.of("User::\"alice\""), List.of("ref-001")),
            arguments(Set.of("docs::User::alice"), List.of("ref-002")),
            arguments(Set.of("docs::User::\"alice\""), List.of("ref-002")),
            arguments(Set.of("User::a::b"), List.of("ref-004")),
            arguments(Set.of("User::\"a::b\""), List.of("ref-004")),
            arguments(Set.of("x::y"), List.of("ref-005")),
            arguments(Set.of("User::\"say \\\"hi\\\"\""), List.of("ref-006")),
            arguments(Set.of("User::\"C:\\\\temp\""), List.of("ref-008")),
            arguments(Set.of("User::\"*\""), List.of("ref-009")),
            arguments(Set.of("User::*"), List.of("ref-001", "ref-004", "ref-006", "ref-007", "ref-008", "ref-009")),
            arguments(Set.of("Agent::alice", "User::\"a::b\""), List.of("ref-003", "ref-004")),
            arguments(Set.of("Agent::bob"), List.of())
        );
    }

    @ParameterizedTest(name = "{0} matches {1}")
    @MethodSource("subject_references")
    void should_match_a_subject_by_the_reference_the_decisions_table_shows(Set<String> subjects, List<String> expectedEventIds)
        throws AnalyticsException {
        var result = metricsV4Repository.searchAuthzDecisionLogs(queryContext, entityRefsQuery().subjectIds(subjects).build());

        assertThat(result.data()).extracting(AuthzDecisionLog::eventId).containsExactlyInAnyOrderElementsOf(expectedEventIds);
    }

    static Stream<Arguments> resource_references() {
        return Stream.of(
            arguments(Set.of("d1"), List.of("ref-001", "ref-002", "ref-007")),
            arguments(Set.of("Doc::d1"), List.of("ref-001", "ref-007")),
            arguments(Set.of("Doc::\"d1\""), List.of("ref-001", "ref-007")),
            arguments(Set.of("docs::Doc::\"d1\""), List.of("ref-002")),
            arguments(Set.of("r::s"), List.of("ref-005"))
        );
    }

    @ParameterizedTest(name = "{0} matches {1}")
    @MethodSource("resource_references")
    void should_match_a_resource_by_the_reference_the_decisions_table_shows(Set<String> resources, List<String> expectedEventIds)
        throws AnalyticsException {
        var result = metricsV4Repository.searchAuthzDecisionLogs(queryContext, entityRefsQuery().resourceIds(resources).build());

        assertThat(result.data()).extracting(AuthzDecisionLog::eventId).containsExactlyInAnyOrderElementsOf(expectedEventIds);
    }

    private AuthzDecisionLogQuery.AuthzDecisionLogQueryBuilder baseQuery() {
        return AuthzDecisionLogQuery.builder().apiIds(Set.of(API_ID)).from(FROM_MILLIS).to(TO_MILLIS).page(1).size(20);
    }

    private AuthzDecisionLogQuery.AuthzDecisionLogQueryBuilder entityRefsQuery() {
        return AuthzDecisionLogQuery.builder().apiIds(Set.of(ENTITY_REFS_API_ID)).from(FROM_MILLIS).to(TO_MILLIS).page(1).size(20);
    }
}
