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
package fixtures;

import io.gravitee.apim.core.dashboard.model.Dashboard;
import io.gravitee.apim.core.dashboard.model.DashboardWidget;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.ArrayFilter;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.CreateUpdateDashboard;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FacetName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Filter;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FilterName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MeasureName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MetricName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MetricRequest;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.NumberFilter;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Operator;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.StringFilter;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.TimeRange;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Widget;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.WidgetLayout;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.WidgetRequest;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.WidgetType;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DashboardFixtures {

    private static final OffsetDateTime DEFAULT_FROM = OffsetDateTime.parse("2025-10-07T06:50:30Z");
    private static final OffsetDateTime DEFAULT_TO = OffsetDateTime.parse("2025-12-07T11:35:30Z");

    private DashboardFixtures() {}

    public static TimeRange defaultTimeRange() {
        return new TimeRange().from(DEFAULT_FROM).to(DEFAULT_TO);
    }

    public static WidgetRequest validMeasuresRequest() {
        return new WidgetRequest()
            .type(WidgetRequest.TypeEnum.MEASURES)
            .timeRange(defaultTimeRange())
            .metrics(List.of(new MetricRequest().name(MetricName.HTTP_REQUESTS).measures(List.of(MeasureName.COUNT))));
    }

    public static WidgetRequest validFacetsRequest(FacetName by, Integer limit) {
        var request = new WidgetRequest()
            .type(WidgetRequest.TypeEnum.FACETS)
            .timeRange(defaultTimeRange())
            .by(List.of(by))
            .metrics(List.of(new MetricRequest().name(MetricName.HTTP_REQUESTS).measures(List.of(MeasureName.COUNT))));
        if (limit != null) {
            request.setLimit(limit);
        }
        return request;
    }

    public static CreateUpdateDashboard aCreateDashboard() {
        var createDashboard = new CreateUpdateDashboard();
        createDashboard.setName("My Dashboard");
        createDashboard.setLabels(Map.of("gravitee_io/team", "apim"));
        createDashboard.setWidgets(
            List.of(
                new Widget()
                    .id("1")
                    .title("Requests")
                    .type(WidgetType.STATS)
                    .layout(new WidgetLayout().cols(1).rows(1).x(0).y(0))
                    .request(validMeasuresRequest()),
                new Widget()
                    .id("2")
                    .title("HTTP Statuses")
                    .type(WidgetType.DOUGHNUT)
                    .layout(new WidgetLayout().cols(1).rows(2).x(0).y(1))
                    .request(validFacetsRequest(FacetName.HTTP_STATUS_CODE_GROUP, null)),
                new Widget()
                    .id("3")
                    .title("Top APIs")
                    .type(WidgetType.TIME_SERIES_LINE)
                    .layout(new WidgetLayout().cols(3).rows(2).x(1).y(1))
                    .request(validFacetsRequest(FacetName.API, 5))
            )
        );
        return createDashboard;
    }

    public static CreateUpdateDashboard createDashboardWithOneWidget(WidgetRequest widgetRequest) {
        var createDashboard = new CreateUpdateDashboard();
        createDashboard.setName("Validation Test Dashboard");
        createDashboard.setWidgets(
            List.of(
                new Widget()
                    .id("1")
                    .title("Widget")
                    .type(WidgetType.STATS)
                    .layout(new WidgetLayout().cols(1).rows(1).x(0).y(0))
                    .request(widgetRequest)
            )
        );
        return createDashboard;
    }

    public static CreateUpdateDashboard anUpdateDashboard() {
        var updatePayload = new CreateUpdateDashboard();
        updatePayload.setName("Updated Dashboard");
        updatePayload.setLabels(Map.of("team", "platform"));
        updatePayload.setWidgets(
            List.of(
                new Widget()
                    .id("10")
                    .title("Error Rate")
                    .type(WidgetType.STATS)
                    .layout(new WidgetLayout().cols(1).rows(1).x(0).y(0))
                    .request(
                        new WidgetRequest()
                            .type(WidgetRequest.TypeEnum.MEASURES)
                            .timeRange(defaultTimeRange())
                            .metrics(
                                List.of(new MetricRequest().name(MetricName.HTTP_ERROR_RATE).measures(List.of(MeasureName.PERCENTAGE)))
                            )
                    ),
                new Widget()
                    .id("11")
                    .title("Top Applications")
                    .type(WidgetType.PIE)
                    .layout(new WidgetLayout().cols(2).rows(2).x(1).y(0))
                    .request(validFacetsRequest(FacetName.APPLICATION, 10))
            )
        );
        return updatePayload;
    }

    public static CreateUpdateDashboard anUpdateDashboardMinimal() {
        var updatePayload = new CreateUpdateDashboard();
        updatePayload.setName("Updated");
        return updatePayload;
    }

    public static Dashboard aDashboard(String id, String environmentId, String createdBy) {
        return Dashboard.builder()
            .id(id)
            .environmentId(environmentId)
            .name("My Dashboard")
            .createdBy(createdBy)
            .createdAt(ZonedDateTime.now())
            .lastModified(ZonedDateTime.now())
            .labels(Map.of("team", "apim"))
            .widgets(
                List.of(
                    DashboardWidget.builder()
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
                                .metrics(
                                    List.of(
                                        DashboardWidget.MetricRequest.builder().name("HTTP_REQUESTS").measures(List.of("COUNT")).build()
                                    )
                                )
                                .build()
                        )
                        .build(),
                    DashboardWidget.builder()
                        .id("2")
                        .title("HTTP Statuses")
                        .type("doughnut")
                        .layout(DashboardWidget.Layout.builder().cols(1).rows(2).x(0).y(1).build())
                        .request(
                            DashboardWidget.Request.builder()
                                .type("facets")
                                .timeRange(
                                    DashboardWidget.TimeRange.builder()
                                        .from(Instant.parse("2025-10-07T06:50:30Z"))
                                        .to(Instant.parse("2025-12-07T11:35:30Z"))
                                        .build()
                                )
                                .by(List.of("HTTP_STATUS_CODE_GROUP"))
                                .metrics(
                                    List.of(
                                        DashboardWidget.MetricRequest.builder().name("HTTP_REQUESTS").measures(List.of("COUNT")).build()
                                    )
                                )
                                .build()
                        )
                        .build(),
                    DashboardWidget.builder()
                        .id("3")
                        .title("Top APIs")
                        .type("time-series-line")
                        .layout(DashboardWidget.Layout.builder().cols(3).rows(2).x(1).y(1).build())
                        .request(
                            DashboardWidget.Request.builder()
                                .type("facets")
                                .timeRange(
                                    DashboardWidget.TimeRange.builder()
                                        .from(Instant.parse("2025-10-07T06:50:30Z"))
                                        .to(Instant.parse("2025-12-07T11:35:30Z"))
                                        .build()
                                )
                                .by(List.of("API"))
                                .limit(5)
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

    public static List<Dashboard> dashboardsForEnvironment(String environmentId, int count) {
        var list = new ArrayList<Dashboard>();
        for (int i = 0; i < count; i++) {
            list.add(
                Dashboard.builder()
                    .id("dashboard-" + (i + 1))
                    .environmentId(environmentId)
                    .name("Dashboard " + (i + 1))
                    .createdBy("user-1")
                    .createdAt(ZonedDateTime.now())
                    .lastModified(ZonedDateTime.now())
                    .labels(Map.of("team", "apim"))
                    .widgets(List.of())
                    .build()
            );
        }
        return list;
    }

    public static CreateUpdateDashboard aCreateDashboardWithFilters() {
        var createDashboard = new CreateUpdateDashboard();
        createDashboard.setName("Dashboard With Filters");
        createDashboard.setWidgets(
            List.of(
                new Widget()
                    .id("1")
                    .title("Filtered Requests")
                    .type(WidgetType.STATS)
                    .layout(new WidgetLayout().cols(2).rows(1).x(0).y(0))
                    .request(
                        new WidgetRequest()
                            .type(WidgetRequest.TypeEnum.MEASURES)
                            .timeRange(defaultTimeRange())
                            .metrics(
                                List.of(
                                    new MetricRequest()
                                        .name(MetricName.HTTP_REQUESTS)
                                        .measures(List.of(MeasureName.COUNT))
                                        .filters(
                                            List.of(
                                                new Filter(
                                                    new NumberFilter().name(FilterName.HTTP_STATUS).operator(Operator.GTE).value(200)
                                                )
                                            )
                                        )
                                )
                            )
                            .filters(
                                List.of(
                                    new Filter(new StringFilter().name(FilterName.API).operator(Operator.EQ).value("my-api")),
                                    new Filter(
                                        new ArrayFilter()
                                            .name(FilterName.HTTP_STATUS_CODE_GROUP)
                                            .operator(Operator.IN)
                                            .value(List.of("2xx", "4xx"))
                                    )
                                )
                            )
                    )
            )
        );
        return createDashboard;
    }

    public static Dashboard aDashboardWithFilters(String id, String environmentId, String createdBy) {
        return Dashboard.builder()
            .id(id)
            .environmentId(environmentId)
            .name("Dashboard With Filters")
            .createdBy(createdBy)
            .createdAt(ZonedDateTime.now())
            .lastModified(ZonedDateTime.now())
            .widgets(
                List.of(
                    DashboardWidget.builder()
                        .id("1")
                        .title("Filtered Requests")
                        .type("stats")
                        .layout(DashboardWidget.Layout.builder().cols(2).rows(1).x(0).y(0).build())
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
                                        DashboardWidget.MetricRequest.builder()
                                            .name("HTTP_REQUESTS")
                                            .measures(List.of("COUNT"))
                                            .filters(
                                                List.of(
                                                    DashboardWidget.Filter.builder().name("HTTP_STATUS").operator("GTE").value(200).build()
                                                )
                                            )
                                            .build()
                                    )
                                )
                                .filters(
                                    List.of(
                                        DashboardWidget.Filter.builder().name("API").operator("EQ").value("my-api").build(),
                                        DashboardWidget.Filter.builder()
                                            .name("HTTP_STATUS_CODE_GROUP")
                                            .operator("IN")
                                            .value(List.of("2xx", "4xx"))
                                            .build()
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
