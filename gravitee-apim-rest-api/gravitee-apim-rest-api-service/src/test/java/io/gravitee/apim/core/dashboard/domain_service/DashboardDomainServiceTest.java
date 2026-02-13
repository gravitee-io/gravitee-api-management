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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryValidator;
import io.gravitee.apim.core.analytics_engine.exception.InvalidQueryException;
import io.gravitee.apim.core.dashboard.crud_service.DashboardCrudService;
import io.gravitee.apim.core.dashboard.exception.DashboardNotFoundException;
import io.gravitee.apim.core.dashboard.model.Dashboard;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DashboardDomainServiceTest {

    private static final String DASHBOARD_ID = "dashboard-id";

    private DashboardCrudService dashboardCrudService;
    private AnalyticsQueryValidator analyticsQueryValidator;
    private DashboardDomainService domainService;

    @BeforeEach
    void setUp() {
        dashboardCrudService = mock(DashboardCrudService.class);
        analyticsQueryValidator = mock(AnalyticsQueryValidator.class);
        domainService = new DashboardDomainService(dashboardCrudService, analyticsQueryValidator);
    }

    @Nested
    class FindById {

        @Test
        void should_forward_to_crud_service() {
            var dashboard = Dashboard.builder().id(DASHBOARD_ID).name("Test").build();
            when(dashboardCrudService.findById(DASHBOARD_ID)).thenReturn(Optional.of(dashboard));

            var result = domainService.findById(DASHBOARD_ID);

            assertThat(result).contains(dashboard);
            verify(dashboardCrudService).findById(DASHBOARD_ID);
        }
    }

    @Nested
    class Create {

        @Test
        void should_validate_then_create() {
            var dashboard = Dashboard.builder().name("New").widgets(null).build();
            when(dashboardCrudService.create(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = domainService.create(dashboard);

            assertThat(result).isEqualTo(dashboard);
            verify(dashboardCrudService).create(dashboard);
        }
    }

    @Nested
    class Update {

        @Test
        void should_validate_then_update() {
            var dashboard = Dashboard.builder().id(DASHBOARD_ID).name("Updated").widgets(null).build();
            when(dashboardCrudService.update(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = domainService.update(dashboard);

            assertThat(result).isEqualTo(dashboard);
            verify(dashboardCrudService).update(dashboard);
        }
    }

    @Nested
    class Delete {

        @Test
        void should_throw_when_not_found() {
            when(dashboardCrudService.findById(DASHBOARD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> domainService.delete(DASHBOARD_ID)).isInstanceOf(DashboardNotFoundException.class);
            verify(dashboardCrudService).findById(DASHBOARD_ID);
            verify(dashboardCrudService, never()).delete(eq(DASHBOARD_ID));
        }

        @Test
        void should_delete_when_found() {
            var dashboard = Dashboard.builder().id(DASHBOARD_ID).name("To delete").build();
            when(dashboardCrudService.findById(DASHBOARD_ID)).thenReturn(Optional.of(dashboard));

            domainService.delete(DASHBOARD_ID);

            verify(dashboardCrudService).findById(DASHBOARD_ID);
            verify(dashboardCrudService).delete(DASHBOARD_ID);
        }
    }

    @Nested
    class Validation {

        @Test
        void create_should_throw_when_validation_fails() {
            var dashboard = Dashboard.builder().name("Bad").widgets(null).build();
            when(dashboardCrudService.create(any())).thenThrow(new InvalidQueryException("Invalid"));

            assertThatThrownBy(() -> domainService.create(dashboard))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("Invalid");
        }
    }
}
