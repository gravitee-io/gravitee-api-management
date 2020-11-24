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

import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.exceptions.NotAuthorizedMembershipException;
import io.gravitee.rest.api.service.exceptions.RoleNotFoundException;
import io.gravitee.rest.api.service.impl.MembershipServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class MembershipService_AddRoleToMemberOnReferenceTest {

    private static final String API_ID = "api-id-1";
    private static final String APPLICATION_ID = "application-id-1";
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
        RoleEntity role = mock(RoleEntity.class);
        when(role.getScope()).thenReturn(RoleScope.API);
        when(roleService.findByScopeAndName(RoleScope.API, "OWNER")).thenReturn(Optional.of(role));
       
        UserEntity userEntity = new UserEntity();
        userEntity.setId("my name");
        userEntity.setEmail("me@mail.com");
        when(userService.findById(userEntity.getId())).thenReturn(userEntity);
        
        Membership newMembership = new Membership();
        newMembership.setReferenceType(io.gravitee.repository.management.model.MembershipReferenceType.API);
        newMembership.setRoleId("API_OWNER");
        newMembership.setReferenceId(API_ID);
        newMembership.setMemberId(GROUP_ID);
        newMembership.setMemberType(io.gravitee.repository.management.model.MembershipMemberType.GROUP);
        when(groupService.findById(GROUP_ID)).thenReturn(mock(GroupEntity.class));
        when(membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(userEntity.getId(), io.gravitee.repository.management.model.MembershipMemberType.USER ,io.gravitee.repository.management.model.MembershipReferenceType.GROUP, GROUP_ID))
            .thenReturn(new HashSet<>(Arrays.asList(newMembership)), Collections.emptySet());
        when(membershipRepository.create(any())).thenReturn(newMembership);

        membershipService.addRoleToMemberOnReference(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipMember("my name", null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, "OWNER"));

        verify(userService, times(1)).findById(userEntity.getId());
        verify(membershipRepository, times(2)).findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(userEntity.getId(), io.gravitee.repository.management.model.MembershipMemberType.USER ,io.gravitee.repository.management.model.MembershipReferenceType.GROUP, GROUP_ID);
        verify(membershipRepository, times(1)).create(any());
        verify(membershipRepository, never()).update(any());
        verify(emailService, times(1)).sendAsyncEmailNotification(any(), any());
    }

    @Test(expected = RoleNotFoundException.class)
    public void shouldDisallowAddUnknownRoleOnApi() throws Exception {
        when(roleService.findByScopeAndName(any(), any())).thenReturn(Optional.empty());
        membershipService.addRoleToMemberOnReference(
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipMember("xxxxx", null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.ENVIRONMENT, "name"));
    }

    @Test(expected = RoleNotFoundException.class)
    public void shouldDisallowAddUnknownRoleOnApplication() throws Exception {
        when(roleService.findByScopeAndName(any(), any())).thenReturn(Optional.empty());
        membershipService.addRoleToMemberOnReference(
                new MembershipService.MembershipReference(MembershipReferenceType.APPLICATION, APPLICATION_ID),
                new MembershipService.MembershipMember("xxxxx", null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.ENVIRONMENT, "name"));
    }

    @Test(expected = NotAuthorizedMembershipException.class)
    public void shouldDisallowAddEnvironmentRoleOnGroup() throws Exception {
        RoleEntity role = mock(RoleEntity.class);
        when(role.getScope()).thenReturn(io.gravitee.rest.api.model.permissions.RoleScope.ENVIRONMENT);
        when(roleService.findByScopeAndName(any(), any())).thenReturn(Optional.of(role));
        membershipService.addRoleToMemberOnReference(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipMember("xxxxx", null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.ENVIRONMENT, "name"));
    }

    @Test(expected = NotAuthorizedMembershipException.class)
    public void shouldDisallowAddAPIPrimaryOwnerRoleOnGroup() throws Exception {
        RoleEntity role = mock(RoleEntity.class);
        when(role.getScope()).thenReturn(io.gravitee.rest.api.model.permissions.RoleScope.API);
        when(roleService.findByScopeAndName(any(), any())).thenReturn(Optional.of(role));
        membershipService.addRoleToMemberOnReference(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipMember("xxxxx", null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, "PRIMARY_OWNER"));
    }

    @Test(expected = NotAuthorizedMembershipException.class)
    public void shouldDisallowAddApplicationPrimaryOwnerRoleOnGroup() throws Exception {
        RoleEntity role = mock(RoleEntity.class);
        when(role.getScope()).thenReturn(io.gravitee.rest.api.model.permissions.RoleScope.APPLICATION);
        when(roleService.findByScopeAndName(any(), any())).thenReturn(Optional.of(role));
        membershipService.addRoleToMemberOnReference(
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipMember("xxxxx", null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, "PRIMARY_OWNER"));
    }
}
