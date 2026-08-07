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

import io.gravitee.apim.core.log.crud_service.AuthzDecisionLogsCrudService;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.MetricsRepository;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.authz.AuthzDecisionLogQuery;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AuthzDecisionLogsCrudServiceImplTest {

    private static final ExecutionContext CONTEXT = new ExecutionContext("org-1", "env-1");

    private MetricsRepository metricsRepository;
    private AuthzDecisionLogsCrudService service;

    @BeforeEach
    void setUp() {
        metricsRepository = mock(MetricsRepository.class);
        service = new AuthzDecisionLogsCrudServiceImpl(metricsRepository);
    }

    @Test
    void does_not_query_the_index_when_no_api_is_in_scope() throws Exception {
        var response = service.searchDecisionLogs(CONTEXT, Set.of(), null, null, new PageableImpl(1, 20));

        assertThat(response.total()).isZero();
        assertThat(response.logs()).isEmpty();
        verify(metricsRepository, never()).searchAuthzDecisionLogs(any(), any());
    }

    @Test
    void carries_the_scope_the_range_and_the_page_into_the_query() throws Exception {
        when(metricsRepository.searchAuthzDecisionLogs(any(), any())).thenReturn(new LogResponse<>(0L, List.of()));

        service.searchDecisionLogs(CONTEXT, Set.of("api-1", "api-2"), 1000L, 2000L, new PageableImpl(3, 50));

        var contextCaptor = ArgumentCaptor.forClass(QueryContext.class);
        var queryCaptor = ArgumentCaptor.forClass(AuthzDecisionLogQuery.class);
        verify(metricsRepository).searchAuthzDecisionLogs(contextCaptor.capture(), queryCaptor.capture());

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(contextCaptor.getValue().placeholder()).containsEntry("orgId", "org-1").containsEntry("envId", "env-1");
            soft.assertThat(queryCaptor.getValue().getApiIds()).containsExactlyInAnyOrder("api-1", "api-2");
            soft.assertThat(queryCaptor.getValue().getFrom()).isEqualTo(1000L);
            soft.assertThat(queryCaptor.getValue().getTo()).isEqualTo(2000L);
            soft.assertThat(queryCaptor.getValue().getPage()).isEqualTo(3);
            soft.assertThat(queryCaptor.getValue().getSize()).isEqualTo(50);
        });
    }

    @Test
    void maps_the_carrier_record_onto_the_domain_projection() throws Exception {
        when(metricsRepository.searchAuthzDecisionLogs(any(), any())).thenReturn(
            new LogResponse<>(
                9L,
                List.of(
                    io.gravitee.repository.log.v4.model.authz.AuthzDecisionLog.builder()
                        .eventId("evt-1")
                        .timestamp(1000L)
                        .apiId("api-1")
                        .organizationId("org-1")
                        .environmentId("env-1")
                        .gatewayId("gateway-1")
                        .requestId("req-1")
                        .operation("evaluate")
                        .status("success")
                        .caller("pep")
                        .targetPdpId("pdp-1")
                        .policyGeneration(7L)
                        .decision("PERMIT")
                        .matchedPolicyNames(List.of("allow-readers"))
                        .reasons(List.of("Permitted by policy 'allow-readers'"))
                        .subjectType("User")
                        .subjectId("alice")
                        .action("read")
                        .resourceType("Document")
                        .resourceId("doc-1")
                        .batchId("batch-1")
                        .batchIndex(1)
                        .batchSize(2)
                        .searchType("subject")
                        .resultCount(12)
                        .durationNanos(4200L)
                        .build()
                )
            )
        );

        var response = service.searchDecisionLogs(CONTEXT, Set.of("api-1"), null, null, new PageableImpl(1, 20));

        assertThat(response.total()).isEqualTo(9L);
        var decision = response.logs().getFirst();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(decision.eventId()).isEqualTo("evt-1");
            soft.assertThat(decision.timestamp()).isEqualTo(1000L);
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
            soft.assertThat(decision.matchedPolicyNames()).containsExactly("allow-readers");
            soft.assertThat(decision.reasons()).containsExactly("Permitted by policy 'allow-readers'");
            soft.assertThat(decision.subjectType()).isEqualTo("User");
            soft.assertThat(decision.subjectId()).isEqualTo("alice");
            soft.assertThat(decision.action()).isEqualTo("read");
            soft.assertThat(decision.resourceType()).isEqualTo("Document");
            soft.assertThat(decision.resourceId()).isEqualTo("doc-1");
            soft.assertThat(decision.batchId()).isEqualTo("batch-1");
            soft.assertThat(decision.batchIndex()).isEqualTo(1);
            soft.assertThat(decision.batchSize()).isEqualTo(2);
            soft.assertThat(decision.searchType()).isEqualTo("subject");
            soft.assertThat(decision.resultCount()).isEqualTo(12);
            soft.assertThat(decision.durationNanos()).isEqualTo(4200L);
        });
    }

    @Test
    void turns_a_repository_failure_into_a_technical_management_exception() throws Exception {
        when(metricsRepository.searchAuthzDecisionLogs(any(), any())).thenThrow(new AnalyticsException("index unavailable"));

        assertThatThrownBy(() -> service.searchDecisionLogs(CONTEXT, Set.of("api-1"), null, null, new PageableImpl(1, 20)))
            .isInstanceOf(TechnicalManagementException.class)
            .hasMessageContaining("authz decision logs");
    }
}
