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
package io.gravitee.apim.core.dashboard.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.DashboardCrudServiceInMemory;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryValidator;
import io.gravitee.apim.core.dashboard.exception.DashboardNotFoundException;
import io.gravitee.apim.core.dashboard.model.Dashboard;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DashboardDomainServiceTest {

    private static final String DASHBOARD_ID = "dashboard-id";

    private final DashboardCrudServiceInMemory dashboardCrudServiceInMemory = new DashboardCrudServiceInMemory();
    private DashboardDomainService domainService;

    @BeforeEach
    void setUp() {
        var analyticsQueryValidator = new AnalyticsQueryValidator(new AnalyticsDefinitionYAMLQueryService());
        domainService = new DashboardDomainService(dashboardCrudServiceInMemory, analyticsQueryValidator);
    }

    @AfterEach
    void tearDown() {
        dashboardCrudServiceInMemory.reset();
    }

    @Nested
    class FindById {

        @Test
        void should_return_dashboard_when_found() {
            var dashboard = Dashboard.builder().id(DASHBOARD_ID).name("Test").build();
            dashboardCrudServiceInMemory.initWith(List.of(dashboard));

            var result = domainService.findById(DASHBOARD_ID);

            assertThat(result).contains(dashboard);
        }

        @Test
        void should_return_empty_when_not_found() {
            var result = domainService.findById(DASHBOARD_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindByOrganizationId {

        @Test
        void should_return_dashboards_for_organization() {
            var dashboard1 = Dashboard.builder().id("d1").organizationId("org-1").name("D1").build();
            var dashboard2 = Dashboard.builder().id("d2").organizationId("org-1").name("D2").build();
            var dashboard3 = Dashboard.builder().id("d3").organizationId("other-org").name("D3").build();
            dashboardCrudServiceInMemory.initWith(List.of(dashboard1, dashboard2, dashboard3));

            var result = domainService.findByOrganizationId("org-1");

            assertThat(result).containsExactly(dashboard1, dashboard2);
        }
    }

    @Nested
    class Create {

        @Test
        void should_create_dashboard() {
            var dashboard = Dashboard.builder().id("new-id").name("New").widgets(null).build();

            var result = domainService.create(dashboard);

            assertThat(result).isEqualTo(dashboard);
            assertThat(dashboardCrudServiceInMemory.storage()).containsExactly(dashboard);
        }
    }

    @Nested
    class Update {

        @Test
        void should_update_dashboard() {
            var dashboard = Dashboard.builder().id(DASHBOARD_ID).name("Original").widgets(null).build();
            dashboardCrudServiceInMemory.initWith(List.of(dashboard));

            var updated = Dashboard.builder().id(DASHBOARD_ID).name("Updated").widgets(null).build();
            var result = domainService.update(updated);

            assertThat(result).isEqualTo(updated);
            assertThat(dashboardCrudServiceInMemory.storage()).containsExactly(updated);
        }
    }

    @Nested
    class Delete {

        @Test
        void should_delete_when_found() {
            var dashboard = Dashboard.builder().id(DASHBOARD_ID).name("To delete").build();
            dashboardCrudServiceInMemory.initWith(List.of(dashboard));

            domainService.delete(DASHBOARD_ID);

            assertThat(dashboardCrudServiceInMemory.storage()).isEmpty();
        }
    }
}
