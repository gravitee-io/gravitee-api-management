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
import io.gravitee.rest.api.service.common.GraviteeContext;

/**
 * Authorization check shared by the observability resources that read one API's traffic (log detail, trace
 * search and detail): {@code API_LOG[READ]} on that API. Callers turn a denial into a 404 so it cannot be told
 * apart from a missing resource. The environment-wide endpoints have no such check: they answer from the rows
 * the caller reads.
 *
 * @author GraviteeSource Team
 */
public final class ObservabilityPermissions {

    private ObservabilityPermissions() {}

    public static boolean canReadApiLogs(PermissionService permissionService, String apiId) {
        return permissionService.hasPermission(
            GraviteeContext.getExecutionContext(),
            RolePermission.API_LOG,
            apiId,
            RolePermissionAction.READ
        );
    }
}
