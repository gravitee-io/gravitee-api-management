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
package io.gravitee.gamma.rest.resources.observability;

import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;

/**
 * Authorization checks shared by the observability resources (logs, analytics, filters, traces).
 *
 * <ul>
 *   <li><b>Environment-wide data</b> (logs search, analytics, filter values and labels):
 *       {@code ENVIRONMENT_PLATFORM[READ]}, the grant of the APIM platform logs and analytics.
 *       {@code ENVIRONMENT_DASHBOARD} only covers dashboard definitions, and {@code ENVIRONMENT_API[READ]}
 *       only allows listing APIs (the default environment {@code USER} role holds it). Which rows the
 *       caller then sees is decided by the use cases, from {@code UserContextLoader#loadApis}.</li>
 *   <li><b>One API's traffic</b> (log detail, trace search and detail): {@code API_LOG[READ]} on
 *       that API. Callers turn a denial into a 404 so it cannot be told apart from a missing
 *       resource.</li>
 * </ul>
 *
 * @author GraviteeSource Team
 */
public final class ObservabilityPermissions {

    private ObservabilityPermissions() {}

    /**
     * @throws ForbiddenAccessException when the caller lacks {@code ENVIRONMENT_PLATFORM[READ]} on the
     *         environment of the request.
     */
    public static void checkEnvironmentRead(PermissionService permissionService) {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        String environmentId = executionContext.getEnvironmentId();
        boolean allowed = permissionService.hasPermission(
            executionContext,
            RolePermission.ENVIRONMENT_PLATFORM,
            environmentId,
            RolePermissionAction.READ
        );
        if (!allowed) {
            throw new ForbiddenAccessException();
        }
    }

    public static boolean canReadApiLogs(PermissionService permissionService, String apiId) {
        return permissionService.hasPermission(
            GraviteeContext.getExecutionContext(),
            RolePermission.API_LOG,
            apiId,
            RolePermissionAction.READ
        );
    }
}
