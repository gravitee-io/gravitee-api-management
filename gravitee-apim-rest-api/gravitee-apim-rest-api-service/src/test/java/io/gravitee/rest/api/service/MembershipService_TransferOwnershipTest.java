/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.exceptions.ApiOwnershipTransferException;
import io.gravitee.rest.api.service.exceptions.RoleNotFoundException;
import io.gravitee.rest.api.service.impl.MembershipServiceImpl;
import java.util.Collections;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Ouahid KHELIFI (ouahid.khelifi at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class MembershipService_TransferOwnershipTest {

    private static final String API_ID = "api-id-1";
    private static final String GROUP_ID = "group-id-1";
    private static final String USER_ID = "user-id-1";
    private static final String ORGANIZATION_ID = "DEFAULT";
    private static final String ENVIRONMENT_ID = "DEFAULT";
    private static final String API_OWNER_ROLE_ID = "API_OWNER";
    private static final String API_PRIMARY_OWNER_ROLE_ID = "API_PRIMARY_OWNER";
    private static final String USER_ROLE_ID = "API_USER";
    private static final String USER_ROLE_NAME = "USER";
    private static final String OWNER_ROLE_NAME = "OWNER";
    private final RoleEntity newPrimaryOwnerRole = new RoleEntity();

    @InjectMocks
    private MembershipService membershipService = new MembershipServiceImpl();

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private UserService userService;

    @Mock
    private AuditService auditService;

    @Mock
    private ApiService apiService;

    @Before
    public void setUp() {
        newPrimaryOwnerRole.setId(USER_ROLE_ID);
        newPrimaryOwnerRole.setName(USER_ROLE_NAME);
        newPrimaryOwnerRole.setScope(RoleScope.API);

        UserEntity user = new UserEntity();
        user.setId(USER_ID);
        when(userService.findByIds(Collections.singletonList(USER_ID), false)).thenReturn(Collections.singleton(user));
        when(userService.findById(USER_ID)).thenReturn(user);

        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId(API_ID);
        when(apiService.findById(API_ID)).thenReturn(apiEntity);
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

        ApiOwnershipTransferException exception = Assert.assertThrows(
            ApiOwnershipTransferException.class,
            () ->
                this.membershipService.transferApiOwnership(
                        ORGANIZATION_ID,
                        ENVIRONMENT_ID,
                        API_ID,
                        new MembershipService.MembershipMember(GROUP_ID, null, MembershipMemberType.GROUP),
                        Collections.singletonList(newPrimaryOwnerRole)
                    )
        );

        assertEquals("Api [api-id-1] transfer not allowed.", exception.getMessage());
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

        RoleNotFoundException exception = Assert.assertThrows(
            RoleNotFoundException.class,
            () ->
                this.membershipService.transferApiOwnership(
                        ORGANIZATION_ID,
                        ENVIRONMENT_ID,
                        API_ID,
                        new MembershipService.MembershipMember(GROUP_ID, null, MembershipMemberType.GROUP),
                        Collections.singletonList(newPrimaryOwnerRole)
                    )
        );

        assertEquals("Role [API_PRIMARY_OWNER] can not be found.", exception.getMessage());
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

        RoleNotFoundException exception = Assert.assertThrows(
            RoleNotFoundException.class,
            () ->
                this.membershipService.transferApiOwnership(
                        ORGANIZATION_ID,
                        ENVIRONMENT_ID,
                        API_ID,
                        new MembershipService.MembershipMember(GROUP_ID, null, MembershipMemberType.GROUP),
                        Collections.singletonList(newPrimaryOwnerRole)
                    )
        );

        assertEquals("Role [API_PRIMARY_OWNER] can not be found.", exception.getMessage());
    }

    @Test
    public void shouldTransferOwnershipToAGroup() throws TechnicalException {
        RoleEntity primaryOwnerRole = new RoleEntity();
        primaryOwnerRole.setId(API_PRIMARY_OWNER_ROLE_ID);
        primaryOwnerRole.setScope(RoleScope.API);
        primaryOwnerRole.setName(SystemRole.PRIMARY_OWNER.name());
        when(roleService.findById(API_PRIMARY_OWNER_ROLE_ID)).thenReturn(primaryOwnerRole);
        when(roleService.findPrimaryOwnerRoleByOrganization(ORGANIZATION_ID, RoleScope.API)).thenReturn(primaryOwnerRole);
        when(roleService.findByScopeAndName(RoleScope.API, SystemRole.PRIMARY_OWNER.name())).thenReturn(Optional.of(primaryOwnerRole));
        when(roleService.findByScopeAndName(RoleScope.API, USER_ROLE_NAME)).thenReturn(Optional.of(newPrimaryOwnerRole));

        Membership poMembership = new Membership();
        poMembership.setReferenceType(MembershipReferenceType.API);
        poMembership.setRoleId(API_PRIMARY_OWNER_ROLE_ID);
        poMembership.setReferenceId(API_ID);
        poMembership.setMemberId(USER_ID);
        poMembership.setMemberType(io.gravitee.repository.management.model.MembershipMemberType.USER);
        when(membershipRepository.findByReferencesAndRoleId(MembershipReferenceType.GROUP, Collections.singletonList(GROUP_ID), null))
            .thenReturn(Collections.singleton(poMembership));
        when(membershipRepository.findByReferenceAndRoleId(MembershipReferenceType.API, API_ID, API_PRIMARY_OWNER_ROLE_ID))
            .thenReturn(Collections.singleton(poMembership));

        this.membershipService.transferApiOwnership(
                ORGANIZATION_ID,
                ENVIRONMENT_ID,
                API_ID,
                new MembershipService.MembershipMember(GROUP_ID, null, MembershipMemberType.GROUP),
                Collections.singletonList(newPrimaryOwnerRole)
            );

        ArgumentCaptor<Membership> membershipCaptor = ArgumentCaptor.forClass(Membership.class);
        verify(membershipRepository, times(2)).create(membershipCaptor.capture());
        assertEquals(membershipCaptor.getAllValues().size(), 2);

        Membership createdPoMembership = membershipCaptor.getAllValues().get(0);
        assertEquals(createdPoMembership.getRoleId(), API_PRIMARY_OWNER_ROLE_ID);
        assertEquals(createdPoMembership.getMemberId(), GROUP_ID);
        assertEquals(createdPoMembership.getReferenceId(), API_ID);

        Membership createdUserMembership = membershipCaptor.getAllValues().get(1);
        assertEquals(createdUserMembership.getRoleId(), USER_ROLE_ID);
        assertEquals(createdUserMembership.getMemberId(), USER_ID);
        assertEquals(createdUserMembership.getReferenceId(), API_ID);
    }
}
