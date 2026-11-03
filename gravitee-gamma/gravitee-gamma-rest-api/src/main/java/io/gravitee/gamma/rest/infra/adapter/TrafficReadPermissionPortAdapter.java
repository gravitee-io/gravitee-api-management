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

import io.gravitee.gamma.rest.core.observability.analytics.model.DataKind;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.TrafficReadPermissionPort;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import lombok.RequiredArgsConstructor;

/**
 * {@link TrafficReadPermissionPort} for the authenticated caller: {@code ENVIRONMENT_PLATFORM[READ]}, or one API of
 * the environment read through {@code API_ANALYTICS[READ]} (aggregates) or {@code API_LOG[READ]} (requests), the
 * rights the analytics and logs endpoints scope their rows by.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class TrafficReadPermissionPortAdapter implements TrafficReadPermissionPort {

    private final PermissionService permissionService;
    private final ObservabilityAnalyticsDataPort analyticsDataPort;

    @Override
    public boolean readsTraffic(String organizationId, String environmentId) {
        return (
            permissionService.hasPermission(
                new ExecutionContext(organizationId, environmentId),
                RolePermission.ENVIRONMENT_PLATFORM,
                environmentId,
                RolePermissionAction.READ
            ) ||
            readsAnyApi(organizationId, environmentId, DataKind.AGGREGATES) ||
            readsAnyApi(organizationId, environmentId, DataKind.PER_REQUEST)
        );
    }

    private boolean readsAnyApi(String organizationId, String environmentId, DataKind kind) {
        return !analyticsDataPort.loadAccessibleApis(organizationId, environmentId, kind).isEmpty();
    }
}
