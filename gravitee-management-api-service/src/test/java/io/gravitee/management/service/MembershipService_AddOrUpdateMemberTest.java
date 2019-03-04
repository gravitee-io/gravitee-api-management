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
package io.gravitee.management.service;

import io.gravitee.management.model.GroupEntity;
import io.gravitee.management.model.MemberEntity;
import io.gravitee.management.model.RoleEntity;
import io.gravitee.management.model.UserEntity;
import io.gravitee.management.model.permissions.SystemRole;
import io.gravitee.management.service.exceptions.AlreadyPrimaryOwnerException;
import io.gravitee.management.service.exceptions.NotAuthorizedMembershipException;
import io.gravitee.management.service.exceptions.RoleNotFoundException;
import io.gravitee.management.service.impl.MembershipServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class MembershipService_AddOrUpdateMemberTest {

    private static final String GROUP_ID = "group-id-1";

    @InjectMocks
    private MembershipService membershipService = new MembershipServiceImpl();

    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private UserService userService;
    @Mock
    private EmailService emailService;
    @Mock
    private RoleService roleService;
    @Mock
    private GroupService groupService;
    @Mock
    private AuditService auditService;
    @Mock
    private IdentityService identityService;
    @Mock
    private NotifierService notifierService;

    @Test
    public void shouldAddApiGroupMembership() throws Exception {
        UserEntity userEntity = new UserEntity();
        userEntity.setId("my name");
        userEntity.setEmail("me@mail.com");
        RoleEntity role = mock(RoleEntity.class);
        Membership newMembership = new Membership();
        newMembership.setReferenceType(MembershipReferenceType.GROUP);
        newMembership.setRoles(Collections.singletonMap(RoleScope.API.getId(), "OWNER"));
        newMembership.setReferenceId(GROUP_ID);
        newMembership.setUserId(userEntity.getId());
        GroupEntity groupEntityMock = mock(GroupEntity.class);
        when(groupEntityMock.getName()).thenReturn("foo");
        when(role.getScope()).thenReturn(io.gravitee.management.model.permissions.RoleScope.API);
        when(roleService.findById(any(), any())).thenReturn(role);
        when(userService.findById(userEntity.getId())).thenReturn(userEntity);
        when(groupService.findById(GROUP_ID)).thenReturn(groupEntityMock);
        when(membershipRepository.findById(userEntity.getId(), MembershipReferenceType.GROUP, GROUP_ID)).thenReturn(empty(), of(newMembership));
        when(membershipRepository.create(any())).thenReturn(newMembership);

        membershipService.addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipUser(userEntity.getId(), null),
                new MembershipService.MembershipRole(RoleScope.API, "OWNER"));

        verify(userService, times(2)).findById(userEntity.getId());
        verify(membershipRepository, times(2)).findById(userEntity.getId(), MembershipReferenceType.GROUP, GROUP_ID);
        verify(membershipRepository, times(1)).create(any());
        verify(membershipRepository, never()).update(any());
        verify(emailService, times(1)).sendAsyncEmailNotification(any());
    }

    @Test
    public void shouldUpdateApiGroupMembership() throws Exception {
        UserEntity userEntity = new UserEntity();
        userEntity.setId("my name");
        userEntity.setEmail("me@mail.com");
        Membership membership = new Membership();
        membership.setUserId(userEntity.getId());
        membership.setReferenceType(MembershipReferenceType.GROUP);
        membership.setReferenceId(GROUP_ID);
        Map<Integer, String> roles = new HashMap<>();
        roles.put(RoleScope.API.getId(), "USER");
        membership.setRoles(roles);
        Membership newMembership = new Membership();
        newMembership.setUserId(userEntity.getId());
        newMembership.setReferenceType(MembershipReferenceType.GROUP);
        newMembership.setReferenceId(GROUP_ID);
        RoleEntity role = mock(RoleEntity.class);
        when(role.getScope()).thenReturn(io.gravitee.management.model.permissions.RoleScope.API);
        when(roleService.findById(any(), any())).thenReturn(role);
        when(userService.findById(userEntity.getId())).thenReturn(userEntity);
        when(membershipRepository.findById(userEntity.getId(), MembershipReferenceType.GROUP, GROUP_ID)).thenReturn(of(membership));
        when(membershipRepository.update(any())).thenReturn(newMembership);

        MemberEntity updateMember = membershipService.addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipUser(userEntity.getId(), null),
                new MembershipService.MembershipRole(RoleScope.API, "OWNER"));

        verify(userService, times(2)).findById(userEntity.getId());
        verify(membershipRepository, times(2)).findById(userEntity.getId(), MembershipReferenceType.GROUP, GROUP_ID);
        verify(membershipRepository, never()).create(any());
        verify(membershipRepository, times(1)).update(any());
        verify(emailService, never()).sendAsyncEmailNotification(any());
    }

    @Test(expected = RoleNotFoundException.class)
    public void shouldDisallowAddUnknownRoleOnApiGroup() throws Exception {
        when(roleService.findById(any(), any())).thenThrow(new RoleNotFoundException(RoleScope.PORTAL, "name"));
        membershipService.addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipUser("xxxxx", null),
                new MembershipService.MembershipRole(RoleScope.PORTAL, "name"));
    }

    @Test(expected = RoleNotFoundException.class)
    public void shouldDisallowAddUnknownRoleOnApplicationGroup() throws Exception {
        when(roleService.findById(any(), any())).thenThrow(new RoleNotFoundException(RoleScope.PORTAL, "name"));
        membershipService.addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipUser("xxxxx", null),
                new MembershipService.MembershipRole(RoleScope.PORTAL, "name"));
    }

    @Test(expected = NotAuthorizedMembershipException.class)
    public void shouldDisallowAddManagementRoleOnGroup() throws Exception {
        RoleEntity role = mock(RoleEntity.class);
        when(role.getScope()).thenReturn(io.gravitee.management.model.permissions.RoleScope.MANAGEMENT);
        when(roleService.findById(any(), any())).thenReturn(role);
        membershipService.addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipUser("xxxxx", null),
                new MembershipService.MembershipRole(RoleScope.MANAGEMENT, "name"));
    }

    @Test(expected = NotAuthorizedMembershipException.class)
    public void shouldDisallowAddPortalRoleOnGroup() throws Exception {
        RoleEntity role = mock(RoleEntity.class);
        when(role.getScope()).thenReturn(io.gravitee.management.model.permissions.RoleScope.PORTAL);
        when(roleService.findById(any(), any())).thenReturn(role);
        membershipService.addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipUser("xxxxx", null),
                new MembershipService.MembershipRole(RoleScope.PORTAL, "name"));
    }

    @Test(expected = NotAuthorizedMembershipException.class)
    public void shouldDisallowAddAPIPrimaryOwnerRoleOnGroup() throws Exception {
        RoleEntity role = mock(RoleEntity.class);
        when(role.getScope()).thenReturn(io.gravitee.management.model.permissions.RoleScope.API);
        when(roleService.findById(any(), any())).thenReturn(role);
        membershipService.addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipUser("xxxxx", null),
                new MembershipService.MembershipRole(RoleScope.API, "PRIMARY_OWNER"));
    }

    @Test(expected = NotAuthorizedMembershipException.class)
    public void shouldDisallowAddApplicationPrimaryOwnerRoleOnGroup() throws Exception {
        RoleEntity role = mock(RoleEntity.class);
        when(role.getScope()).thenReturn(io.gravitee.management.model.permissions.RoleScope.APPLICATION);
        when(roleService.findById(any(), any())).thenReturn(role);
        membershipService.addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipUser("xxxxx", null),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, "PRIMARY_OWNER"));
    }

    @Test(expected = AlreadyPrimaryOwnerException.class)
    public void shouldNotOverridePrimaryOwner() throws TechnicalException {
        RoleEntity role = mock(RoleEntity.class);
        when(role.getScope()).thenReturn(io.gravitee.management.model.permissions.RoleScope.API);
        when(role.getName()).thenReturn(SystemRole.PRIMARY_OWNER.name());
        when(roleService.findById(any(), any())).thenReturn(role);
        Membership membership = mock(Membership.class);
        when(membership.getUserId()).thenReturn("userId");
        when(membership.getRoles()).thenReturn(Collections.singletonMap(RoleScope.API.getId(), SystemRole.PRIMARY_OWNER.name()));
        when(membership.getReferenceId()).thenReturn("API_ID");
        when(membership.getReferenceType()).thenReturn(MembershipReferenceType.API);
        when(membershipRepository.findById("userId", MembershipReferenceType.API, "API_ID")).thenReturn(of(membership));
        UserEntity userEntity = new UserEntity();
        userEntity.setId("userId");
        userEntity.setEmail("me@mail.com");
        when(userService.findById("userId")).thenReturn(userEntity);
        membershipService.addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.API, "API_ID"),
                new MembershipService.MembershipUser("userId", null),
                new MembershipService.MembershipRole(RoleScope.API, "USER"));
    }
}
