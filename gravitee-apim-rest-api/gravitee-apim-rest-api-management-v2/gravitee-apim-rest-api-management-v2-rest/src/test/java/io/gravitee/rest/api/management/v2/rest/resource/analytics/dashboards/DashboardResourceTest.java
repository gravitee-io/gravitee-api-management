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
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.NO_CONTENT_204;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import fixtures.DashboardFixtures;
import inmemory.DashboardCrudServiceInMemory;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FacetName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MetricName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.WidgetRequest;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.WidgetType;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
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
class DashboardResourceTest extends AbstractResourceTest {

    private static final String DASHBOARD_ID = "dashboard-123";
    private static final String NON_EXISTENT_ID = "non-existent-id";

    @Inject
    private DashboardCrudServiceInMemory dashboardCrudServiceInMemory;

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/analytics/dashboards";
    }

    private WebTarget target;

    @BeforeEach
    void init() {
        super.setUp();
        GraviteeContext.cleanContext();
        GraviteeContext.fromExecutionContext(new ExecutionContext(ORGANIZATION));

        target = rootTarget().path(DASHBOARD_ID);

        dashboardCrudServiceInMemory.initWith(List.of(DashboardFixtures.aDashboard(DASHBOARD_ID, ORGANIZATION, USER_NAME)));
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        dashboardCrudServiceInMemory.reset();
    }

    @Nested
    class GetDashboardTest {

        @Test
        void should_get_dashboard() {
            var response = target.request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);

            var result = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Dashboard.class);

            assertAll(
                () -> assertThat(result.getId()).isEqualTo(DASHBOARD_ID),
                () -> assertThat(result.getName()).isEqualTo("My Dashboard"),
                () -> assertThat(result.getCreatedBy()).isEqualTo(USER_NAME),
                () -> assertThat(result.getLabels()).isEqualTo(Map.of("team", "apim")),
                () -> assertThat(result.getWidgets()).hasSize(3)
            );

            var statsWidget = result.getWidgets().getFirst();

            assertAll(
                () -> assertThat(statsWidget.getId()).isEqualTo("1"),
                () -> assertThat(statsWidget.getTitle()).isEqualTo("Requests"),
                () -> assertThat(statsWidget.getType()).isEqualTo(WidgetType.STATS),
                () -> assertThat(statsWidget.getLayout().getCols()).isEqualTo(1),
                () -> assertThat(statsWidget.getLayout().getRows()).isEqualTo(1),
                () -> assertThat(statsWidget.getLayout().getX()).isEqualTo(0),
                () -> assertThat(statsWidget.getLayout().getY()).isEqualTo(0),
                () -> assertThat(statsWidget.getRequest().getType()).isEqualTo(WidgetRequest.TypeEnum.MEASURES),
                () -> assertThat(statsWidget.getRequest().getTimeRange().getFrom()).isEqualTo(OffsetDateTime.parse("2025-10-07T06:50:30Z")),
                () -> assertThat(statsWidget.getRequest().getTimeRange().getTo()).isEqualTo(OffsetDateTime.parse("2025-12-07T11:35:30Z")),
                () -> assertThat(statsWidget.getRequest().getMetrics()).hasSize(1),
                () -> assertThat(statsWidget.getRequest().getMetrics().getFirst().getName()).isEqualTo(MetricName.HTTP_REQUESTS)
            );

            var doughnutWidget = result.getWidgets().get(1);

            assertAll(
                () -> assertThat(doughnutWidget.getId()).isEqualTo("2"),
                () -> assertThat(doughnutWidget.getTitle()).isEqualTo("HTTP Statuses"),
                () -> assertThat(doughnutWidget.getType()).isEqualTo(WidgetType.DOUGHNUT),
                () -> assertThat(doughnutWidget.getLayout().getCols()).isEqualTo(1),
                () -> assertThat(doughnutWidget.getLayout().getRows()).isEqualTo(2),
                () -> assertThat(doughnutWidget.getRequest().getType()).isEqualTo(WidgetRequest.TypeEnum.FACETS),
                () -> assertThat(doughnutWidget.getRequest().getBy()).containsExactly(FacetName.HTTP_STATUS_CODE_GROUP),
                () -> assertThat(doughnutWidget.getRequest().getMetrics().getFirst().getName()).isEqualTo(MetricName.HTTP_REQUESTS)
            );

            var lineWidget = result.getWidgets().get(2);

            assertAll(
                () -> assertThat(lineWidget.getId()).isEqualTo("3"),
                () -> assertThat(lineWidget.getTitle()).isEqualTo("Top APIs"),
                () -> assertThat(lineWidget.getType()).isEqualTo(WidgetType.LINE),
                () -> assertThat(lineWidget.getLayout().getCols()).isEqualTo(3),
                () -> assertThat(lineWidget.getLayout().getRows()).isEqualTo(2),
                () -> assertThat(lineWidget.getRequest().getType()).isEqualTo(WidgetRequest.TypeEnum.FACETS),
                () -> assertThat(lineWidget.getRequest().getBy()).containsExactly(FacetName.API),
                () -> assertThat(lineWidget.getRequest().getLimit()).isEqualTo(5),
                () -> assertThat(lineWidget.getRequest().getMetrics().getFirst().getName()).isEqualTo(MetricName.HTTP_REQUESTS)
            );
        }

        @Test
        void should_return_404_when_dashboard_not_found() {
            var response = rootTarget().path(NON_EXISTENT_ID).request().get();

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.ORGANIZATION_DASHBOARD, ORGANIZATION, RolePermissionAction.READ, () -> target.request().get());
        }
    }

    @Nested
    class UpdateDashboardTest {

        @Test
        void should_update_dashboard() {
            var response = target.request().put(json(DashboardFixtures.anUpdateDashboard()));

            assertThat(response.getStatus()).isEqualTo(OK_200);

            var result = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Dashboard.class);
            assertAll(
                () -> assertThat(result.getId()).isEqualTo(DASHBOARD_ID),
                () -> assertThat(result.getName()).isEqualTo("Updated Dashboard"),
                () -> assertThat(result.getLabels()).isEqualTo(Map.of("team", "platform")),
                () -> assertThat(result.getWidgets()).hasSize(2)
            );

            var statsWidget = result.getWidgets().get(0);
            assertAll(
                () -> assertThat(statsWidget.getId()).isEqualTo("10"),
                () -> assertThat(statsWidget.getTitle()).isEqualTo("Error Rate"),
                () -> assertThat(statsWidget.getType()).isEqualTo(WidgetType.STATS),
                () -> assertThat(statsWidget.getRequest().getType()).isEqualTo(WidgetRequest.TypeEnum.MEASURES),
                () -> assertThat(statsWidget.getRequest().getTimeRange().getFrom()).isEqualTo(OffsetDateTime.parse("2025-10-07T06:50:30Z")),
                () -> assertThat(statsWidget.getRequest().getMetrics().getFirst().getName()).isEqualTo(MetricName.HTTP_ERRORS)
            );

            var pieWidget = result.getWidgets().get(1);
            assertAll(
                () -> assertThat(pieWidget.getId()).isEqualTo("11"),
                () -> assertThat(pieWidget.getTitle()).isEqualTo("Top Applications"),
                () -> assertThat(pieWidget.getType()).isEqualTo(WidgetType.PIE),
                () -> assertThat(pieWidget.getRequest().getType()).isEqualTo(WidgetRequest.TypeEnum.FACETS),
                () -> assertThat(pieWidget.getRequest().getBy()).containsExactly(FacetName.APPLICATION),
                () -> assertThat(pieWidget.getRequest().getLimit()).isEqualTo(10)
            );

            var stored = dashboardCrudServiceInMemory.findById(DASHBOARD_ID).orElseThrow();
            assertThat(stored.getName()).isEqualTo("Updated Dashboard");
            assertThat(stored.getLabels()).isEqualTo(Map.of("team", "platform"));
            assertThat(stored.getWidgets()).hasSize(2);
            assertThat(stored.getWidgets().getFirst().getType()).isEqualTo("stats");
            assertThat(stored.getWidgets().getFirst().getRequest().getType()).isEqualTo("measures");
            assertThat(stored.getWidgets().getFirst().getRequest().getMetrics())
                .hasSize(1)
                .first()
                .satisfies(m -> assertThat(m.getName()).isEqualTo("HTTP_ERRORS"));
            assertThat(stored.getWidgets().get(1).getType()).isEqualTo("pie");
            assertThat(stored.getWidgets().get(1).getRequest().getBy()).containsExactly("APPLICATION");
            assertThat(stored.getWidgets().get(1).getRequest().getLimit()).isEqualTo(10);
        }

        @Test
        void should_return_404_when_dashboard_not_found() {
            var response = rootTarget().path(NON_EXISTENT_ID).request().put(json(DashboardFixtures.anUpdateDashboardMinimal()));
            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
        }

        @Test
        void should_return_400_if_missing_body() {
            var response = target.request().put(json(""));
            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.ORGANIZATION_DASHBOARD, ORGANIZATION, RolePermissionAction.UPDATE, () ->
                target.request().put(json(DashboardFixtures.anUpdateDashboardMinimal()))
            );
        }
    }

    @Nested
    class DeleteDashboardTest {

        @Test
        void should_delete_dashboard() {
            var response = target.request().delete();

            assertThat(response.getStatus()).isEqualTo(NO_CONTENT_204);

            assertThat(dashboardCrudServiceInMemory.findById(DASHBOARD_ID)).isEmpty();
        }

        @Test
        void should_return_404_when_dashboard_not_found() {
            var response = rootTarget().path(NON_EXISTENT_ID).request().delete();

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.ORGANIZATION_DASHBOARD, ORGANIZATION, RolePermissionAction.DELETE, () ->
                target.request().delete()
            );
        }
    }
}
