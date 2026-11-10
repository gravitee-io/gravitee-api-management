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
package io.gravitee.rest.api.management.v2.rest.resource.credential;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.credential.model.CredentialActor;
import io.gravitee.apim.core.credential.model.CredentialActor.SecretAccess;
import io.gravitee.apim.core.exception.NotAllowedDomainException;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.SecurityContext;
import java.util.EnumSet;
import org.springframework.security.core.context.SecurityContextHolder;

/** The caller of a credential endpoint, with the rights the credential rules depend on. */
final class CurrentCredentialActor {

    private static final String ORGANIZATION_ADMIN = RoleScope.ORGANIZATION.name() + ':' + SystemRole.ADMIN.name();
    private static final String ENVIRONMENT_ADMIN = RoleScope.ENVIRONMENT.name() + ':' + SystemRole.ADMIN.name();

    private CurrentCredentialActor() {}

    static CredentialActor of(SecurityContext securityContext, PermissionService permissionService) {
        var ctx = GraviteeContext.getExecutionContext();
        var auditInfo = AuditInfo.builder()
            .organizationId(ctx.getOrganizationId())
            .environmentId(ctx.getEnvironmentId())
            .actor(authenticatedUser())
            .build();
        var admin = securityContext.isUserInRole(ORGANIZATION_ADMIN) || securityContext.isUserInRole(ENVIRONMENT_ADMIN);

        var secretAccess = EnumSet.noneOf(SecretAccess.class);
        for (var access : SecretAccess.values()) {
            if (
                permissionService.hasPermission(
                    ctx,
                    RolePermission.ENVIRONMENT_SECRET,
                    ctx.getEnvironmentId(),
                    RolePermissionAction.valueOf(access.name())
                )
            ) {
                secretAccess.add(access);
            }
        }
        return new CredentialActor(auditInfo, admin, secretAccess);
    }

    // The owner rule compares user ids, so a caller without one must not get through as a null owner.
    private static AuditActor authenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails user)) {
            throw new NotAllowedDomainException("Credentials can only be managed by an authenticated user.");
        }
        return AuditActor.builder().userId(user.getUsername()).userSource(user.getSource()).userSourceId(user.getSourceId()).build();
    }
}
