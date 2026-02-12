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
package io.gravitee.rest.api.management.v2.rest.resource.analytics.dashboards;

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import inmemory.DashboardCrudServiceInMemory;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.CreateDashboard;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FacetName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MetricName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MetricRequest;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.TimeRange;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Widget;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.WidgetLayout;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.WidgetRequest;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.WidgetType;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DashboardsResourceTest extends AbstractResourceTest {

    @Inject
    private DashboardCrudServiceInMemory dashboardCrudServiceInMemory;

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/analytics/dashboards";
    }

    @BeforeEach
    void init() {
        super.setUp();
        GraviteeContext.cleanContext();
        GraviteeContext.fromExecutionContext(new ExecutionContext(ORGANIZATION));
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        dashboardCrudServiceInMemory.reset();
    }

    @Nested
    class CreateDashboardTest {

        @Test
        void should_create_dashboard() {
            var createDashboard = new CreateDashboard();
            createDashboard.setName("My Dashboard");
            createDashboard.setLabels(Map.of("gravitee_io/team", "apim"));
            createDashboard.setWidgets(
                List.of(
                    new Widget()
                        .id("1")
                        .title("Requests")
                        .type(WidgetType.STATS)
                        .layout(new WidgetLayout().cols(1).rows(1).x(0).y(0))
                        .request(
                            new WidgetRequest()
                                .type(WidgetRequest.TypeEnum.MEASURES)
                                .timeRange(
                                    new TimeRange()
                                        .from(OffsetDateTime.parse("2025-10-07T06:50:30Z"))
                                        .to(OffsetDateTime.parse("2025-12-07T11:35:30Z"))
                                )
                                .metrics(List.of(new MetricRequest().name(MetricName.HTTP_REQUESTS)))
                        ),
                    new Widget()
                        .id("2")
                        .title("HTTP Statuses")
                        .type(WidgetType.DOUGHNUT)
                        .layout(new WidgetLayout().cols(1).rows(2).x(0).y(1))
                        .request(
                            new WidgetRequest()
                                .type(WidgetRequest.TypeEnum.FACETS)
                                .timeRange(
                                    new TimeRange()
                                        .from(OffsetDateTime.parse("2025-10-07T06:50:30Z"))
                                        .to(OffsetDateTime.parse("2025-12-07T11:35:30Z"))
                                )
                                .by(List.of(FacetName.HTTP_STATUS_CODE_GROUP))
                                .metrics(List.of(new MetricRequest().name(MetricName.HTTP_REQUESTS)))
                        ),
                    new Widget()
                        .id("3")
                        .title("Top APIs")
                        .type(WidgetType.LINE)
                        .layout(new WidgetLayout().cols(3).rows(2).x(1).y(1))
                        .request(
                            new WidgetRequest()
                                .type(WidgetRequest.TypeEnum.FACETS)
                                .timeRange(
                                    new TimeRange()
                                        .from(OffsetDateTime.parse("2025-10-07T06:50:30Z"))
                                        .to(OffsetDateTime.parse("2025-12-07T11:35:30Z"))
                                )
                                .by(List.of(FacetName.API))
                                .limit(5)
                                .metrics(List.of(new MetricRequest().name(MetricName.HTTP_REQUESTS)))
                        )
                )
            );

            var response = rootTarget().request().post(json(createDashboard));

            assertThat(response.getStatus()).isEqualTo(CREATED_201);

            var created = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Dashboard.class);
            assertAll(
                () -> assertThat(created.getId()).isNotBlank(),
                () -> assertThat(created.getName()).isEqualTo("My Dashboard"),
                () -> assertThat(created.getCreatedBy()).isEqualTo(USER_NAME),
                () -> assertThat(created.getCreatedAt()).isNotNull(),
                () -> assertThat(created.getLastModified()).isNotNull(),
                () -> assertThat(created.getLabels()).isEqualTo(Map.of("gravitee_io/team", "apim")),
                () -> assertThat(created.getWidgets()).hasSize(3)
            );

            // Verify response widget mapping (REST -> core -> REST round-trip)
            var statsWidget = created.getWidgets().get(0);
            assertAll(
                () -> assertThat(statsWidget.getId()).isEqualTo("1"),
                () -> assertThat(statsWidget.getTitle()).isEqualTo("Requests"),
                () -> assertThat(statsWidget.getType()).isEqualTo(WidgetType.STATS),
                () -> assertThat(statsWidget.getLayout().getCols()).isEqualTo(1),
                () -> assertThat(statsWidget.getLayout().getRows()).isEqualTo(1),
                () -> assertThat(statsWidget.getRequest().getType()).isEqualTo(WidgetRequest.TypeEnum.MEASURES),
                () -> assertThat(statsWidget.getRequest().getTimeRange().getFrom()).isEqualTo(OffsetDateTime.parse("2025-10-07T06:50:30Z")),
                () -> assertThat(statsWidget.getRequest().getTimeRange().getTo()).isEqualTo(OffsetDateTime.parse("2025-12-07T11:35:30Z")),
                () -> assertThat(statsWidget.getRequest().getMetrics().get(0).getName()).isEqualTo(MetricName.HTTP_REQUESTS)
            );

            var doughnutWidget = created.getWidgets().get(1);
            assertAll(
                () -> assertThat(doughnutWidget.getType()).isEqualTo(WidgetType.DOUGHNUT),
                () -> assertThat(doughnutWidget.getRequest().getType()).isEqualTo(WidgetRequest.TypeEnum.FACETS),
                () -> assertThat(doughnutWidget.getRequest().getBy()).containsExactly(FacetName.HTTP_STATUS_CODE_GROUP)
            );

            var lineWidget = created.getWidgets().get(2);
            assertAll(
                () -> assertThat(lineWidget.getType()).isEqualTo(WidgetType.LINE),
                () -> assertThat(lineWidget.getRequest().getType()).isEqualTo(WidgetRequest.TypeEnum.FACETS),
                () -> assertThat(lineWidget.getRequest().getBy()).containsExactly(FacetName.API),
                () -> assertThat(lineWidget.getRequest().getLimit()).isEqualTo(5)
            );

            // Verify stored core model
            var storage = dashboardCrudServiceInMemory.storage();
            assertThat(storage).hasSize(1);

            var stored = storage.getFirst();
            assertThat(stored.getName()).isEqualTo("My Dashboard");
            assertThat(stored.getOrganizationId()).isEqualTo(ORGANIZATION);
            assertThat(stored.getWidgets()).hasSize(3);
            assertThat(stored.getWidgets().get(0).getType()).isEqualTo("stats");
            assertThat(stored.getWidgets().get(0).getRequest().getType()).isEqualTo("measures");
            assertThat(stored.getWidgets().get(0).getRequest().getTimeRange().getFrom()).isEqualTo("2025-10-07T06:50:30Z");
            assertThat(stored.getWidgets().get(0).getRequest().getMetrics()).containsExactly("HTTP_REQUESTS");
            assertThat(stored.getWidgets().get(1).getType()).isEqualTo("doughnut");
            assertThat(stored.getWidgets().get(1).getRequest().getBy()).containsExactly("HTTP_STATUS_CODE_GROUP");
            assertThat(stored.getWidgets().get(2).getType()).isEqualTo("line");
            assertThat(stored.getWidgets().get(2).getRequest().getBy()).containsExactly("API");
            assertThat(stored.getWidgets().get(2).getRequest().getLimit()).isEqualTo(5);
        }

        @Test
        void should_return_400_if_missing_body() {
            var response = rootTarget().request().post(json(null));
            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.ORGANIZATION_DASHBOARD, ORGANIZATION, RolePermissionAction.CREATE, () ->
                rootTarget().request().post(json(new CreateDashboard()))
            );
        }
    }
}
