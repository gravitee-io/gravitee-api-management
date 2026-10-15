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

import inmemory.AuditCrudServiceInMemory;
import inmemory.DashboardCrudServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryValidator;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.dashboard.domain_service.DashboardDomainService;
import io.gravitee.apim.core.dashboard.exception.DashboardNotFoundException;
import io.gravitee.apim.core.dashboard.model.Dashboard;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DeleteDashboardUseCaseTest {

    private static final String DASHBOARD_ID = "dashboard-id";

    private final DashboardCrudServiceInMemory dashboardCrudServiceInMemory = new DashboardCrudServiceInMemory();
    private final AuditCrudServiceInMemory auditCrudServiceInMemory = new AuditCrudServiceInMemory();
    private DeleteDashboardUseCase useCase;

    private static AuditInfo auditInfo() {
        return AuditInfo.builder()
            .organizationId("org-1")
            .environmentId("env-1")
            .actor(AuditActor.builder().userId("user-1").build())
            .build();
    }

    @BeforeEach
    void setUp() {
        var auditDomainService = new AuditDomainService(
            auditCrudServiceInMemory,
            new UserCrudServiceInMemory(),
            new JacksonJsonDiffProcessor()
        );
        var analyticsQueryValidator = new AnalyticsQueryValidator(new AnalyticsDefinitionYAMLQueryService());
        var dashboardDomainService = new DashboardDomainService(dashboardCrudServiceInMemory, analyticsQueryValidator);
        useCase = new DeleteDashboardUseCase(dashboardDomainService, auditDomainService);
    }

    @Test
    void should_delete_dashboard() {
        dashboardCrudServiceInMemory.initWith(List.of(Dashboard.builder().id(DASHBOARD_ID).environmentId("env-1").name("Mine").build()));

        useCase.execute(new DeleteDashboardUseCase.Input(DASHBOARD_ID, auditInfo()));

        assertThat(dashboardCrudServiceInMemory.storage()).isEmpty();
        assertThat(auditCrudServiceInMemory.storage()).hasSize(1);
    }

    @Test
    void should_throw_and_change_nothing_when_dashboard_belongs_to_another_environment() {
        var foreign = Dashboard.builder().id(DASHBOARD_ID).environmentId("other-env").name("Foreign").build();
        dashboardCrudServiceInMemory.initWith(List.of(foreign));

        assertThatThrownBy(() -> useCase.execute(new DeleteDashboardUseCase.Input(DASHBOARD_ID, auditInfo()))).isInstanceOf(
            DashboardNotFoundException.class
        );
        assertThat(dashboardCrudServiceInMemory.storage()).containsExactly(foreign);
        assertThat(auditCrudServiceInMemory.storage()).isEmpty();
    }
}
