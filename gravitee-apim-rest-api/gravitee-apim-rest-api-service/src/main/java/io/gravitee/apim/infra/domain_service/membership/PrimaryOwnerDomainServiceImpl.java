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
package io.gravitee.apim.infra.domain_service.membership;

import io.gravitee.apim.core.api.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.application.domain_service.ApplicationPrimaryOwnerDomainService;
import io.gravitee.apim.core.exception.NotFoundDomainException;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.membership.exception.ApiPrimaryOwnerNotFoundException;
import io.gravitee.apim.core.membership.exception.ApplicationPrimaryOwnerNotFoundException;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.core.user.domain_service.UserPrimaryOwnerDomainService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PrimaryOwnerDomainServiceImpl
    implements ApiPrimaryOwnerDomainService, ApplicationPrimaryOwnerDomainService, UserPrimaryOwnerDomainService {

    private final RoleRepository roleRepository;
    private final MembershipRepository membershipRepository;
    private final UserCrudService userCrudService;
    private final GroupRepository groupRepository;

    public PrimaryOwnerDomainServiceImpl(
        @Lazy RoleRepository roleRepository,
        @Lazy MembershipRepository membershipRepository,
        UserCrudService userCrudService,
        @Lazy GroupRepository groupRepository
    ) {
        this.roleRepository = roleRepository;
        this.membershipRepository = membershipRepository;
        this.userCrudService = userCrudService;
        this.groupRepository = groupRepository;
    }

    @Override
    public PrimaryOwnerEntity getApiPrimaryOwner(final String organizationId, String apiId) throws ApiPrimaryOwnerNotFoundException {
        return findPrimaryOwnerRole(organizationId, RoleScope.API)
            .flatMap(role ->
                findApiPrimaryOwnerMembership(apiId, role)
                    .flatMap(membership ->
                        switch (membership.getMemberType()) {
                            case USER -> findUserPrimaryOwner(membership.getMemberId());
                            case GROUP -> findGroupPrimaryOwner(membership, role.getId());
                        }
                    )
            )
            .orElseThrow(() -> new ApiPrimaryOwnerNotFoundException(apiId));
    }

    @Override
    public PrimaryOwnerEntity getApplicationPrimaryOwner(String organizationId, String applicationId)
        throws ApplicationPrimaryOwnerNotFoundException {
        return findPrimaryOwnerRole(organizationId, RoleScope.APPLICATION)
            .flatMap(role ->
                findApplicationPrimaryOwnerMembership(applicationId, role)
                    .flatMap(membership ->
                        switch (membership.getMemberType()) {
                            case USER -> findUserPrimaryOwner(membership.getMemberId());
                            case GROUP -> findGroupPrimaryOwner(membership, role.getId());
                        }
                    )
            )
            .orElseThrow(() -> new ApplicationPrimaryOwnerNotFoundException(applicationId));
    }

    private Optional<Role> findPrimaryOwnerRole(String organizationId, RoleScope scope) {
        try {
            return roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                scope,
                SystemRole.PRIMARY_OWNER.name(),
                organizationId,
                RoleReferenceType.ORGANIZATION
            );
        } catch (TechnicalException e) {
            log.error("An error occurs while trying to get primary owner role for [organizationId={}]", organizationId, e);
            throw new TechnicalManagementException(e);
        }
    }

    private Optional<Membership> findApiPrimaryOwnerMembership(String apiId, Role role) {
        try {
            return membershipRepository.findByReferenceAndRoleId(MembershipReferenceType.API, apiId, role.getId()).stream().findFirst();
        } catch (TechnicalException e) {
            log.error("An error occurs while trying to get primary owner for [apiId={}]", apiId, e);
            throw new TechnicalManagementException(e);
        }
    }

    private Optional<Membership> findApplicationPrimaryOwnerMembership(String applicationId, Role role) {
        try {
            return membershipRepository
                .findByReferenceAndRoleId(MembershipReferenceType.APPLICATION, applicationId, role.getId())
                .stream()
                .findFirst();
        } catch (TechnicalException e) {
            log.error("An error occurs while trying to get primary owner for [applicationId={}]", applicationId, e);
            throw new TechnicalManagementException(e);
        }
    }

    private Optional<PrimaryOwnerEntity> findUserPrimaryOwner(String userId) {
        return userCrudService
            .findBaseUserById(userId)
            .map(user ->
                PrimaryOwnerEntity.builder().id(user.getId()).displayName(user.displayName()).email(user.getEmail()).type("USER").build()
            );
    }

    private Optional<PrimaryOwnerEntity> findGroupPrimaryOwner(Membership membership, String primaryOwnerRoleId) {
        try {
            var group = groupRepository.findById(membership.getMemberId());
            var user = findPrimaryOwnerGroupMember(membership.getMemberId(), primaryOwnerRoleId)
                .flatMap(m -> userCrudService.findBaseUserById(m.getMemberId()));

            return group.map(value ->
                PrimaryOwnerEntity
                    .builder()
                    .id(value.getId())
                    .displayName(value.getName())
                    .type("GROUP")
                    .email(user.map(BaseUserEntity::getEmail).orElse(null))
                    .build()
            );
        } catch (TechnicalException e) {
            log.error("An error occurs while trying to get group [groupId={}]", membership.getMemberId(), e);
            throw new TechnicalManagementException(e);
        }
    }

    private Optional<Membership> findPrimaryOwnerGroupMember(String groupId, String primaryOwnerRoleId) {
        try {
            return membershipRepository
                .findByReferenceAndRoleId(MembershipReferenceType.GROUP, groupId, primaryOwnerRoleId)
                .stream()
                .findFirst();
        } catch (TechnicalException e) {
            log.error("An error occurs while trying to get group member for [groupId={}]", groupId, e);
            throw new TechnicalManagementException(e);
        }
    }

    @Override
    public PrimaryOwnerEntity getUserPrimaryOwner(String userId) throws TechnicalDomainException {
        return this.findUserPrimaryOwner(userId).orElseThrow(() -> new NotFoundDomainException("User not found", userId));
    }
}
