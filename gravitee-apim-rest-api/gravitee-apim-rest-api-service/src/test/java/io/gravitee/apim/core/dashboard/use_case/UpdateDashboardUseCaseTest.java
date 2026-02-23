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
package io.gravitee.apim.core.dashboard.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.AuditCrudServiceInMemory;
import inmemory.DashboardCrudServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryValidator;
import io.gravitee.apim.core.analytics_engine.exception.InvalidQueryException;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.dashboard.domain_service.DashboardDomainService;
import io.gravitee.apim.core.dashboard.exception.DashboardNotFoundException;
import io.gravitee.apim.core.dashboard.model.Dashboard;
import io.gravitee.apim.core.dashboard.model.DashboardWidget;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UpdateDashboardUseCaseTest {

    private static final String DASHBOARD_ID = "dashboard-id";

    private DashboardCrudServiceInMemory dashboardCrudServiceInMemory;
    private AuditCrudServiceInMemory auditCrudServiceInMemory;
    private UpdateDashboardUseCase useCase;

    private static AuditInfo auditInfo() {
        return AuditInfo.builder()
            .organizationId("org-1")
            .environmentId("env-1")
            .actor(io.gravitee.apim.core.audit.model.AuditActor.builder().userId("user-1").build())
            .build();
    }

    private static DashboardWidget widgetWithMeasuresRequest() {
        return DashboardWidget.builder()
            .id("1")
            .title("Requests")
            .type("stats")
            .layout(DashboardWidget.Layout.builder().cols(1).rows(1).x(0).y(0).build())
            .request(
                DashboardWidget.Request.builder()
                    .type("measures")
                    .timeRange(
                        DashboardWidget.TimeRange.builder()
                            .from(Instant.parse("2025-10-07T06:50:30Z"))
                            .to(Instant.parse("2025-12-07T11:35:30Z"))
                            .build()
                    )
                    .metrics(List.of(DashboardWidget.MetricRequest.builder().name("HTTP_REQUESTS").measures(List.of("COUNT")).build()))
                    .build()
            )
            .build();
    }

    private static DashboardWidget widgetWithInvalidTimeRange() {
        return DashboardWidget.builder()
            .id("2")
            .title("Bad range")
            .type("stats")
            .layout(DashboardWidget.Layout.builder().cols(1).rows(1).x(0).y(0).build())
            .request(
                DashboardWidget.Request.builder()
                    .type("measures")
                    .timeRange(
                        DashboardWidget.TimeRange.builder()
                            .from(Instant.parse("2025-12-07T11:35:30Z"))
                            .to(Instant.parse("2025-10-07T06:50:30Z"))
                            .build()
                    )
                    .metrics(List.of(DashboardWidget.MetricRequest.builder().name("HTTP_REQUESTS").measures(List.of("COUNT")).build()))
                    .build()
            )
            .build();
    }

    @BeforeEach
    void setUp() {
        dashboardCrudServiceInMemory = new DashboardCrudServiceInMemory();
        auditCrudServiceInMemory = new inmemory.AuditCrudServiceInMemory();
        var auditDomainService = new AuditDomainService(
            auditCrudServiceInMemory,
            new UserCrudServiceInMemory(),
            new JacksonJsonDiffProcessor()
        );
        var definitionQueryService = new AnalyticsDefinitionYAMLQueryService();
        var analyticsQueryValidator = new AnalyticsQueryValidator(definitionQueryService);
        var dashboardDomainService = new DashboardDomainService(dashboardCrudServiceInMemory, analyticsQueryValidator);
        useCase = new UpdateDashboardUseCase(dashboardDomainService, auditDomainService);
    }

    @AfterEach
    void tearDown() {
        dashboardCrudServiceInMemory.reset();
    }

    @Nested
    class HappyPath {

        @Test
        void should_update_dashboard_when_widgets_are_valid() {
            var existing = Dashboard.builder().id(DASHBOARD_ID).organizationId("org-1").name("Old").widgets(List.of()).build();
            dashboardCrudServiceInMemory.initWith(List.of(existing));

            var updatedDashboard = Dashboard.builder().name("Updated").widgets(List.of(widgetWithMeasuresRequest())).build();
            var output = useCase.execute(new UpdateDashboardUseCase.Input(DASHBOARD_ID, updatedDashboard, auditInfo()));

            assertThat(output.dashboard().getName()).isEqualTo("Updated");
            assertThat(dashboardCrudServiceInMemory.storage()).hasSize(1);
            assertThat(dashboardCrudServiceInMemory.storage().getFirst().getName()).isEqualTo("Updated");
            assertThat(auditCrudServiceInMemory.storage()).hasSize(1);
        }

        @Test
        void should_throw_when_dashboard_not_found() {
            var updatedDashboard = Dashboard.builder().name("Updated").widgets(List.of()).build();

            assertThatThrownBy(() ->
                useCase.execute(new UpdateDashboardUseCase.Input(DASHBOARD_ID, updatedDashboard, auditInfo()))
            ).isInstanceOf(DashboardNotFoundException.class);
        }
    }

    @Nested
    class Validation {

        @Test
        void should_throw_when_time_range_invalid() {
            var existing = Dashboard.builder().id(DASHBOARD_ID).organizationId("org-1").name("Old").widgets(List.of()).build();
            dashboardCrudServiceInMemory.initWith(List.of(existing));
            var updatedDashboard = Dashboard.builder().name("Updated").widgets(List.of(widgetWithInvalidTimeRange())).build();

            assertThatThrownBy(() -> useCase.execute(new UpdateDashboardUseCase.Input(DASHBOARD_ID, updatedDashboard, auditInfo())))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("upper bound");
            assertThat(dashboardCrudServiceInMemory.storage().getFirst().getName()).isEqualTo("Old");
        }
    }
}
