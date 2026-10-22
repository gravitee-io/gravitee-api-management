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
package io.gravitee.rest.api.service;

import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;

/**
 * @author Nicolas GERAUD(nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface PermissionService {
    boolean hasPermission(
        final ExecutionContext executionContext,
        RolePermission permission,
        String referenceId,
        RolePermissionAction... acls
    );

    boolean hasPermission(
        final ExecutionContext executionContext,
        String userId,
        RolePermission permission,
        String referenceId,
        RolePermissionAction... acls
    );

    boolean hasManagementRights(ExecutionContext executionContext, String userId);

    enum MemberSearchAccess {
        NONE,
        APPLICATION_ONLY,
        FULL,
    }

    /**
     * Tells what kind of directory search access the user has. This is what the member-management screens need
     * and what the user directory search endpoint opens to.
     *
     * @return FULL when the user manages members on a non-application scope (org, env, api, group, etc.);
     *         APPLICATION_ONLY when the only member-management scope is applications (exact-match search only);
     *         NONE when the user manages no members anywhere.
     */
    MemberSearchAccess memberSearchAccess(ExecutionContext executionContext, String userId);

    /**
     * Tells whether the user manages members somewhere: users or roles of the organization, groups of an
     * environment, or members of an API, application, integration, cluster or group. This is what the
     * member-management screens need, and what the user directory search is opened to.
     */
    boolean canManageMembers(ExecutionContext executionContext, String userId);
}
