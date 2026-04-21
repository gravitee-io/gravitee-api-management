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
package io.gravitee.rest.api.portal.rest.fixture;

import io.gravitee.apim.core.dashboard.model.Dashboard;
import io.gravitee.apim.core.dashboard.model.DashboardWidget;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * Shared test fixtures for the analytics dashboard endpoints.
 */
public final class PortalAnalyticsDashboardFixtures {

    private PortalAnalyticsDashboardFixtures() {}

    public static Dashboard aDashboard(String id, String name) {
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
