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
package io.gravitee.apim.rest.api.automation.resource;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.security.CreateOrUpdateAuthorization;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.gravitee.rest.api.service.impl.PermissionServiceImpl;
import jakarta.inject.Inject;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractResource {

    @Inject
    private PermissionService permissionService;

    protected boolean hasPermission(
        ExecutionContext executionContext,
        RolePermission permission,
        String referenceId,
        RolePermissionAction... acls
    ) {
        return permissionService.hasPermission(executionContext, permission, referenceId, acls);
    }

    /**
     * @param existingId the id of the object the import targets, or {@code null} when it does not exist yet
     * @see CreateOrUpdateAuthorization#require
     */
    protected void requireCreateOrUpdate(RolePermission createPermission, RolePermission updatePermission, String existingId) {
        CreateOrUpdateAuthorization.require(
            permissionService,
            GraviteeContext.getExecutionContext(),
            createPermission,
            updatePermission,
            existingId
        );
    }

    /**
     * Authorizes a create-or-update import of an object held by another one, such as an API page: {@code permission} CREATE
     * on {@code referenceId} when the object is absent, UPDATE when it is present.
     */
    protected void requireCreateOrUpdateOn(RolePermission permission, String referenceId, boolean exists) {
        var action = exists ? RolePermissionAction.UPDATE : RolePermissionAction.CREATE;
        if (!hasPermission(GraviteeContext.getExecutionContext(), permission, referenceId, action)) {
            throw new ForbiddenAccessException();
        }
    }

    // The Automation application registers no SecurityContextFilter: the JAX-RS role check would not see the Gravitee authorities
    protected boolean isAdmin() {
        return PermissionServiceImpl.isOrganizationAdmin();
    }

    protected UserDetails getAuthenticatedUserDetails() {
        return (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    protected AuditInfo getAuditInfo() {
        return buildAuditInfo(GraviteeContext.getExecutionContext(), getAuthenticatedUserDetails());
    }

    protected static AuditInfo buildAuditInfo(ExecutionContext executionContext, UserDetails userDetails) {
        return AuditInfo.builder()
            .organizationId(executionContext.getOrganizationId())
            .environmentId(executionContext.getEnvironmentId())
            .actor(
                AuditActor.builder()
                    .userId(userDetails.getUsername())
                    .userSource(userDetails.getSource())
                    .userSourceId(userDetails.getSourceId())
                    .build()
            )
            .build();
    }
}
