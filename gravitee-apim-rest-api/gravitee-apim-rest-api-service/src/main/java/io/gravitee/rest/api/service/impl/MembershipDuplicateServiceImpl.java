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
package io.gravitee.rest.api.service.impl;

import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipDuplicateService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MembershipDuplicateServiceImpl implements MembershipDuplicateService {

    private final RoleService roleService;
    private final MembershipService membershipService;

    public MembershipDuplicateServiceImpl(RoleService roleService, MembershipService membershipService) {
        this.roleService = roleService;
        this.membershipService = membershipService;
    }

    @Override
    public List<MemberEntity> duplicateMemberships(
        ExecutionContext executionContext,
        String sourceApiId,
        String duplicatedApiId,
        String userId
    ) {
        var primaryOwnerRole = roleService.findPrimaryOwnerRoleByOrganization(executionContext.getOrganizationId(), RoleScope.API);
        if (primaryOwnerRole == null) {
            return List.of();
        }

        var defaultRoles = roleService.findDefaultRoleByScopes(executionContext.getOrganizationId(), RoleScope.API);
        if (defaultRoles == null || defaultRoles.isEmpty()) {
            throw new IllegalStateException("No default role defined for API scope");
        }

        var defaultRole = defaultRoles.get(0);
        return membershipService
            .getMembershipsByReference(MembershipReferenceType.API, sourceApiId)
            .stream()
            .filter(m -> !m.getMemberId().equals(userId))
            .map(m -> {
                var roleId = m.getRoleId();

                if (roleId.equals(primaryOwnerRole.getId())) {
                    roleId = defaultRole.getId();
                }

                return membershipService.addRoleToMemberOnReference(
                    executionContext,
                    MembershipReferenceType.API,
                    duplicatedApiId,
                    m.getMemberType(),
                    m.getMemberId(),
                    roleId
                );
            })
            .toList();
    }
}
