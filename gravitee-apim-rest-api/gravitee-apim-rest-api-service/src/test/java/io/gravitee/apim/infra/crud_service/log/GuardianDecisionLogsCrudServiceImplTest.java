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

import io.gravitee.apim.core.log.crud_service.GuardianDecisionLogsCrudService;
import io.gravitee.apim.core.log.model.GuardianDecisionLog;
import io.gravitee.apim.core.log.model.GuardianDecisionLogFilters;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.MetricsRepository;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.guardian.GuardianDecisionLogQuery;
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
class GuardianDecisionLogsCrudServiceImplTest {

    private static final ExecutionContext CONTEXT = new ExecutionContext("org-1", "env-1");

    private MetricsRepository metricsRepository;
    private GuardianDecisionLogsCrudService service;

    @BeforeEach
    void setUp() {
        metricsRepository = mock(MetricsRepository.class);
        service = new GuardianDecisionLogsCrudServiceImpl(metricsRepository);
    }

    @Test
    void does_not_query_the_index_when_no_api_is_in_scope() throws Exception {
        var response = service.searchDecisionLogs(
            CONTEXT,
            GuardianDecisionLogFilters.builder().apiIds(Set.of()).build(),
            new PageableImpl(1, 20)
        );

        assertThat(response.total()).isZero();
        assertThat(response.logs()).isEmpty();
        verify(metricsRepository, never()).searchGuardianDecisionLogs(any(), any());
    }

    @Test
    void carries_the_scope_the_filters_the_range_and_the_page_into_the_query() throws Exception {
        when(metricsRepository.searchGuardianDecisionLogs(any(), any())).thenReturn(new LogResponse<>(0L, List.of()));

        service.searchDecisionLogs(
            CONTEXT,
            GuardianDecisionLogFilters.builder()
                .apiIds(Set.of("api-1", "api-2"))
                .from(1000L)
                .to(2000L)
                .guardianIds(Set.of("guardian-a"))
                .outcomes(Set.of("DENY"))
                .text("aws")
                .textApiIds(Set.of("api-2"))
                .build(),
            new PageableImpl(3, 50)
        );

        var contextCaptor = ArgumentCaptor.forClass(QueryContext.class);
        var queryCaptor = ArgumentCaptor.forClass(GuardianDecisionLogQuery.class);
        verify(metricsRepository).searchGuardianDecisionLogs(contextCaptor.capture(), queryCaptor.capture());

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(contextCaptor.getValue().placeholder()).containsEntry("orgId", "org-1").containsEntry("envId", "env-1");
            soft.assertThat(queryCaptor.getValue().getApiIds()).containsExactlyInAnyOrder("api-1", "api-2");
            soft.assertThat(queryCaptor.getValue().getFrom()).isEqualTo(1000L);
            soft.assertThat(queryCaptor.getValue().getTo()).isEqualTo(2000L);
            soft.assertThat(queryCaptor.getValue().getGuardianIds()).containsExactly("guardian-a");
            soft.assertThat(queryCaptor.getValue().getOutcomes()).containsExactly("DENY");
            soft.assertThat(queryCaptor.getValue().getText()).isEqualTo("aws");
            soft.assertThat(queryCaptor.getValue().getTextApiIds()).containsExactly("api-2");
            soft.assertThat(queryCaptor.getValue().getPage()).isEqualTo(3);
            soft.assertThat(queryCaptor.getValue().getSize()).isEqualTo(50);
        });
    }

    @Test
    void maps_every_field_of_a_decision() throws Exception {
        when(metricsRepository.searchGuardianDecisionLogs(any(), any())).thenReturn(new LogResponse<>(9L, List.of(aStoredDecision())));

        var response = service.searchDecisionLogs(
            CONTEXT,
            GuardianDecisionLogFilters.builder().apiIds(Set.of("api-1")).build(),
            new PageableImpl(1, 20)
        );

        assertThat(response.total()).isEqualTo(9L);
        assertThat(response.logs()).containsExactly(theSameDecision());
    }

    @Test
    void turns_a_failed_search_into_a_technical_error() throws Exception {
        when(metricsRepository.searchGuardianDecisionLogs(any(), any())).thenThrow(new AnalyticsException("down"));

        assertThatThrownBy(() ->
            service.searchDecisionLogs(
                CONTEXT,
                GuardianDecisionLogFilters.builder().apiIds(Set.of("api-1")).build(),
                new PageableImpl(1, 20)
            )
        ).isInstanceOf(TechnicalManagementException.class);
    }

    private static io.gravitee.repository.log.v4.model.guardian.GuardianDecisionLog aStoredDecision() {
        return io.gravitee.repository.log.v4.model.guardian.GuardianDecisionLog.builder()
            .eventId("evt-1")
            .timestamp(1000L)
            .apiId("api-1")
            .requestId("req-1")
            .guardianId("guardian-a")
            .revision("3")
            .checkpoint("response.llm-proxy")
            .outcome("TRANSFORM")
            .enforced("TRANSFORM")
            .transformationType("REDACT")
            .verdict("REDACT")
            .reasons(List.of("Redact PII: REDACT — 1 passage"))
            .findings(
                List.of(
                    new io.gravitee.repository.log.v4.model.guardian.GuardianDecisionLog.Finding(
                        "Redact PII",
                        "EMAIL",
                        "$.choices[0].message.content",
                        "TRANSFORM"
                    )
                )
            )
            .durationNanos(9_100_000L)
            .build();
    }

    private static GuardianDecisionLog theSameDecision() {
        return GuardianDecisionLog.builder()
            .eventId("evt-1")
            .timestamp(1000L)
            .apiId("api-1")
            .requestId("req-1")
            .guardianId("guardian-a")
            .revision("3")
            .checkpoint("response.llm-proxy")
            .outcome("TRANSFORM")
            .enforced("TRANSFORM")
            .transformationType("REDACT")
            .verdict("REDACT")
            .reasons(List.of("Redact PII: REDACT — 1 passage"))
            .findings(List.of(new GuardianDecisionLog.Finding("Redact PII", "EMAIL", "$.choices[0].message.content", "TRANSFORM")))
            .durationNanos(9_100_000L)
            .build();
    }
}
