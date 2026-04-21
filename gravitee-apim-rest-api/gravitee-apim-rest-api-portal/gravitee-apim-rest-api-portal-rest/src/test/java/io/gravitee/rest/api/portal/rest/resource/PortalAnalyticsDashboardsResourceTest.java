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
package io.gravitee.rest.api.portal.rest.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.dashboard.exception.DashboardNotFoundException;
import io.gravitee.apim.core.dashboard.model.Dashboard;
import io.gravitee.apim.core.dashboard.model.DashboardWidget;
import io.gravitee.apim.core.dashboard.use_case.GetDashboardUseCase;
import io.gravitee.apim.core.dashboard.use_case.ListDashboardsUseCase;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.portal.rest.model.AnalyticsDashboard;
import io.gravitee.rest.api.portal.rest.model.AnalyticsDashboardsResponse;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Story 2 — Portal backend: dashboard list and detail endpoints (see APIM-12689 implementation plan).
 *
 * @author GraviteeSource Team
 */
@DisplayName("Story 2 — Portal analytics dashboards")
public class PortalAnalyticsDashboardsResourceTest extends AbstractResourceTest {

    @Autowired
    private ListDashboardsUseCase listDashboardsUseCase;

    @Autowired
    private GetDashboardUseCase getDashboardUseCase;

    @Override
    protected String contextPath() {
        return "analytics/dashboards";
    }

    @BeforeEach
    void setUp() {
        resetAllMocks();
        reset(listDashboardsUseCase, getDashboardUseCase);
        when(
            parameterService.findAsBoolean(any(), eq(Key.PORTAL_NEXT_ANALYTICS_ENABLED), eq(ParameterReferenceType.ENVIRONMENT))
        ).thenReturn(true);
    }

    @Nested
    @DisplayName("Acceptance: GET /portal/environments/{envId}/analytics/dashboards returns paginated dashboard list")
    class PaginatedDashboardList {

        @Test
        @DisplayName("returns data, pagination metadata, and links; delegates to ListDashboardsUseCase")
        void returnsPaginatedList() {
            when(listDashboardsUseCase.execute(any())).thenReturn(
                new ListDashboardsUseCase.Output(
                    List.of(aDashboard("dashboard-1", "Dashboard 1"), aDashboard("dashboard-2", "Dashboard 2"))
                )
            );

            final var response = target().queryParam("page", 1).queryParam("size", 10).request().get();

            assertEquals(HttpStatusCode.OK_200, response.getStatus());

            final var dashboardsResponse = response.readEntity(AnalyticsDashboardsResponse.class);
            assertEquals(2, dashboardsResponse.getData().size());
            assertEquals("dashboard-1", dashboardsResponse.getData().get(0).getId());
            assertEquals("Dashboard 2", dashboardsResponse.getData().get(1).getName());
            assertNotNull(dashboardsResponse.getMetadata());
            assertNotNull(dashboardsResponse.getLinks());

            verify(listDashboardsUseCase).execute(any());
        }
    }

    @Nested
    @DisplayName("Acceptance: GET /portal/environments/{envId}/analytics/dashboards/{dashboardId} returns single dashboard")
    class SingleDashboard {

        @Test
        @DisplayName("returns dashboard body; delegates to GetDashboardUseCase")
        void returnsOneDashboard() {
            when(getDashboardUseCase.execute(any())).thenReturn(new GetDashboardUseCase.Output(aDashboard("dashboard-1", "Dashboard 1")));

            final var response = target().path("dashboard-1").request().get();

            assertEquals(HttpStatusCode.OK_200, response.getStatus());

            final var dashboard = response.readEntity(AnalyticsDashboard.class);
            assertEquals("dashboard-1", dashboard.getId());
            assertEquals("Dashboard 1", dashboard.getName());
            assertEquals("stats", dashboard.getWidgets().get(0).getType().getValue());

            verify(getDashboardUseCase).execute(any());
        }

        @Test
        @DisplayName("404 when dashboard does not exist")
        void notFoundWhenMissing() {
            when(getDashboardUseCase.execute(any())).thenThrow(new DashboardNotFoundException("missing-dashboard"));

            final var response = target().path("missing-dashboard").request().get();

            assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
        }

        @Test
        @DisplayName("404 when dashboard belongs to another environment")
        void notFoundWhenWrongEnvironment() {
            when(getDashboardUseCase.execute(any())).thenReturn(
                new GetDashboardUseCase.Output(aDashboard("dashboard-1", "Dashboard 1").toBuilder().environmentId("OTHER").build())
            );

            final var response = target().path("dashboard-1").request().get();

            assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
        }
    }

    @Nested
    @DisplayName("Acceptance: endpoints gated by analytics-enabled setting")
    class GatedByAnalyticsSetting {

        @BeforeEach
        void analyticsDisabled() {
            when(
                parameterService.findAsBoolean(any(), eq(Key.PORTAL_NEXT_ANALYTICS_ENABLED), eq(ParameterReferenceType.ENVIRONMENT))
            ).thenReturn(false);
        }

        @Test
        @DisplayName("403 on dashboard list when PORTAL_NEXT_ANALYTICS_ENABLED is false")
        void forbiddenOnList() {
            final var response = target().request().get();

            assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
            verify(listDashboardsUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("403 on dashboard by id when PORTAL_NEXT_ANALYTICS_ENABLED is false")
        void forbiddenOnGetById() {
            final var response = target().path("dashboard-1").request().get();

            assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
            verify(getDashboardUseCase, never()).execute(any());
        }
    }

    private static Dashboard aDashboard(String id, String name) {
        return Dashboard.builder()
            .id(id)
            .environmentId(GraviteeContext.getExecutionContext().getEnvironmentId())
            .name(name)
            .createdBy("user-1")
            .createdAt(ZonedDateTime.parse("2025-10-07T06:50:30Z"))
            .lastModified(ZonedDateTime.parse("2025-12-07T11:35:30Z"))
            .labels(Map.of("team", "apim"))
            .widgets(
                List.of(
                    DashboardWidget.builder()
                        .id("widget-1")
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
                                .metrics(
                                    List.of(
                                        DashboardWidget.MetricRequest.builder().name("HTTP_REQUESTS").measures(List.of("COUNT")).build()
                                    )
                                )
                                .build()
                        )
                        .build()
                )
            )
            .build();
    }
}
