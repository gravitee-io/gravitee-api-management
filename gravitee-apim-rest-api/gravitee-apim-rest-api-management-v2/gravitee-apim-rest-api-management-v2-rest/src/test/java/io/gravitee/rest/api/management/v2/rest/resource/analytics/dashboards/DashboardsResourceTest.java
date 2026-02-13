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

import assertions.MAPIAssertions;
import fixtures.DashboardFixtures;
import inmemory.DashboardCrudServiceInMemory;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.CreateDashboard;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.CustomInterval;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FacetName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MeasureName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MetricName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MetricRequest;
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
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
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
            var response = rootTarget().request().post(json(DashboardFixtures.aCreateDashboard()));

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

            var statsWidget = created.getWidgets().getFirst();
            assertAll(
                () -> assertThat(statsWidget.getId()).isEqualTo("1"),
                () -> assertThat(statsWidget.getTitle()).isEqualTo("Requests"),
                () -> assertThat(statsWidget.getType()).isEqualTo(WidgetType.STATS),
                () -> assertThat(statsWidget.getLayout().getCols()).isEqualTo(1),
                () -> assertThat(statsWidget.getLayout().getRows()).isEqualTo(1),
                () -> assertThat(statsWidget.getRequest().getType()).isEqualTo(WidgetRequest.TypeEnum.MEASURES),
                () -> assertThat(statsWidget.getRequest().getTimeRange().getFrom()).isEqualTo(OffsetDateTime.parse("2025-10-07T06:50:30Z")),
                () -> assertThat(statsWidget.getRequest().getTimeRange().getTo()).isEqualTo(OffsetDateTime.parse("2025-12-07T11:35:30Z")),
                () -> assertThat(statsWidget.getRequest().getMetrics().getFirst().getName()).isEqualTo(MetricName.HTTP_REQUESTS)
            );

            var doughnutWidget = created.getWidgets().get(1);
            assertAll(
                () -> assertThat(doughnutWidget.getType()).isEqualTo(WidgetType.DOUGHNUT),
                () -> assertThat(doughnutWidget.getRequest().getType()).isEqualTo(WidgetRequest.TypeEnum.FACETS),
                () -> assertThat(doughnutWidget.getRequest().getBy()).containsExactly(FacetName.HTTP_STATUS_CODE_GROUP)
            );

            var lineWidget = created.getWidgets().getLast();

            assertAll(
                () -> assertThat(lineWidget.getType()).isEqualTo(WidgetType.LINE),
                () -> assertThat(lineWidget.getRequest().getType()).isEqualTo(WidgetRequest.TypeEnum.FACETS),
                () -> assertThat(lineWidget.getRequest().getBy()).containsExactly(FacetName.API),
                () -> assertThat(lineWidget.getRequest().getLimit()).isEqualTo(5)
            );

            var storage = dashboardCrudServiceInMemory.storage();
            assertThat(storage).hasSize(1);

            var stored = storage.getFirst();

            assertThat(stored.getName()).isEqualTo("My Dashboard");
            assertThat(stored.getOrganizationId()).isEqualTo(ORGANIZATION);
            assertThat(stored.getWidgets()).hasSize(3);
            assertThat(stored.getWidgets().getFirst().getType()).isEqualTo("stats");
            assertThat(stored.getWidgets().getFirst().getRequest().getType()).isEqualTo("measures");
            assertThat(stored.getWidgets().getFirst().getRequest().getTimeRange().getFrom()).isEqualTo("2025-10-07T06:50:30Z");
            assertThat(stored.getWidgets().getFirst().getRequest().getMetrics())
                .hasSize(1)
                .first()
                .satisfies(m -> assertThat(m.getName()).isEqualTo("HTTP_REQUESTS"));
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

    @Nested
    class CreateDashboardWidgetValidationTest {

        @Test
        void should_return_400_when_facets_request_has_empty_by() {
            var request = new WidgetRequest()
                .type(WidgetRequest.TypeEnum.FACETS)
                .timeRange(DashboardFixtures.defaultTimeRange())
                .by(List.of())
                .metrics(List.of(new MetricRequest().name(MetricName.HTTP_REQUESTS).measures(List.of(MeasureName.COUNT))));
            var response = rootTarget().request().post(json(DashboardFixtures.createDashboardWithOneWidget(request)));

            MAPIAssertions.assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasHttpStatus(BAD_REQUEST_400)
                .hasMessageContaining("by clause");
        }

        @Test
        void should_return_400_when_time_range_from_after_to() {
            var request = DashboardFixtures.validMeasuresRequest();
            request.getTimeRange().setFrom(OffsetDateTime.parse("2025-12-07T11:35:30Z"));
            request.getTimeRange().setTo(OffsetDateTime.parse("2025-10-07T06:50:30Z"));
            var response = rootTarget().request().post(json(DashboardFixtures.createDashboardWithOneWidget(request)));

            MAPIAssertions.assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasHttpStatus(BAD_REQUEST_400)
                .hasMessageContaining("upper bound");
        }

        @Test
        void should_return_400_when_time_series_interval_zero() {
            var request = new WidgetRequest()
                .type(WidgetRequest.TypeEnum.TIME_SERIES)
                .timeRange(DashboardFixtures.defaultTimeRange())
                .interval(new CustomInterval(-1L))
                .metrics(List.of(new MetricRequest().name(MetricName.HTTP_REQUESTS).measures(List.of(MeasureName.COUNT))));
            var response = rootTarget().request().post(json(DashboardFixtures.createDashboardWithOneWidget(request)));

            MAPIAssertions.assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasHttpStatus(BAD_REQUEST_400)
                .hasMessageContaining("interval");
        }

        @Test
        void should_return_400_when_metric_has_no_measures() {
            var request = DashboardFixtures.validMeasuresRequest();
            request.getMetrics().getFirst().setMeasures(null);
            var response = rootTarget().request().post(json(DashboardFixtures.createDashboardWithOneWidget(request)));

            MAPIAssertions.assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasHttpStatus(BAD_REQUEST_400)
                .hasMessageContaining("measure");
        }

        @Test
        void should_return_400_when_metric_has_empty_measures() {
            var request = DashboardFixtures.validMeasuresRequest();
            request.getMetrics().getFirst().setMeasures(List.of());
            var response = rootTarget().request().post(json(DashboardFixtures.createDashboardWithOneWidget(request)));

            MAPIAssertions.assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasHttpStatus(BAD_REQUEST_400)
                .hasMessageContaining("measure");
        }
    }
}
