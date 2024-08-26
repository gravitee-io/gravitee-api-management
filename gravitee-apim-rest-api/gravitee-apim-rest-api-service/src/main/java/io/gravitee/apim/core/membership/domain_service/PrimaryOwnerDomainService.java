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
import io.gravitee.apim.core.group.query_service.GroupQueryService;
import io.gravitee.apim.core.membership.crud_service.MembershipCrudService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.common.ReferenceContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Optional;
import java.util.function.BiFunction;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@DomainService
public class PrimaryOwnerDomainService {

    private final MembershipCrudService membershipCrudService;
    private final RoleQueryService roleQueryService;
    private final MembershipQueryService membershipQueryService;
    private final GroupQueryService groupQueryService;
    private final UserCrudService userCrudService;

    public void createIntegrationPrimaryOwnerMembership(String integrationId, PrimaryOwnerEntity primaryOwner, AuditInfo auditInfo) {
        var type = Membership.ReferenceType.INTEGRATION;
        findPrimaryOwnerRole(type, auditInfo.organizationId())
            .ifPresent(role -> {
                var membership = Membership
                    .builder()
                    .id(UuidString.generateRandom())
                    .referenceId(integrationId)
                    .referenceType(type)
                    .roleId(role.getId())
                    .memberId(primaryOwner.id())
                    .memberType(Membership.Type.valueOf(primaryOwner.type().name()))
                    .createdAt(TimeProvider.now())
                    .updatedAt(TimeProvider.now())
                    .build();
                membershipCrudService.create(membership);
            });
    }

    public Maybe<PrimaryOwnerEntity> getApplicationPrimaryOwner(final String organizationId, String applicationId) {
        return getPrimaryOwner(Membership.ReferenceType.APPLICATION, organizationId, applicationId);
    }

    public Maybe<PrimaryOwnerEntity> getIntegrationPrimaryOwner(final String organizationId, String integrationId) {
        return getPrimaryOwner(Membership.ReferenceType.INTEGRATION, organizationId, integrationId);
    }

    private Maybe<PrimaryOwnerEntity> getPrimaryOwner(Membership.ReferenceType type, final String organizationId, String integrationId) {
        return Maybe
            .fromOptional(findPrimaryOwnerRole(type, organizationId))
            .flatMap(role ->
                findPrimaryOwnerMembership(type, integrationId, role)
                    .flatMap(membership ->
                        switch (membership.getMemberType()) {
                            case USER -> findUserPrimaryOwner(membership);
                            case GROUP -> findGroupPrimaryOwner(membership, role.getId());
                        }
                    )
            );
    }

    private Optional<Role> findPrimaryOwnerRole(Membership.ReferenceType type, String organizationId) {
        BiFunction<String, ReferenceContext, Optional<Role>> find =
            switch (type) {
                case INTEGRATION -> roleQueryService::findIntegrationRole;
                case APPLICATION -> roleQueryService::findApplicationRole;
                case API -> roleQueryService::findApiRole;
                default -> {
                    log.warn("Find primary role for {} isn't managed", type);
                    yield (a, b) -> Optional.empty();
                }
            };
        return find.apply(
            SystemRole.PRIMARY_OWNER.name(),
            ReferenceContext.builder().referenceType(ReferenceContext.Type.ORGANIZATION).referenceId(organizationId).build()
        );
    }

    private Maybe<Membership> findPrimaryOwnerMembership(Membership.ReferenceType type, String referenceId, Role role) {
        return Flowable.fromIterable(membershipQueryService.findByReferenceAndRoleId(type, referenceId, role.getId())).firstElement();
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

    private Maybe<PrimaryOwnerEntity> findGroupPrimaryOwner(Membership membership, String primaryOwnerRoleId) {
        var group = Maybe.fromOptional(groupQueryService.findById(membership.getMemberId()));
        var user = findPrimaryOwnerGroupMember(membership.getMemberId(), primaryOwnerRoleId)
            .flatMap(m -> userCrudService.findBaseUserById(m.getMemberId()));

        return group.map(value ->
            PrimaryOwnerEntity
                .builder()
                .id(value.getId())
                .displayName(value.getName())
                .type(PrimaryOwnerEntity.Type.GROUP)
                .email(user.map(BaseUserEntity::getEmail).orElse(null))
                .build()
        );
    }

    private Optional<Membership> findPrimaryOwnerGroupMember(String groupId, String primaryOwnerRoleId) {
        return membershipQueryService
            .findByReferenceAndRoleId(Membership.ReferenceType.GROUP, groupId, primaryOwnerRoleId)
            .stream()
            .findFirst();
    }
}
