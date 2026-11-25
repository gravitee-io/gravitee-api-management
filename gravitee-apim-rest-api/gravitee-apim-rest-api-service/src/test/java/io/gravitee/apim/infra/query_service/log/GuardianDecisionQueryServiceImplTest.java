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
package io.gravitee.apim.infra.query_service.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.log.model.GuardianDecision;
import io.gravitee.apim.core.log.model.GuardianDecisionFilters;
import io.gravitee.apim.core.log.query_service.GuardianDecisionQueryService;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.MetricsRepository;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.guardian.GuardianDecisionLog;
import io.gravitee.repository.log.v4.model.guardian.GuardianDecisionLogQuery;
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
class GuardianDecisionQueryServiceImplTest {

    private static final ExecutionContext CONTEXT = new ExecutionContext("org-1", "env-1");

    private MetricsRepository metricsRepository;
    private GuardianDecisionQueryService service;

    @BeforeEach
    void setUp() {
        metricsRepository = mock(MetricsRepository.class);
        service = new GuardianDecisionQueryServiceImpl(metricsRepository);
    }

    @Test
    void does_not_query_the_index_when_no_api_is_in_scope() throws Exception {
        var response = service.search(CONTEXT, GuardianDecisionFilters.builder().apiIds(Set.of()).build(), new PageableImpl(1, 20));

        assertThat(response.total()).isZero();
        assertThat(response.logs()).isEmpty();
        verify(metricsRepository, never()).searchGuardianDecisionLogs(any(), any());
    }

