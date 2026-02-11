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
package io.gravitee.repository.management;

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import io.gravitee.repository.management.model.CustomDashboard;
import io.gravitee.repository.management.model.CustomDashboardWidget;
import java.util.*;
import org.junit.Test;

public class CustomDashboardRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/custom-dashboard-tests/";
    }

    @Test
    public void shouldFindByOrganizationId() throws Exception {
        var dashboards = customDashboardRepository.findByOrganizationId("DEFAULT");
        assertThat(dashboards).hasSize(3);
    }

    @Test
    public void shouldFindByOrganizationId_otherOrg() throws Exception {
        var dashboards = customDashboardRepository.findByOrganizationId("OTHER_ORG");
        assertThat(dashboards).hasSize(1);
        assertThat(dashboards.get(0).getName()).isEqualTo("Other Org Dashboard");
    }

    @Test
    public void shouldReturnEmptyListForUnknownOrganizationId() throws Exception {
        var dashboards = customDashboardRepository.findByOrganizationId("UNKNOWN");
        assertThat(dashboards).isEmpty();
    }

    @Test
    public void shouldFindById() throws Exception {
        var dashboard = customDashboardRepository.findById("cd-1");
        assertThat(dashboard).hasValueSatisfying(result -> {
            assertThat(result.getName()).isEqualTo("Performance Overview");
            assertThat(result.getCreatedBy()).isEqualTo("user-1");
            assertThat(result.getOrganizationId()).isEqualTo("DEFAULT");
            assertThat(result.getLabels()).containsEntry("team", "backend").containsEntry("category", "performance");
            assertThat(result.getWidgets()).hasSize(2);

            var widget1 = result
                .getWidgets()
                .stream()
                .filter(w -> "widget-1".equals(w.getId()))
                .findFirst()
                .orElseThrow();
            assertThat(widget1.getTitle()).isEqualTo("Request Count");
            assertThat(widget1.getType()).isEqualTo("stats");
            assertThat(widget1.getLayout().getCols()).isEqualTo(6);
            assertThat(widget1.getLayout().getRows()).isEqualTo(4);
            assertThat(widget1.getLayout().getX()).isEqualTo(0);
            assertThat(widget1.getLayout().getY()).isEqualTo(0);
            assertThat(widget1.getRequest().getType()).isEqualTo("measures");
            assertThat(widget1.getRequest().getTimeRange()).isNotNull();
            assertThat(widget1.getRequest().getTimeRange().getFrom()).isEqualTo("2025-01-01T00:00:00Z");
            assertThat(widget1.getRequest().getTimeRange().getTo()).isEqualTo("2025-01-02T00:00:00Z");
            assertThat(widget1.getRequest().getMetrics()).containsExactly("request_total_count");
            assertThat(widget1.getRequest().getLimit()).isEqualTo(10);
        });
    }

    @Test
    public void shouldReturnEmptyForUnknownId() throws Exception {
        var dashboard = customDashboardRepository.findById("unknown");
        assertThat(dashboard).isEmpty();
    }

    @Test
    public void shouldCreate() throws Exception {
        var widget = CustomDashboardWidget.builder()
            .id("new-widget-1")
            .title("New Widget")
            .type("stats")
            .layout(CustomDashboardWidget.Layout.builder().cols(6).rows(4).x(0).y(0).build())
            .request(
                CustomDashboardWidget.Request.builder()
                    .type("measures")
                    .timeRange(CustomDashboardWidget.TimeRange.builder().from("2025-01-01T00:00:00Z").to("2025-01-02T00:00:00Z").build())
                    .metrics(List.of("request_total_count"))
                    .by(List.of())
                    .limit(10)
                    .build()
            )
            .build();

        var dashboard = CustomDashboard.builder()
            .id("new-cd")
            .organizationId("DEFAULT")
            .name("New Dashboard")
            .createdBy("user-new")
            .createdAt(new Date(1000000000000L))
            .lastModified(new Date(1111111111111L))
            .labels(Map.of("env", "prod"))
            .widgets(List.of(widget))
            .build();

        var nbBefore = customDashboardRepository.findByOrganizationId("DEFAULT").size();
        customDashboardRepository.create(dashboard);
        var nbAfter = customDashboardRepository.findByOrganizationId("DEFAULT").size();

        assertThat(nbAfter).isEqualTo(nbBefore + 1);

        var saved = customDashboardRepository.findById("new-cd");
        assertThat(saved).hasValueSatisfying(result -> {
            assertThat(result.getId()).isEqualTo("new-cd");
            assertThat(result.getOrganizationId()).isEqualTo("DEFAULT");
            assertThat(result.getName()).isEqualTo("New Dashboard");
            assertThat(result.getCreatedBy()).isEqualTo("user-new");
            assertThat(compareDate(result.getCreatedAt(), new Date(1000000000000L))).isTrue();
            assertThat(compareDate(result.getLastModified(), new Date(1111111111111L))).isTrue();
            assertThat(result.getLabels()).containsEntry("env", "prod");
            assertThat(result.getWidgets()).hasSize(1);

            var savedWidget = result.getWidgets().get(0);
            assertThat(savedWidget.getId()).isEqualTo("new-widget-1");
            assertThat(savedWidget.getTitle()).isEqualTo("New Widget");
            assertThat(savedWidget.getType()).isEqualTo("stats");
            assertThat(savedWidget.getLayout().getCols()).isEqualTo(6);
            assertThat(savedWidget.getLayout().getRows()).isEqualTo(4);
            assertThat(savedWidget.getLayout().getX()).isEqualTo(0);
            assertThat(savedWidget.getLayout().getY()).isEqualTo(0);
            assertThat(savedWidget.getRequest().getType()).isEqualTo("measures");
            assertThat(savedWidget.getRequest().getTimeRange()).isNotNull();
            assertThat(savedWidget.getRequest().getTimeRange().getFrom()).isEqualTo("2025-01-01T00:00:00Z");
            assertThat(savedWidget.getRequest().getTimeRange().getTo()).isEqualTo("2025-01-02T00:00:00Z");
            assertThat(savedWidget.getRequest().getMetrics()).containsExactly("request_total_count");
            assertThat(savedWidget.getRequest().getBy()).isEmpty();
            assertThat(savedWidget.getRequest().getLimit()).isEqualTo(10);
        });
    }

    @Test
    public void shouldCreateWithNullOrEmptyLabelsAndWidgets() throws Exception {
        var withNull = CustomDashboard.builder()
            .id("new-cd-null")
            .organizationId("DEFAULT")
            .name("Dashboard With Null")
            .createdBy("user-new")
            .createdAt(new Date(1000000000000L))
            .lastModified(new Date(1111111111111L))
            .labels(null)
            .widgets(null)
            .build();
        customDashboardRepository.create(withNull);

        var savedNull = customDashboardRepository.findById("new-cd-null");
        assertThat(savedNull).hasValueSatisfying(result -> {
            assertThat(result.getLabels()).isNull();
            assertThat(result.getWidgets()).isNull();
        });

        var withEmpty = CustomDashboard.builder()
            .id("new-cd-empty")
            .organizationId("DEFAULT")
            .name("Dashboard With Empty")
            .createdBy("user-new")
            .createdAt(new Date(1000000000000L))
            .lastModified(new Date(1111111111111L))
            .labels(Map.of())
            .widgets(List.of())
            .build();
        customDashboardRepository.create(withEmpty);

        var savedEmpty = customDashboardRepository.findById("new-cd-empty");
        assertThat(savedEmpty).hasValueSatisfying(result -> {
            assertThat(result.getLabels()).isNotNull().isEmpty();
            assertThat(result.getWidgets()).isNotNull().isEmpty();
        });
    }

    @Test
    public void shouldUpdate() throws Exception {
        var optional = customDashboardRepository.findById("cd-2");
        assertThat(optional).as("Custom dashboard to update not found").isPresent();
        assertThat(optional.get().getName()).isEqualTo("API Metrics");

        var dashboard = optional.get();
        dashboard.setName("Updated Dashboard");
        dashboard.setLabels(Map.of("updated", "true"));
        dashboard.setWidgets(List.of());
        dashboard.setLastModified(new Date(1222222222222L));

        customDashboardRepository.update(dashboard);

        var updated = customDashboardRepository.findById("cd-2");
        assertThat(updated).hasValueSatisfying(result -> {
            assertThat(result.getId()).isEqualTo("cd-2");
            assertThat(result.getOrganizationId()).isEqualTo("DEFAULT");
            assertThat(result.getName()).isEqualTo("Updated Dashboard");
            assertThat(result.getCreatedBy()).isEqualTo("user-2");
            assertThat(compareDate(result.getCreatedAt(), new Date(1000000000000L))).isTrue();
            assertThat(compareDate(result.getLastModified(), new Date(1222222222222L))).isTrue();
            assertThat(result.getLabels()).containsEntry("updated", "true").hasSize(1);
            assertThat(result.getWidgets()).isEmpty();
        });
    }

    @Test
    public void shouldUpdateWithNewWidgets() throws Exception {
        var optional = customDashboardRepository.findById("cd-3");
        assertThat(optional).as("Custom dashboard to update not found").isPresent();
        assertThat(optional.get().getWidgets()).isEmpty();

        var dashboard = optional.get();
        var newWidget = CustomDashboardWidget.builder()
            .id("added-widget")
            .title("Added Widget")
            .type("doughnut")
            .layout(CustomDashboardWidget.Layout.builder().cols(12).rows(8).x(0).y(0).build())
            .request(
                CustomDashboardWidget.Request.builder()
                    .type("facets")
                    .timeRange(CustomDashboardWidget.TimeRange.builder().from("2025-01-01T00:00:00Z").to("2025-02-01T00:00:00Z").build())
                    .metrics(List.of("request_total_count"))
                    .by(List.of("api"))
                    .limit(20)
                    .build()
            )
            .build();
        dashboard.setWidgets(List.of(newWidget));
        dashboard.setLastModified(new Date(1333333333333L));

        customDashboardRepository.update(dashboard);

        var updated = customDashboardRepository.findById("cd-3");
        assertThat(updated).hasValueSatisfying(result -> {
            assertThat(result.getWidgets()).hasSize(1);
            assertThat(result.getWidgets().get(0).getId()).isEqualTo("added-widget");
            assertThat(result.getWidgets().get(0).getTitle()).isEqualTo("Added Widget");
            assertThat(result.getWidgets().get(0).getType()).isEqualTo("doughnut");
        });
    }

    @Test
    public void shouldDelete() throws Exception {
        var nbBefore = customDashboardRepository.findByOrganizationId("DEFAULT").size();
        customDashboardRepository.delete("cd-3");
        var nbAfter = customDashboardRepository.findByOrganizationId("DEFAULT").size();

        assertThat(nbAfter).isEqualTo(nbBefore - 1);
    }

    @Test
    public void shouldDeleteByOrganizationId() throws Exception {
        assertThat(customDashboardRepository.findByOrganizationId("OTHER_ORG")).hasSize(1);
        assertThat(customDashboardRepository.findByOrganizationId("DEFAULT")).hasSize(3);

        customDashboardRepository.deleteByOrganizationId("OTHER_ORG");

        assertThat(customDashboardRepository.findByOrganizationId("OTHER_ORG")).isEmpty();
        assertThat(customDashboardRepository.findByOrganizationId("DEFAULT")).hasSize(3);
        assertThat(customDashboardRepository.findById("cd-4")).isEmpty();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownDashboard() throws Exception {
        var unknown = CustomDashboard.builder().id("unknown").build();
        customDashboardRepository.update(unknown);
        fail("An unknown custom dashboard should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        customDashboardRepository.update(null);
        fail("A null custom dashboard should not be updated");
    }
}
