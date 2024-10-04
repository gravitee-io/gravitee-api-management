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
package io.gravitee.apim.infra.domain_service.permission;

import static io.gravitee.rest.api.service.impl.PermissionServiceImpl.ROLE_SCOPE_TO_REFERENCE_TYPE;
import static io.gravitee.rest.api.service.impl.PermissionServiceImpl.isOrganizationAdmin;

import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.impl.PermissionServiceImpl;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PermissionDomainServiceLegacyWrapper implements PermissionDomainService {

    private final MembershipService membershipService;
    private final PermissionService permissionService;

    public PermissionDomainServiceLegacyWrapper(MembershipService membershipService, PermissionService permissionService) {
        this.membershipService = membershipService;
        this.permissionService = permissionService;
    }

    @Override
    public boolean hasExactPermissions(
        String organizationId,
        String userId,
        io.gravitee.rest.api.model.permissions.RolePermission permission,
        String referenceId,
        io.gravitee.rest.api.model.permissions.RolePermissionAction... acls
    ) {
        if (isOrganizationAdmin()) {
            log.debug("User [{}] has full access because of its ORGANIZATION ADMIN role", userId);
            return true;
        }

        MembershipReferenceType membershipReferenceType = ROLE_SCOPE_TO_REFERENCE_TYPE.get(permission.getScope());

        Map<String, char[]> permissions = membershipService.getUserMemberPermissions(
            new ExecutionContext(organizationId),
            membershipReferenceType,
            referenceId,
            userId
        );

        return checkForExactPermissions(permissions, permission.getPermission(), acls);
    }

    @Override
    public boolean hasPermission(
        String organizationId,
        String userId,
        RolePermission permission,
        String referenceId,
        RolePermissionAction... acls
    ) {
        return this.permissionService.hasPermission(new ExecutionContext(organizationId), userId, permission, referenceId, acls);
    }
}
