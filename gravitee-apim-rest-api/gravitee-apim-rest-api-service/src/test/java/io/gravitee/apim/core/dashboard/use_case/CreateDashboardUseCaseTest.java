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

import inmemory.DashboardCrudServiceInMemory;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryValidator;
import io.gravitee.apim.core.analytics_engine.exception.InvalidQueryException;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsDefinitionQueryService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.dashboard.domain_service.DashboardDomainService;
import io.gravitee.apim.core.dashboard.model.Dashboard;
import io.gravitee.apim.core.dashboard.model.DashboardWidget;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateDashboardUseCaseTest {

    private DashboardCrudServiceInMemory dashboardCrudServiceInMemory;
    private CreateDashboardUseCase useCase;

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
                    .timeRange(DashboardWidget.TimeRange.builder().from("2025-10-07T06:50:30Z").to("2025-12-07T11:35:30Z").build())
                    .metrics(List.of(DashboardWidget.MetricRequest.builder().name("HTTP_REQUESTS").measures(List.of("COUNT")).build()))
                    .build()
            )
            .build();
    }

    private static DashboardWidget widgetWithFacetsRequestEmptyBy() {
        return DashboardWidget.builder()
            .id("2")
            .title("Facets")
            .type("doughnut")
            .layout(DashboardWidget.Layout.builder().cols(1).rows(1).x(0).y(0).build())
            .request(
                DashboardWidget.Request.builder()
                    .type("facets")
                    .timeRange(DashboardWidget.TimeRange.builder().from("2025-10-07T06:50:30Z").to("2025-12-07T11:35:30Z").build())
                    .by(List.of())
                    .metrics(List.of(DashboardWidget.MetricRequest.builder().name("HTTP_REQUESTS").measures(List.of("COUNT")).build()))
                    .build()
            )
            .build();
    }

    @BeforeEach
    void setUp() {
        dashboardCrudServiceInMemory = new DashboardCrudServiceInMemory();
        AnalyticsDefinitionQueryService definitionQueryService = new AnalyticsDefinitionYAMLQueryService();
        var analyticsQueryValidator = new AnalyticsQueryValidator(definitionQueryService);
        var dashboardDomainService = new DashboardDomainService(dashboardCrudServiceInMemory, analyticsQueryValidator);
        useCase = new CreateDashboardUseCase(dashboardDomainService);
    }

    @AfterEach
    void tearDown() {
        dashboardCrudServiceInMemory.reset();
    }

    @Nested
    class HappyPath {

        @Test
        void should_create_dashboard_when_widgets_are_valid() {
            var dashboard = Dashboard.builder().name("My Dashboard").widgets(List.of(widgetWithMeasuresRequest())).build();

            var output = useCase.execute(new CreateDashboardUseCase.Input(dashboard, auditInfo()));

            assertThat(output.dashboard()).isNotNull();
            assertThat(output.dashboard().getName()).isEqualTo("My Dashboard");
            assertThat(output.dashboard().getId()).isNotBlank();
            assertThat(dashboardCrudServiceInMemory.storage()).hasSize(1);
            assertThat(dashboardCrudServiceInMemory.storage().getFirst().getName()).isEqualTo("My Dashboard");
        }

        @Test
        void should_create_dashboard_when_no_widgets() {
            var dashboard = Dashboard.builder().name("Empty").widgets(List.of()).build();

            var output = useCase.execute(new CreateDashboardUseCase.Input(dashboard, auditInfo()));

            assertThat(output.dashboard().getName()).isEqualTo("Empty");
            assertThat(dashboardCrudServiceInMemory.storage()).hasSize(1);
        }

        @Test
        void should_create_dashboard_when_widgets_null() {
            var dashboard = Dashboard.builder().name("No widgets").widgets(null).build();

            var output = useCase.execute(new CreateDashboardUseCase.Input(dashboard, auditInfo()));

            assertThat(output.dashboard().getName()).isEqualTo("No widgets");
            assertThat(dashboardCrudServiceInMemory.storage()).hasSize(1);
        }
    }

    @Nested
    class Validation {

        @Test
        void should_throw_when_facets_request_has_empty_by() {
            var dashboard = Dashboard.builder().name("Bad").widgets(List.of(widgetWithFacetsRequestEmptyBy())).build();

            assertThatThrownBy(() -> useCase.execute(new CreateDashboardUseCase.Input(dashboard, auditInfo())))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("by clause");
            assertThat(dashboardCrudServiceInMemory.storage()).isEmpty();
        }
    }
}
