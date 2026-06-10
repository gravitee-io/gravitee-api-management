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
package io.gravitee.apim.infra.domain_service.user;

import io.gravitee.apim.core.user.domain_service.AssignUserDefaultRolesDomainService;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.DefaultRoleNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssignUserDefaultRolesDomainServiceImpl implements AssignUserDefaultRolesDomainService {

    private final RoleService roleService;
    private final MembershipService membershipService;

    @Override
    public void assignDefaultRoles(ExecutionContext executionContext, String userId) {
        RoleScope[] scopes = { RoleScope.ORGANIZATION, RoleScope.ENVIRONMENT };
        List<RoleEntity> defaultRoleByScopes = roleService.findDefaultRoleByScopes(executionContext.getOrganizationId(), scopes);
        if (defaultRoleByScopes == null || defaultRoleByScopes.isEmpty()) {
            throw new DefaultRoleNotFoundException(scopes);
        }
        for (RoleEntity defaultRoleByScope : defaultRoleByScopes) {
            switch (defaultRoleByScope.getScope()) {
                case ORGANIZATION -> membershipService.addRoleToMemberOnReference(
                    executionContext,
                    new MembershipService.MembershipReference(MembershipReferenceType.ORGANIZATION, executionContext.getOrganizationId()),
                    new MembershipService.MembershipMember(userId, null, MembershipMemberType.USER),
                    new MembershipService.MembershipRole(RoleScope.ORGANIZATION, defaultRoleByScope.getName())
                );
                case ENVIRONMENT -> membershipService.addRoleToMemberOnReference(
                    executionContext,
                    new MembershipService.MembershipReference(
                        MembershipReferenceType.ENVIRONMENT,
                        executionContext.hasEnvironmentId() ? executionContext.getEnvironmentId() : GraviteeContext.getDefaultEnvironment()
                    ),
                    new MembershipService.MembershipMember(userId, null, MembershipMemberType.USER),
                    new MembershipService.MembershipRole(RoleScope.ENVIRONMENT, defaultRoleByScope.getName())
                );
            }
        }
    }
}
