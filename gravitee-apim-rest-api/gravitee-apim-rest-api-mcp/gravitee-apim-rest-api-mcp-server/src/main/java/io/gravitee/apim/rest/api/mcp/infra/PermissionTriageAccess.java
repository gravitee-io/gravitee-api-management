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
package io.gravitee.apim.rest.api.mcp.infra;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.rest.api.mcp.core.port.TriageAccess;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.ExecutionContext;

public class PermissionTriageAccess implements TriageAccess {

    private final PermissionService permissionService;

    public PermissionTriageAccess(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public boolean canReadTraffic(AuditInfo caller) {
        return (
            can(caller, RolePermission.ENVIRONMENT_DASHBOARD, caller.environmentId()) ||
            can(caller, RolePermission.ENVIRONMENT_API, caller.environmentId())
        );
    }

    @Override
    public boolean canReadApiEvents(AuditInfo caller, String apiId) {
        return can(caller, RolePermission.API_EVENT, apiId);
    }

    @Override
    public boolean canReadApiAudit(AuditInfo caller, String apiId) {
        return can(caller, RolePermission.API_AUDIT, apiId);
    }

    @Override
    public boolean canNotify(AuditInfo caller, String apiId) {
        return can(caller, RolePermission.API_MESSAGE, apiId, RolePermissionAction.CREATE);
    }

    private boolean can(AuditInfo caller, RolePermission permission, String referenceId) {
        return can(caller, permission, referenceId, RolePermissionAction.READ);
    }

    private boolean can(AuditInfo caller, RolePermission permission, String referenceId, RolePermissionAction action) {
        return ApimContext.run(caller, () ->
            permissionService.hasPermission(
                new ExecutionContext(caller.organizationId(), caller.environmentId()),
                caller.actor().userId(),
                permission,
                referenceId,
                action
            )
        );
    }
}
