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
package io.gravitee.apim.core.membership.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.group.query_service.GroupQueryService;
import io.gravitee.apim.core.membership.exception.NoPrimaryOwnerGroupForUserException;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import io.gravitee.apim.core.parameters.model.ParameterContext;
import io.gravitee.apim.core.parameters.query_service.ParametersQueryService;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.model.settings.ApiPrimaryOwnerMode;
import io.gravitee.rest.api.service.common.ReferenceContext;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

@DomainService
@AllArgsConstructor
public class IntegrationPrimaryOwnerFactory {

    private final MembershipQueryService membershipQueryService;
    private final ParametersQueryService parametersQueryService;
    private final RoleQueryService roleQueryService;
    private final UserCrudService userCrudService;
    private final GroupQueryService groupQueryService;

    public PrimaryOwnerEntity createForNewIntegration(String organizationId, String environmentId, String userId) {
        var mode = ApiPrimaryOwnerMode.valueOf(
            parametersQueryService.findAsString(
                Key.API_PRIMARY_OWNER_MODE,
                new ParameterContext(environmentId, organizationId, ParameterReferenceType.ENVIRONMENT)
            )
        );
        return switch (mode) {
            case HYBRID, USER -> initUserPrimaryOwner(userId);
            case GROUP -> initWithFirstGroupWhereUserIsPrimaryOwner(userId, organizationId);
        };
    }

    private PrimaryOwnerEntity initUserPrimaryOwner(String userId) {
        var user = userCrudService.getBaseUser(userId);
        return new PrimaryOwnerEntity(user.getId(), user.getEmail(), user.displayName(), PrimaryOwnerEntity.Type.USER);
    }

    private PrimaryOwnerEntity initWithFirstGroupWhereUserIsPrimaryOwner(String userId, String organizationId) {
        var userGroupIds = membershipQueryService
            .findGroupsThatUserBelongsTo(userId)
            .stream()
            .map(Membership::getReferenceId)
            .collect(Collectors.toSet());
        var group = groupQueryService
            .findByIds(userGroupIds)
            .stream()
            .filter(g -> g.getApiPrimaryOwner() != null && !g.getApiPrimaryOwner().isBlank())
            .findFirst();

        return group
            .flatMap(g ->
                membershipQueryService
                    .findByReferenceAndRoleId(Membership.ReferenceType.GROUP, g.getId(), getApiPrimaryOwnerRole(organizationId).getId())
                    .stream()
                    .findFirst()
                    .map(membership -> userCrudService.getBaseUser(membership.getMemberId()))
                    .map(user -> new PrimaryOwnerEntity(g.getId(), user.getEmail(), g.getName(), PrimaryOwnerEntity.Type.GROUP))
            )
            .orElseThrow(() -> new NoPrimaryOwnerGroupForUserException(userId));
    }

    private Role getApiPrimaryOwnerRole(String organizationId) {
        return roleQueryService.getApiRole(
            SystemRole.PRIMARY_OWNER.name(),
            ReferenceContext.builder().referenceType(ReferenceContext.Type.ORGANIZATION).referenceId(organizationId).build()
        );
    }
}
