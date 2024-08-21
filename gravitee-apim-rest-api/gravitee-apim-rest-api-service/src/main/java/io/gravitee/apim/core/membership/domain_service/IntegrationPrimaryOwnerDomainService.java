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
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.crud_service.MembershipCrudService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.common.ReferenceContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@DomainService
public class IntegrationPrimaryOwnerDomainService {

    private final MembershipCrudService membershipCrudService;
    private final RoleQueryService roleQueryService;
    private final MembershipQueryService membershipQueryService;
    private final UserCrudService userCrudService;

    public void createIntegrationPrimaryOwnerMembership(String integrationId, PrimaryOwnerEntity primaryOwner, AuditInfo auditInfo) {
        if (primaryOwner.type() == PrimaryOwnerEntity.Type.GROUP) {
            throw new UnsupportedOperationException(
                String.format("Group Primary Owner is not supported for integrations. Integration id: %s", integrationId)
            );
        }

        findPrimaryOwnerRole(auditInfo.organizationId())
            .ifPresent(role -> {
                var membership = Membership
                    .builder()
                    .id(UuidString.generateRandom())
                    .referenceId(integrationId)
                    .referenceType(Membership.ReferenceType.INTEGRATION)
                    .roleId(role.getId())
                    .memberId(primaryOwner.id())
                    .memberType(Membership.Type.valueOf(primaryOwner.type().name()))
                    .createdAt(TimeProvider.now())
                    .updatedAt(TimeProvider.now())
                    .build();
                membershipCrudService.create(membership);
            });
    }

    public Maybe<PrimaryOwnerEntity> getIntegrationPrimaryOwner(final String organizationId, String integrationId) {
        return Maybe
            .fromOptional(findPrimaryOwnerRole(organizationId))
            .flatMap(role ->
                findApiPrimaryOwnerMembership(integrationId, role)
                    .flatMap(membership ->
                        switch (membership.getMemberType()) {
                            case USER -> findUserPrimaryOwner(membership);
                            case GROUP -> {
                                log.error("Can't have group primary owner for Integrations");
                                yield Maybe.empty();
                            }
                        }
                    )
            );
    }

    private Optional<Role> findPrimaryOwnerRole(String organizationId) {
        return roleQueryService.findIntegrationRole(
            SystemRole.PRIMARY_OWNER.name(),
            ReferenceContext.builder().referenceType(ReferenceContext.Type.ORGANIZATION).referenceId(organizationId).build()
        );
    }

    private Maybe<Membership> findApiPrimaryOwnerMembership(String integrationId, Role role) {
        return Flowable
            .fromIterable(
                membershipQueryService.findByReferenceAndRoleId(Membership.ReferenceType.INTEGRATION, integrationId, role.getId())
            )
            .firstElement();
    }

    private Maybe<PrimaryOwnerEntity> findUserPrimaryOwner(Membership membership) {
        return Maybe
            .fromOptional(userCrudService.findBaseUserById(membership.getMemberId()))
            .map(user ->
                PrimaryOwnerEntity
                    .builder()
                    .id(user.getId())
                    .displayName(user.displayName())
                    .email(user.getEmail())
                    .type(PrimaryOwnerEntity.Type.USER)
                    .build()
            );
    }
}
