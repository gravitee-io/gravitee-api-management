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
package io.gravitee.apim.infra.crud_service.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.log.crud_service.DecisionLogsCrudService;
import io.gravitee.apim.core.log.model.DecisionLog;
import io.gravitee.apim.core.log.model.DecisionLogFilters;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.MetricsRepository;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.decision.DecisionLogQuery;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DecisionLogsCrudServiceImplTest {

    private static final ExecutionContext CONTEXT = new ExecutionContext("org-1", "env-1");

    private MetricsRepository metricsRepository;
    private DecisionLogsCrudService service;

    @BeforeEach
    void setUp() {
        metricsRepository = mock(MetricsRepository.class);
        service = new DecisionLogsCrudServiceImpl(metricsRepository);
    }

    @Test
    void serves_any_decision_point_family_the_caller_names() throws Exception {
        when(metricsRepository.searchDecisionLogs(any(), any())).thenReturn(new LogResponse<>(0L, List.of()));

        service.searchDecisionLogs(CONTEXT, DecisionLogFilters.builder().decisionPointType("guardian").build(), new PageableImpl(1, 20));
        service.searchDecisionLogs(
            CONTEXT,
            DecisionLogFilters.builder().decisionPointType("human-approval").build(),
            new PageableImpl(1, 20)
        );

        var queries = ArgumentCaptor.forClass(DecisionLogQuery.class);
        verify(metricsRepository, org.mockito.Mockito.times(2)).searchDecisionLogs(any(), queries.capture());
        assertThat(queries.getAllValues()).extracting(DecisionLogQuery::getDecisionPointType).containsExactly("guardian", "human-approval");
    }

    @Test
    void refuses_a_search_that_names_no_decision_point_family() throws Exception {
        assertThatThrownBy(() -> service.searchDecisionLogs(CONTEXT, DecisionLogFilters.builder().build(), new PageableImpl(1, 20)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("decisionPointType");

        verify(metricsRepository, never()).searchDecisionLogs(any(), any());
    }

    @Test
    void does_not_query_the_index_when_the_caller_may_see_no_api() throws Exception {
        var response = service.searchDecisionLogs(
            CONTEXT,
            DecisionLogFilters.builder().decisionPointType("guardian").apiIds(Set.of()).build(),
            new PageableImpl(1, 20)
        );

        assertThat(response.total()).isZero();
        assertThat(response.logs()).isEmpty();
        verify(metricsRepository, never()).searchDecisionLogs(any(), any());
    }

    @Test
    void reads_the_whole_environment_when_no_api_restriction_is_given() throws Exception {
        when(metricsRepository.searchDecisionLogs(any(), any())).thenReturn(new LogResponse<>(0L, List.of()));

        service.searchDecisionLogs(CONTEXT, DecisionLogFilters.builder().decisionPointType("guardian").build(), new PageableImpl(1, 20));

        var query = ArgumentCaptor.forClass(DecisionLogQuery.class);
        verify(metricsRepository).searchDecisionLogs(any(), query.capture());
        assertThat(query.getValue().getApiIds()).isNull();
    }

    @Test
    void carries_the_scope_the_filters_and_the_page_into_the_query() throws Exception {
        when(metricsRepository.searchDecisionLogs(any(), any())).thenReturn(new LogResponse<>(0L, List.of()));

        service.searchDecisionLogs(
            CONTEXT,
            DecisionLogFilters.builder()
                .decisionPointType("guardian")
                .apiIds(Set.of("api-1", "api-2"))
                .applicationIds(Set.of("app-1"))
                .planIds(Set.of("plan-1"))
                .from(1000L)
                .to(2000L)
                .decisionPointIds(Set.of("prompt-guardian"))
                .checkpoints(Set.of("response.tool_calls"))
                .callers(Set.of("pep"))
                .outcomes(Set.of("DENY"))
                .enforcements(Set.of("DENY"))
                .verdicts(Set.of("prompt-injection"))
                .statuses(Set.of("success"))
                .subjectIds(Set.of("alice"))
                .actorIds(Set.of("booking-agent"))
                .actions(Set.of("flight_booking"))
                .resourceIds(Set.of("travel.flight_booking"))
                .caseIds(Set.of("case-1"))
                .requestIds(Set.of("req-1"))
                .traceIds(Set.of("trace-1"))
                .reasonContains("injection")
                .build(),
            new PageableImpl(3, 50)
        );

        var contextCaptor = ArgumentCaptor.forClass(QueryContext.class);
        var queryCaptor = ArgumentCaptor.forClass(DecisionLogQuery.class);
        verify(metricsRepository).searchDecisionLogs(contextCaptor.capture(), queryCaptor.capture());

        var query = queryCaptor.getValue();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(contextCaptor.getValue().placeholder()).containsEntry("orgId", "org-1").containsEntry("envId", "env-1");
            soft.assertThat(query.getDecisionPointType()).isEqualTo("guardian");
            soft.assertThat(query.getApiIds()).containsExactlyInAnyOrder("api-1", "api-2");
            soft.assertThat(query.getApplicationIds()).containsExactly("app-1");
            soft.assertThat(query.getPlanIds()).containsExactly("plan-1");
            soft.assertThat(query.getFrom()).isEqualTo(1000L);
            soft.assertThat(query.getTo()).isEqualTo(2000L);
            soft.assertThat(query.getDecisionPointIds()).containsExactly("prompt-guardian");
            soft.assertThat(query.getCheckpoints()).containsExactly("response.tool_calls");
            soft.assertThat(query.getCallers()).containsExactly("pep");
            soft.assertThat(query.getOutcomes()).containsExactly("DENY");
            soft.assertThat(query.getEnforcements()).containsExactly("DENY");
            soft.assertThat(query.getVerdicts()).containsExactly("prompt-injection");
            soft.assertThat(query.getStatuses()).containsExactly("success");
            soft.assertThat(query.getSubjectIds()).containsExactly("alice");
            soft.assertThat(query.getActorIds()).containsExactly("booking-agent");
            soft.assertThat(query.getActions()).containsExactly("flight_booking");
            soft.assertThat(query.getResourceIds()).containsExactly("travel.flight_booking");
            soft.assertThat(query.getCaseIds()).containsExactly("case-1");
            soft.assertThat(query.getRequestIds()).containsExactly("req-1");
            soft.assertThat(query.getTraceIds()).containsExactly("trace-1");
            soft.assertThat(query.getReasonContains()).isEqualTo("injection");
            soft.assertThat(query.getPage()).isEqualTo(3);
            soft.assertThat(query.getSize()).isEqualTo(50);
        });
    }

    @Test
    void carries_the_exclusions_into_the_query() throws Exception {
        when(metricsRepository.searchDecisionLogs(any(), any())).thenReturn(new LogResponse<>(0L, List.of()));

        service.searchDecisionLogs(
            CONTEXT,
            DecisionLogFilters.builder()
                .decisionPointType("guardian")
                .excludedApiIds(Set.of("api-3"))
                .excludedApplicationIds(Set.of("app-3"))
                .excludedDecisionPointIds(Set.of("noisy-guardian"))
                .excludedOutcomes(Set.of("ALLOW"))
                .build(),
            new PageableImpl(1, 20)
        );

        var queryCaptor = ArgumentCaptor.forClass(DecisionLogQuery.class);
        verify(metricsRepository).searchDecisionLogs(any(), queryCaptor.capture());

        var query = queryCaptor.getValue();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(query.getExcludedApiIds()).containsExactly("api-3");
            soft.assertThat(query.getExcludedApplicationIds()).containsExactly("app-3");
            soft.assertThat(query.getExcludedDecisionPointIds()).containsExactly("noisy-guardian");
            soft.assertThat(query.getExcludedOutcomes()).containsExactly("ALLOW");
        });
    }

    @Test
    void still_reads_the_index_when_the_caller_excludes_no_api() throws Exception {
        when(metricsRepository.searchDecisionLogs(any(), any())).thenReturn(new LogResponse<>(0L, List.of()));

        service.searchDecisionLogs(
            CONTEXT,
            DecisionLogFilters.builder().decisionPointType("guardian").excludedApiIds(Set.<String>of()).build(),
            new PageableImpl(1, 20)
        );

        // An empty exclusion rules nothing out, unlike an empty apiIds which short-circuits the read.
        var queryCaptor = ArgumentCaptor.forClass(DecisionLogQuery.class);
        verify(metricsRepository).searchDecisionLogs(any(), queryCaptor.capture());
        assertThat(queryCaptor.getValue().getExcludedApiIds()).isEmpty();
    }

    @Test
    void maps_the_carrier_record_onto_the_domain_projection() throws Exception {
        when(metricsRepository.searchDecisionLogs(any(), any())).thenReturn(new LogResponse<>(9L, List.of(carrierRecord())));

        var response = service.searchDecisionLogs(
            CONTEXT,
            DecisionLogFilters.builder().decisionPointType("guardian").build(),
            new PageableImpl(1, 20)
        );

        assertThat(response.total()).isEqualTo(9L);
        var decision = response.logs().getFirst();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(decision.timestamp()).isEqualTo(1000L);
            soft.assertThat(decision.gatewayId()).isEqualTo("gateway-1");
            soft.assertThat(decision.organizationId()).isEqualTo("org-1");
            soft.assertThat(decision.environmentId()).isEqualTo("env-1");
            soft.assertThat(decision.apiId()).isEqualTo("api-1");
            soft.assertThat(decision.planId()).isEqualTo("plan-1");
            soft.assertThat(decision.applicationId()).isEqualTo("app-1");
            soft.assertThat(decision.eventId()).isEqualTo("dec-1");
            soft.assertThat(decision.caseId()).isEqualTo("case-1");
            soft.assertThat(decision.batchId()).isEqualTo("batch-1");
            soft.assertThat(decision.phase()).isEqualTo("RESOLVED");
            soft.assertThat(decision.decisionPointType()).isEqualTo("guardian");
            soft.assertThat(decision.decisionPointId()).isEqualTo("prompt-guardian");
            soft.assertThat(decision.decisionPointVersion()).isEqualTo("gpt-4o-2024-11");
            soft.assertThat(decision.checkpoint()).isEqualTo("response.tool_calls");
            soft.assertThat(decision.caller()).isEqualTo("pep");
            soft.assertThat(decision.subjectType()).isEqualTo("User");
            soft.assertThat(decision.subjectId()).isEqualTo("alice");
            soft.assertThat(decision.actorType()).isEqualTo("AgentIdentity");
            soft.assertThat(decision.actorId()).isEqualTo("booking-agent");
            soft.assertThat(decision.action()).isEqualTo("flight_booking");
            soft.assertThat(decision.resourceType()).isEqualTo("MCPTool");
            soft.assertThat(decision.resourceId()).isEqualTo("travel.flight_booking");
            soft.assertThat(decision.argsHash()).isEqualTo("sha256:9c41f2a8e7b3");
            soft.assertThat(decision.outcome()).isEqualTo("INDETERMINATE");
            soft.assertThat(decision.enforced()).isEqualTo("ALLOW");
            soft.assertThat(decision.verdict()).isEqualTo("unsure");
            soft.assertThat(decision.indeterminateCause()).isEqualTo("TIMEOUT");
            soft.assertThat(decision.confidence()).isEqualTo(0.94d);
            soft.assertThat(decision.reasons()).containsExactly("Guardian did not answer within 250ms");
            soft.assertThat(decision.matchedRules()).containsExactly(new DecisionLog.MatchedRule("r1", "spend-threshold", "FORBID"));
            soft.assertThat(decision.transformed()).isTrue();
            soft.assertThat(decision.transformationType()).isEqualTo("REDACT");
            soft.assertThat(decision.requiredApprover()).isEqualTo("group:finance");
            soft.assertThat(decision.deciderType()).isEqualTo("User");
            soft.assertThat(decision.deciderId()).isEqualTo("frank");
            soft.assertThat(decision.channel()).isEqualTo("slack");
            soft.assertThat(decision.requestId()).isEqualTo("req-1");
            soft.assertThat(decision.traceId()).isEqualTo("trace-1");
            soft.assertThat(decision.conversationId()).isEqualTo("conv-1");
            soft.assertThat(decision.missionId()).isEqualTo("mission-1");
            soft.assertThat(decision.status()).isEqualTo("error");
            soft.assertThat(decision.errorType()).isEqualTo("TimeoutException");
            soft.assertThat(decision.durationNanos()).isEqualTo(4416541L);
            soft.assertThat(decision.waitedNanos()).isEqualTo(60000000000L);
            soft.assertThat(decision.additionalMetrics()).containsEntry("double_guardian_cost", 0.5d);
        });
    }

    @Test
    void leaves_a_field_the_family_does_not_write_null() throws Exception {
        when(metricsRepository.searchDecisionLogs(any(), any())).thenReturn(
            new LogResponse<>(
                1L,
                List.of(
                    io.gravitee.repository.log.v4.model.decision.DecisionLog.builder()
                        .eventId("dec-2")
                        .outcome("ALLOW")
                        .enforced("ALLOW")
                        .build()
                )
            )
        );

        var decision = service
            .searchDecisionLogs(CONTEXT, DecisionLogFilters.builder().decisionPointType("guardian").build(), new PageableImpl(1, 20))
            .logs()
            .getFirst();

        assertThat(decision.indeterminateCause()).isNull();
        assertThat(decision.requiredApprover()).isNull();
    }

    @Test
    void turns_a_repository_failure_into_a_technical_management_exception() throws Exception {
        when(metricsRepository.searchDecisionLogs(any(), any())).thenThrow(new AnalyticsException("index unavailable"));

        assertThatThrownBy(() ->
            service.searchDecisionLogs(CONTEXT, DecisionLogFilters.builder().decisionPointType("guardian").build(), new PageableImpl(1, 20))
        )
            .isInstanceOf(TechnicalManagementException.class)
            .hasMessageContaining("decision logs");
    }

    @Test
    void finds_one_record_by_event_id_within_the_api() throws Exception {
        when(metricsRepository.findDecisionLog(any(), any(), any())).thenReturn(
            Optional.of(
                io.gravitee.repository.log.v4.model.decision.DecisionLog.builder()
                    .eventId("dec-9")
                    .apiId("api-1")
                    .phase("REQUESTED")
                    .outcome("PENDING")
                    .enforced("SUSPEND")
                    .build()
            )
        );

        var decision = service.findDecisionLog(CONTEXT, "api-1", "dec-9");

        assertThat(decision).isPresent();
        // A by-id read is not restricted to settled records: an open approval is the one worth looking at.
        assertThat(decision.get().phase()).isEqualTo("REQUESTED");
        assertThat(decision.get().outcome()).isEqualTo("PENDING");
    }

    @Test
    void finds_nothing_when_no_record_carries_that_event_id() throws Exception {
        when(metricsRepository.findDecisionLog(any(), any(), any())).thenReturn(Optional.empty());

        assertThat(service.findDecisionLog(CONTEXT, "api-1", "missing")).isEmpty();
    }

    @Test
    void turns_a_failed_lookup_into_a_technical_management_exception() throws Exception {
        when(metricsRepository.findDecisionLog(any(), any(), any())).thenThrow(new AnalyticsException("index unavailable"));

        assertThatThrownBy(() -> service.findDecisionLog(CONTEXT, "api-1", "dec-9"))
            .isInstanceOf(TechnicalManagementException.class)
            .hasMessageContaining("dec-9");
    }

    private static io.gravitee.repository.log.v4.model.decision.DecisionLog carrierRecord() {
        return io.gravitee.repository.log.v4.model.decision.DecisionLog.builder()
            .timestamp(1000L)
            .gatewayId("gateway-1")
            .organizationId("org-1")
            .environmentId("env-1")
            .apiId("api-1")
            .planId("plan-1")
            .applicationId("app-1")
            .eventId("dec-1")
            .caseId("case-1")
            .batchId("batch-1")
            .phase("RESOLVED")
            .decisionPointType("guardian")
            .decisionPointId("prompt-guardian")
            .decisionPointVersion("gpt-4o-2024-11")
            .checkpoint("response.tool_calls")
            .caller("pep")
            .subjectType("User")
            .subjectId("alice")
            .actorType("AgentIdentity")
            .actorId("booking-agent")
            .action("flight_booking")
            .resourceType("MCPTool")
            .resourceId("travel.flight_booking")
            .argsHash("sha256:9c41f2a8e7b3")
            .outcome("INDETERMINATE")
            .enforced("ALLOW")
            .verdict("unsure")
            .indeterminateCause("TIMEOUT")
            .confidence(0.94d)
            .reasons(List.of("Guardian did not answer within 250ms"))
            .matchedRules(
                List.of(new io.gravitee.repository.log.v4.model.decision.DecisionLog.MatchedRule("r1", "spend-threshold", "FORBID"))
            )
            .transformed(true)
            .transformationType("REDACT")
            .requiredApprover("group:finance")
            .deciderType("User")
            .deciderId("frank")
            .channel("slack")
            .requestId("req-1")
            .traceId("trace-1")
            .conversationId("conv-1")
            .missionId("mission-1")
            .status("error")
            .errorType("TimeoutException")
            .durationNanos(4416541L)
            .waitedNanos(60000000000L)
            .additionalMetrics(Map.of("double_guardian_cost", 0.5d))
            .build();
    }
}
