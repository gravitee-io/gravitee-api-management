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

import inmemory.DashboardCrudServiceInMemory;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryValidator;
import io.gravitee.apim.core.dashboard.domain_service.DashboardDomainService;
import io.gravitee.apim.core.dashboard.model.Dashboard;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ListDashboardsUseCaseTest {

    private static final String ORGANIZATION_ID = "org-1";

    private final DashboardCrudServiceInMemory dashboardCrudServiceInMemory = new DashboardCrudServiceInMemory();
    private ListDashboardsUseCase listDashboardsUseCase;

    @BeforeEach
    void setUp() {
        var analyticsQueryValidator = new AnalyticsQueryValidator(new AnalyticsDefinitionYAMLQueryService());
        var dashboardDomainService = new DashboardDomainService(dashboardCrudServiceInMemory, analyticsQueryValidator);
        listDashboardsUseCase = new ListDashboardsUseCase(dashboardDomainService);
    }

    @AfterEach
    void tearDown() {
        dashboardCrudServiceInMemory.reset();
    }

    @Test
    void should_return_dashboards_for_organization() {
        var dashboard1 = Dashboard.builder().id("d1").organizationId(ORGANIZATION_ID).name("Dashboard 1").build();
        var dashboard2 = Dashboard.builder().id("d2").organizationId(ORGANIZATION_ID).name("Dashboard 2").build();
        dashboardCrudServiceInMemory.initWith(List.of(dashboard1, dashboard2));

        var output = listDashboardsUseCase.execute(new ListDashboardsUseCase.Input(ORGANIZATION_ID));

        assertThat(output.dashboards()).containsExactly(dashboard1, dashboard2);
    }

    @Test
    void should_return_empty_list_when_no_dashboards() {
        var output = listDashboardsUseCase.execute(new ListDashboardsUseCase.Input(ORGANIZATION_ID));

        assertThat(output.dashboards()).isEmpty();
    }

    @Test
    void should_only_return_dashboards_for_requested_organization() {
        var dashboard1 = Dashboard.builder().id("d1").organizationId(ORGANIZATION_ID).name("Dashboard 1").build();
        var dashboard2 = Dashboard.builder().id("d2").organizationId("other-org").name("Dashboard 2").build();
        dashboardCrudServiceInMemory.initWith(List.of(dashboard1, dashboard2));

        var output = listDashboardsUseCase.execute(new ListDashboardsUseCase.Input(ORGANIZATION_ID));

        assertThat(output.dashboards()).containsExactly(dashboard1);
    }
}
