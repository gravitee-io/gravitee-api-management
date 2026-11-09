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
package io.gravitee.rest.api.service.exceptions;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import java.util.Map;

/**
 * A 403 that names the permission the caller lacks, for requests that need more than the permission the endpoint checks.
 */
public class MissingPermissionException extends AbstractManagementException {

    private final RolePermission permission;
    private final RolePermissionAction action;
    private final String referenceId;

    public MissingPermissionException(RolePermission permission, RolePermissionAction action, String referenceId) {
        this.permission = permission;
        this.action = action;
        this.referenceId = referenceId;
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.FORBIDDEN_403;
    }

    @Override
    public String getMessage() {
        return "Missing permission " + permission.name() + "[" + action.name() + "] on [" + referenceId + "]";
    }

    @Override
    public String getTechnicalCode() {
        return "permission.missing";
    }

    @Override
    public Map<String, String> getParameters() {
        return Map.of("permission", permission.name(), "action", action.name(), "referenceId", referenceId);
    }
}
