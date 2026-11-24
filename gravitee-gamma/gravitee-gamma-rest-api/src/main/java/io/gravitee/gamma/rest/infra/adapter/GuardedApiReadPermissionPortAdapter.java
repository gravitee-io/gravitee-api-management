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

import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.gamma.rest.core.guardrail.port.service_provider.GuardedApiReadPermissionPort;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import lombok.RequiredArgsConstructor;

/**
 * {@link GuardedApiReadPermissionPort} on the platform {@link PermissionDomainService}.
 *
 * <p>The two permission names live here rather than in the core, as they do for
 * {@link DashboardPermissionPortAdapter} and {@link TrafficReadPermissionPortAdapter}: {@code RolePermission} and
 * {@code RolePermissionAction} belong to {@code io.gravitee.rest.api.model}, which AGENTS.md §5 keeps out of the
 * core.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class GuardedApiReadPermissionPortAdapter implements GuardedApiReadPermissionPort {

    private final PermissionDomainService permissions;

    @Override
    public boolean canRead(String organizationId, String userId, String apiId) {
        return permissions.hasPermission(organizationId, userId, RolePermission.API_DEFINITION, apiId, RolePermissionAction.READ);
    }
}
