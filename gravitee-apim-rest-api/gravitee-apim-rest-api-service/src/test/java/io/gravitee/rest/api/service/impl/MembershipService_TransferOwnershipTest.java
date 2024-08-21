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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ApiOwnershipTransferException;
import io.gravitee.rest.api.service.exceptions.RoleNotFoundException;
import io.gravitee.rest.api.service.v4.ApiGroupService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ouahid KHELIFI (ouahid.khelifi at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class MembershipService_TransferOwnershipTest {

    private static final String API_ID = "api-id-1";
    private static final String GROUP_ID = "group-id-1";
    private static final String USER_ID = "user-id-1";
    private static final String ORGANIZATION_ID = "DEFAULT";
    private static final String ENVIRONMENT_ID = "DEFAULT";
    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
    private static final String API_OWNER_ROLE_ID = "API_OWNER";
    private static final String API_PRIMARY_OWNER_ROLE_ID = "API_PRIMARY_OWNER";
    private static final String USER_ROLE_ID = "API_USER";
    private static final String USER_ROLE_NAME = "USER";
    private static final String OWNER_ROLE_NAME = "OWNER";
    private final RoleEntity newPrimaryOwnerRole = new RoleEntity();

    private MembershipService membershipService;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private UserService userService;

    @Mock
    private AuditService auditService;

    @Mock
    private ApiSearchService apiSearchService;

    @Mock
    private ApiGroupService apiGroupService;

    @Mock
    private ApiRepository apiRepository;

    @BeforeEach
    public void setUp() throws TechnicalException {
        membershipService =
            new MembershipServiceImpl(
                null,
                userService,
                null,
                null,
                null,
                null,
                membershipRepository,
                roleService,
                null,
                null,
                apiSearchService,
                apiGroupService,
                apiRepository,
                null,
                auditService,
                null,
                null
            );
        newPrimaryOwnerRole.setId(USER_ROLE_ID);
        newPrimaryOwnerRole.setName(USER_ROLE_NAME);
        newPrimaryOwnerRole.setScope(RoleScope.API);

        UserEntity user = new UserEntity();
        user.setId(USER_ID);
        lenient()
            .when(userService.findByIds(EXECUTION_CONTEXT, Collections.singletonList(USER_ID), false))
            .thenReturn(Collections.singleton(user));
        lenient().when(userService.findById(EXECUTION_CONTEXT, USER_ID)).thenReturn(user);

        Api api = new Api();
        api.setId(API_ID);
        lenient().when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
    }

    @Test
    public void shouldThrowApiOwnershipTransferException() throws TechnicalException {
        RoleEntity ownerRole = new RoleEntity();
        ownerRole.setId(API_OWNER_ROLE_ID);
        ownerRole.setScope(RoleScope.API);
        ownerRole.setName(OWNER_ROLE_NAME);
        when(roleService.findById(API_OWNER_ROLE_ID)).thenReturn(ownerRole);

        Membership ownerMembership = new Membership();
        ownerMembership.setReferenceType(MembershipReferenceType.API);
        ownerMembership.setRoleId(API_OWNER_ROLE_ID);
        ownerMembership.setReferenceId(API_ID);
        ownerMembership.setMemberId(USER_ID);
        ownerMembership.setMemberType(io.gravitee.repository.management.model.MembershipMemberType.USER);
        when(membershipRepository.findByReferencesAndRoleId(MembershipReferenceType.GROUP, Collections.singletonList(GROUP_ID), null))
            .thenReturn(Collections.singleton(ownerMembership));

        assertThatThrownBy(() ->
                this.membershipService.transferApiOwnership(
                        new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID),
                        API_ID,
                        new MembershipService.MembershipMember(GROUP_ID, null, MembershipMemberType.GROUP),
                        Collections.singletonList(newPrimaryOwnerRole)
                    )
            )
            .isInstanceOf(ApiOwnershipTransferException.class)
            .hasMessage("Api [api-id-1] transfer not allowed.");
    }

    @Test
    public void shouldThrowRoleNotFoundExceptionForMembershipReferenceType() throws TechnicalException {
        RoleEntity primaryOwnerRole = new RoleEntity();
        primaryOwnerRole.setId(API_PRIMARY_OWNER_ROLE_ID);
        primaryOwnerRole.setScope(RoleScope.API);
        primaryOwnerRole.setName(SystemRole.PRIMARY_OWNER.name());
        when(roleService.findById(API_PRIMARY_OWNER_ROLE_ID)).thenReturn(primaryOwnerRole);

        Membership poMembership = new Membership();
        poMembership.setReferenceType(MembershipReferenceType.ENVIRONMENT);
        poMembership.setRoleId(API_PRIMARY_OWNER_ROLE_ID);
        poMembership.setReferenceId(API_ID);
        poMembership.setMemberId(USER_ID);
        poMembership.setMemberType(io.gravitee.repository.management.model.MembershipMemberType.USER);
        when(membershipRepository.findByReferencesAndRoleId(MembershipReferenceType.GROUP, Collections.singletonList(GROUP_ID), null))
            .thenReturn(Collections.singleton(poMembership));

        assertThatThrownBy(() ->
                this.membershipService.transferApiOwnership(
                        new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID),
                        API_ID,
                        new MembershipService.MembershipMember(GROUP_ID, null, MembershipMemberType.GROUP),
                        Collections.singletonList(newPrimaryOwnerRole)
                    )
            )
            .isInstanceOf(RoleNotFoundException.class)
            .hasMessage("Role [API_PRIMARY_OWNER] cannot be found.");
    }

    @Test
    public void shouldThrowRoleNotFoundExceptionForRoleEntity() throws TechnicalException {
        RoleEntity primaryOwnerRole = new RoleEntity();
        primaryOwnerRole.setId(API_PRIMARY_OWNER_ROLE_ID);
        primaryOwnerRole.setScope(RoleScope.API);
        primaryOwnerRole.setName(SystemRole.PRIMARY_OWNER.name());
        when(roleService.findById(API_PRIMARY_OWNER_ROLE_ID)).thenReturn(primaryOwnerRole);

        Membership poMembership = new Membership();
        poMembership.setReferenceType(MembershipReferenceType.API);
        poMembership.setRoleId(API_PRIMARY_OWNER_ROLE_ID);
        poMembership.setReferenceId(API_ID);
        poMembership.setMemberId(USER_ID);
        poMembership.setMemberType(io.gravitee.repository.management.model.MembershipMemberType.USER);
        when(membershipRepository.findByReferencesAndRoleId(MembershipReferenceType.GROUP, Collections.singletonList(GROUP_ID), null))
            .thenReturn(Collections.singleton(poMembership));

        assertThatThrownBy(() ->
                this.membershipService.transferApiOwnership(
                        new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID),
                        API_ID,
                        new MembershipService.MembershipMember(GROUP_ID, null, MembershipMemberType.GROUP),
                        Collections.singletonList(newPrimaryOwnerRole)
                    )
            )
            .isInstanceOf(RoleNotFoundException.class)
            .hasMessage("Role [API_PRIMARY_OWNER] cannot be found.");
    }

    @Test
    public void shouldTransferOwnershipToAGroup() throws TechnicalException {
        RoleEntity primaryOwnerRole = new RoleEntity();
        primaryOwnerRole.setId(API_PRIMARY_OWNER_ROLE_ID);
        primaryOwnerRole.setScope(RoleScope.API);
        primaryOwnerRole.setName(SystemRole.PRIMARY_OWNER.name());
        when(roleService.findById(API_PRIMARY_OWNER_ROLE_ID)).thenReturn(primaryOwnerRole);
        when(roleService.findScopeByMembershipReferenceType(any())).thenReturn(RoleScope.API);
        when(roleService.findPrimaryOwnerRoleByOrganization(ORGANIZATION_ID, RoleScope.API)).thenReturn(primaryOwnerRole);
        when(roleService.findByScopeAndName(RoleScope.API, SystemRole.PRIMARY_OWNER.name(), ORGANIZATION_ID))
            .thenReturn(Optional.of(primaryOwnerRole));
        when(roleService.findByScopeAndName(RoleScope.API, USER_ROLE_NAME, ORGANIZATION_ID)).thenReturn(Optional.of(newPrimaryOwnerRole));

        Membership poMembership = new Membership();
        poMembership.setReferenceType(MembershipReferenceType.API);
        poMembership.setRoleId(API_PRIMARY_OWNER_ROLE_ID);
        poMembership.setReferenceId(API_ID);
        poMembership.setMemberId(USER_ID);
        poMembership.setMemberType(io.gravitee.repository.management.model.MembershipMemberType.USER);
        when(membershipRepository.findByReferencesAndRoleId(MembershipReferenceType.GROUP, List.of(GROUP_ID), null))
            .thenReturn(Set.of(poMembership));
        when(membershipRepository.findByReferenceAndRoleId(MembershipReferenceType.API, API_ID, API_PRIMARY_OWNER_ROLE_ID))
            .thenReturn(Set.of(poMembership));

        membershipService.transferApiOwnership(
            EXECUTION_CONTEXT,
            API_ID,
            new MembershipService.MembershipMember(GROUP_ID, null, MembershipMemberType.GROUP),
            List.of(newPrimaryOwnerRole)
        );

        ArgumentCaptor<Membership> membershipCaptor = ArgumentCaptor.forClass(Membership.class);
        verify(membershipRepository, times(2)).create(membershipCaptor.capture());
        assertThat(membershipCaptor.getAllValues()).hasSize(2);

        Membership createdPoMembership = membershipCaptor.getAllValues().get(0);
        assertThat(createdPoMembership.getRoleId()).isEqualTo(API_PRIMARY_OWNER_ROLE_ID);
        assertThat(createdPoMembership.getMemberId()).isEqualTo(GROUP_ID);
        assertThat(createdPoMembership.getReferenceId()).isEqualTo(API_ID);

        Membership createdUserMembership = membershipCaptor.getAllValues().get(1);
        assertThat(createdUserMembership.getRoleId()).isEqualTo(USER_ROLE_ID);
        assertThat(createdUserMembership.getMemberId()).isEqualTo(USER_ID);
        assertThat(createdUserMembership.getReferenceId()).isEqualTo(API_ID);
    }
}