    @Test
    void carries_the_environment_every_filter_and_the_page_into_the_query() throws Exception {
        when(metricsRepository.searchGuardianDecisionLogs(any(), any())).thenReturn(new LogResponse<>(0L, List.of()));
        var filters = GuardianDecisionFilters.builder()
            .apiIds(Set.of("api-1"))
            .guardianIds(Set.of("guardian-1"))
            .outcomes(Set.of("DENY"))
            .from(1000L)
            .to(2000L)
            .requestIds(Set.of("req-1"))
            .requestIdContains("req")
            .build();

        service.search(CONTEXT, filters, new PageableImpl(3, 50));

        var contextCaptor = ArgumentCaptor.forClass(QueryContext.class);
        var queryCaptor = ArgumentCaptor.forClass(GuardianDecisionLogQuery.class);
        verify(metricsRepository).searchGuardianDecisionLogs(contextCaptor.capture(), queryCaptor.capture());
        var query = queryCaptor.getValue();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(contextCaptor.getValue().placeholder()).containsEntry("orgId", "org-1").containsEntry("envId", "env-1");
            soft.assertThat(query.getApiIds()).containsExactly("api-1");
            soft.assertThat(query.getGuardianIds()).containsExactly("guardian-1");
            soft.assertThat(query.getOutcomes()).containsExactly("DENY");
            soft.assertThat(query.getFrom()).isEqualTo(1000L);
            soft.assertThat(query.getTo()).isEqualTo(2000L);
            soft.assertThat(query.getRequestIds()).containsExactly("req-1");
            soft.assertThat(query.getRequestIdContains()).isEqualTo("req");
            soft.assertThat(query.getPage()).isEqualTo(3);
            soft.assertThat(query.getSize()).isEqualTo(50);
        });
    }

    @Test
    void reads_every_api_of_the_environment_when_none_is_given() throws Exception {
        when(metricsRepository.searchGuardianDecisionLogs(any(), any())).thenReturn(new LogResponse<>(0L, List.of()));

        service.search(CONTEXT, GuardianDecisionFilters.builder().build(), new PageableImpl(1, 20));

        var queryCaptor = ArgumentCaptor.forClass(GuardianDecisionLogQuery.class);
        verify(metricsRepository).searchGuardianDecisionLogs(any(), queryCaptor.capture());
        assertThat(queryCaptor.getValue().getApiIds()).isNull();
    }

    @Test
    void maps_every_field_of_the_stored_decision() throws Exception {
        when(metricsRepository.searchGuardianDecisionLogs(any(), any())).thenReturn(new LogResponse<>(9L, List.of(aStoredDecision())));

        var response = service.search(CONTEXT, GuardianDecisionFilters.builder().build(), new PageableImpl(1, 20));

        assertThat(response.total()).isEqualTo(9L);
        assertThat(response.logs()).containsExactly(theDomainDecision());
    }

    @Test
    void finds_one_decision_in_the_environment() throws Exception {
        when(metricsRepository.findGuardianDecisionLog(any(), eq("evt-1"))).thenReturn(Optional.of(aStoredDecision()));

        assertThat(service.findById(CONTEXT, "evt-1")).contains(theDomainDecision());

        var contextCaptor = ArgumentCaptor.forClass(QueryContext.class);
        verify(metricsRepository).findGuardianDecisionLog(contextCaptor.capture(), eq("evt-1"));
        assertThat(contextCaptor.getValue().placeholder()).containsEntry("orgId", "org-1").containsEntry("envId", "env-1");
    }

    @Test
    void fails_with_a_technical_error_when_the_search_fails() throws Exception {
        when(metricsRepository.searchGuardianDecisionLogs(any(), any())).thenThrow(new AnalyticsException("boom"));

        assertThatThrownBy(() -> service.search(CONTEXT, GuardianDecisionFilters.builder().build(), new PageableImpl(1, 20))).isInstanceOf(
            TechnicalManagementException.class
        );
    }

    @Test
    void fails_with_a_technical_error_when_the_lookup_fails() throws Exception {
        when(metricsRepository.findGuardianDecisionLog(any(), any())).thenThrow(new AnalyticsException("boom"));

        assertThatThrownBy(() -> service.findById(CONTEXT, "evt-1")).isInstanceOf(TechnicalManagementException.class);
    }

    private static GuardianDecisionLog aStoredDecision() {
        return GuardianDecisionLog.builder()
            .eventId("evt-1")
            .timestamp(1000L)
            .organizationId("org-1")
            .environmentId("env-1")
            .gatewayId("gw-1")
            .apiId("api-1")
            .planId("plan-1")
            .applicationId("app-1")
            .guardianId("guardian-1")
            .guardianRevision("3")
            .checkpoint("request.llm-proxy")
            .caller("Guardian 1")
            .requestId("req-1")
            .traceId("trace-1")
            .outcome("DENY")
            .enforced("DENY")
            .verdict("GUARDRAIL_BLOCKED")
            .indeterminateCause(null)
            .errorType(null)
            .status("success")
            .reasons(List.of("zz: BLOCK — Blocked by guardrail"))
            .matchedRules(
                List.of(
                    new GuardianDecisionLog.MatchedRule(null, null, null, "DENY", Map.of("guardrail", "zz", "location", "messages[0].user"))
                )
            )
            .transformed(null)
            .transformationType(null)
            .durationNanos(4200L)
            .subjectType("user")
            .subjectId("alice")
            .actorType("agent")
            .actorId("app-1")
            .additionalMetrics(Map.of("keyword_custom_tag", "a"))
            .build();
    }

    private static GuardianDecision theDomainDecision() {
        return GuardianDecision.builder()
            .id("evt-1")
            .timestamp(1000L)
            .organizationId("org-1")
            .environmentId("env-1")
            .gatewayId("gw-1")
            .apiId("api-1")
            .planId("plan-1")
            .applicationId("app-1")
            .guardianId("guardian-1")
            .guardianRevision("3")
            .checkpoint("request.llm-proxy")
            .caller("Guardian 1")
            .requestId("req-1")
            .traceId("trace-1")
            .outcome("DENY")
            .enforced("DENY")
            .verdict("GUARDRAIL_BLOCKED")
            .status("success")
            .reasons(List.of("zz: BLOCK — Blocked by guardrail"))
            .matchedRules(
                List.of(
                    new GuardianDecision.MatchedRule(null, null, null, "DENY", Map.of("guardrail", "zz", "location", "messages[0].user"))
                )
            )
            .durationNanos(4200L)
            .subjectType("user")
            .subjectId("alice")
            .actorType("agent")
            .actorId("app-1")
            .additionalMetrics(Map.of("keyword_custom_tag", "a"))
            .build();
    }
}
