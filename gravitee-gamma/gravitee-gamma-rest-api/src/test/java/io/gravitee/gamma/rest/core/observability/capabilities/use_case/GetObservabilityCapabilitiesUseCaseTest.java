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
package io.gravitee.gamma.rest.core.observability.capabilities.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.gravitee.gamma.rest.core.observability.analytics.model.DataKind;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort;
import io.gravitee.gamma.rest.core.observability.capabilities.model.ObservabilityCapabilities;
import io.gravitee.gamma.rest.core.observability.capabilities.port.service_provider.DashboardPermissionPort;
import io.gravitee.gamma.rest.core.observability.capabilities.port.service_provider.DashboardPermissionPort.Action;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort.AccessibleApi;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class GetObservabilityCapabilitiesUseCaseTest {

    private static final String ORG = "org-1";
    private static final String ENV = "env-1";
    private static final List<AccessibleApi> ONE_API = List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY));

    @Mock
    private ObservabilityAnalyticsDataPort analyticsDataPort;

    @Mock
    private DashboardPermissionPort dashboardPermissionPort;

    private GetObservabilityCapabilitiesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetObservabilityCapabilitiesUseCase(analyticsDataPort, dashboardPermissionPort);
        lenient().when(analyticsDataPort.loadAccessibleApis(ORG, ENV, DataKind.PER_REQUEST)).thenReturn(List.of());
        lenient().when(analyticsDataPort.loadAccessibleApis(ORG, ENV, DataKind.AGGREGATES)).thenReturn(List.of());
        lenient().when(dashboardPermissionPort.holds(any(), any(), any())).thenReturn(false);
    }

    @Test
    void should_open_nothing_to_a_caller_who_reads_no_api_and_holds_no_dashboard_permission() {
        assertThat(capabilities()).isEqualTo(new ObservabilityCapabilities(false, false, false, false));
    }

    @Test
    void should_open_logs_and_traces_to_a_caller_who_reads_the_requests_of_an_api() {
        when(analyticsDataPort.loadAccessibleApis(ORG, ENV, DataKind.PER_REQUEST)).thenReturn(ONE_API);

        var capabilities = capabilities();

        assertThat(capabilities.logsRead()).isTrue();
        assertThat(capabilities.tracesRead()).isTrue();
    }

    @Test
    void should_open_dashboards_to_a_caller_who_reads_the_aggregates_of_an_api_without_any_dashboard_permission() {
        when(analyticsDataPort.loadAccessibleApis(ORG, ENV, DataKind.AGGREGATES)).thenReturn(ONE_API);

        assertThat(capabilities()).isEqualTo(new ObservabilityCapabilities(false, false, true, false));
    }

    @Test
    void should_open_dashboards_to_a_holder_of_dashboard_read_who_reads_no_api() {
        when(dashboardPermissionPort.holds(ORG, ENV, Action.READ)).thenReturn(true);

        assertThat(capabilities().dashboardsRead()).isTrue();
    }

    @Test
    void should_let_a_holder_of_dashboard_create_write_dashboards() {
        when(dashboardPermissionPort.holds(ORG, ENV, Action.CREATE)).thenReturn(true);

        assertThat(capabilities().dashboardsWrite()).isTrue();
    }

    @Test
    void should_let_a_holder_of_dashboard_update_write_dashboards() {
        when(dashboardPermissionPort.holds(ORG, ENV, Action.UPDATE)).thenReturn(true);

        assertThat(capabilities().dashboardsWrite()).isTrue();
    }

    @Test
    void should_not_let_a_dashboard_reader_write_dashboards() {
        when(dashboardPermissionPort.holds(ORG, ENV, Action.READ)).thenReturn(true);

        assertThat(capabilities().dashboardsWrite()).isFalse();
    }

    private ObservabilityCapabilities capabilities() {
        return useCase.execute(new GetObservabilityCapabilitiesUseCase.Input(ORG, ENV)).capabilities();
    }
}
