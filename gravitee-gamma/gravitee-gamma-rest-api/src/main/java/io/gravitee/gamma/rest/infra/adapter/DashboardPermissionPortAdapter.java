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

import io.gravitee.gamma.rest.core.observability.capabilities.port.service_provider.DashboardPermissionPort;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import lombok.RequiredArgsConstructor;

/**
 * {@link DashboardPermissionPort} on the platform {@link PermissionService}, for the authenticated caller.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class DashboardPermissionPortAdapter implements DashboardPermissionPort {

    private final PermissionService permissionService;

    @Override
    public boolean holds(String organizationId, String environmentId, Action action) {
        return permissionService.hasPermission(
            new ExecutionContext(organizationId, environmentId),
            RolePermission.ENVIRONMENT_DASHBOARD,
            environmentId,
            toAcl(action)
        );
    }

    private static RolePermissionAction toAcl(Action action) {
        return switch (action) {
            case READ -> RolePermissionAction.READ;
            case CREATE -> RolePermissionAction.CREATE;
            case UPDATE -> RolePermissionAction.UPDATE;
        };
    }
}
