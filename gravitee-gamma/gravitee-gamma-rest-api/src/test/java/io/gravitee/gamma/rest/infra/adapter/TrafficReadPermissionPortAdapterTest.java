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
package io.gravitee.gamma.rest.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gamma.rest.core.observability.analytics.model.DataKind;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort.AccessibleApi;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TrafficReadPermissionPortAdapterTest {

    private static final List<AccessibleApi> ONE_API = List.of(new AccessibleApi("api-1", "API", ApiType.HTTP_PROXY));

    private final PermissionService permissionService = mock(PermissionService.class);
    private final ObservabilityAnalyticsDataPort analyticsDataPort = mock(ObservabilityAnalyticsDataPort.class);
    private final TrafficReadPermissionPortAdapter adapter = new TrafficReadPermissionPortAdapter(permissionService, analyticsDataPort);

    @Test
    void should_grant_a_caller_with_environment_platform_read() {
        when(
            permissionService.hasPermission(
                eq(new ExecutionContext("org-1", "env-1")),
                eq(RolePermission.ENVIRONMENT_PLATFORM),
                eq("env-1"),
                eq(RolePermissionAction.READ)
            )
        ).thenReturn(true);

        assertThat(adapter.readsTraffic("org-1", "env-1")).isTrue();
    }

    @Test
    void should_grant_a_caller_who_reads_the_aggregates_of_one_api() {
        when(analyticsDataPort.loadAccessibleApis("org-1", "env-1", DataKind.AGGREGATES)).thenReturn(ONE_API);

        assertThat(adapter.readsTraffic("org-1", "env-1")).isTrue();
    }

    @Test
    void should_grant_a_caller_who_reads_the_requests_of_one_api() {
        when(analyticsDataPort.loadAccessibleApis("org-1", "env-1", DataKind.AGGREGATES)).thenReturn(List.of());
        when(analyticsDataPort.loadAccessibleApis("org-1", "env-1", DataKind.PER_REQUEST)).thenReturn(ONE_API);

        assertThat(adapter.readsTraffic("org-1", "env-1")).isTrue();
    }

    @Test
    void should_refuse_a_caller_who_reads_no_api_of_the_environment() {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
        when(analyticsDataPort.loadAccessibleApis(any(), any(), any())).thenReturn(List.of());

        assertThat(adapter.readsTraffic("org-1", "env-1")).isFalse();
    }
}
