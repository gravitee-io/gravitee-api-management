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
import io.gravitee.apim.core.dashboard.domain_service.DashboardDomainService;
import io.gravitee.apim.core.dashboard.exception.DashboardNotFoundException;
import io.gravitee.apim.core.dashboard.model.Dashboard;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetDashboardUseCaseTest {

    private static final String DASHBOARD_ID = "dashboard-id";

    private final DashboardCrudServiceInMemory dashboardCrudServiceInMemory = new DashboardCrudServiceInMemory();
    private GetDashboardUseCase useCase;

    @BeforeEach
    void setUp() {
        var analyticsQueryValidator = new AnalyticsQueryValidator(new AnalyticsDefinitionYAMLQueryService());
        useCase = new GetDashboardUseCase(new DashboardDomainService(dashboardCrudServiceInMemory, analyticsQueryValidator));
    }

    @Test
    void should_return_dashboard_of_the_environment() {
        var dashboard = Dashboard.builder().id(DASHBOARD_ID).environmentId("env-1").name("Mine").build();
        dashboardCrudServiceInMemory.initWith(List.of(dashboard));

        var output = useCase.execute(new GetDashboardUseCase.Input("env-1", DASHBOARD_ID));

        assertThat(output.dashboard()).isEqualTo(dashboard);
    }

    @Test
    void should_throw_when_dashboard_belongs_to_another_environment() {
        dashboardCrudServiceInMemory.initWith(
            List.of(Dashboard.builder().id(DASHBOARD_ID).environmentId("other-env").name("Foreign").build())
        );

        assertThatThrownBy(() -> useCase.execute(new GetDashboardUseCase.Input("env-1", DASHBOARD_ID))).isInstanceOf(
            DashboardNotFoundException.class
        );
    }
}
