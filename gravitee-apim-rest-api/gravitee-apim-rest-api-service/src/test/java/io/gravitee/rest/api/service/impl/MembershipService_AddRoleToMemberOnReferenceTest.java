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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EmailService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.NotAuthorizedMembershipException;
import io.gravitee.rest.api.service.exceptions.RoleNotFoundException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class MembershipService_AddRoleToMemberOnReferenceTest {

    private static final String API_ID = "api-id-1";
    private static final String APPLICATION_ID = "application-id-1";
    private static final String GROUP_ID = "group-id-1";

    private MembershipService membershipService;

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
    private ParameterService parameterService;

    @BeforeEach
    public void setUp() throws Exception {
        membershipService =
            new MembershipServiceImpl(
                null,
                userService,
                null,
                null,
                null,
                emailService,
                membershipRepository,
                roleService,
                null,
                null,
                null,
                null,
                null,
                groupService,
                auditService,
                parameterService,
                null
            );
    }

    @Test
    public void shouldAddApiGroupMembership() throws Exception {
        RoleEntity role = mock(RoleEntity.class);
        when(role.getScope()).thenReturn(RoleScope.API);
        when(role.getName()).thenReturn("OWNER");
        when(role.getId()).thenReturn("API_OWNER");
        when(roleService.findByScopeAndName(RoleScope.API, "OWNER", GraviteeContext.getCurrentOrganization()))
            .thenReturn(Optional.of(role));

        UserEntity userEntity = new UserEntity();
        userEntity.setId("my name");
        userEntity.setEmail("me@mail.com");
        when(userService.findById(GraviteeContext.getExecutionContext(), userEntity.getId())).thenReturn(userEntity);

        Membership newMembership = new Membership();
        newMembership.setReferenceType(io.gravitee.repository.management.model.MembershipReferenceType.API);
        newMembership.setRoleId("API_OWNER");
        newMembership.setReferenceId(API_ID);
        newMembership.setMemberId(GROUP_ID);
        newMembership.setMemberType(io.gravitee.repository.management.model.MembershipMemberType.GROUP);
        when(groupService.findById(GraviteeContext.getExecutionContext(), GROUP_ID)).thenReturn(mock(GroupEntity.class));
        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                userEntity.getId(),
                io.gravitee.repository.management.model.MembershipMemberType.USER,
                io.gravitee.repository.management.model.MembershipReferenceType.GROUP,
                GROUP_ID
            )
        )
            .thenReturn(Set.of(newMembership), Collections.emptySet());
        when(membershipRepository.create(any())).thenReturn(newMembership);

        membershipService.addRoleToMemberOnReference(
            GraviteeContext.getExecutionContext(),
            new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
            new MembershipService.MembershipMember("my name", null, MembershipMemberType.USER),
            new MembershipService.MembershipRole(RoleScope.API, "OWNER")
        );

        verify(userService, times(1)).findById(GraviteeContext.getExecutionContext(), userEntity.getId());
        verify(membershipRepository, times(2))
            .findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                userEntity.getId(),
                io.gravitee.repository.management.model.MembershipMemberType.USER,
                io.gravitee.repository.management.model.MembershipReferenceType.GROUP,
                GROUP_ID
            );
        verify(membershipRepository, times(1)).create(any());
        verify(membershipRepository, never()).update(any());
        verify(emailService, times(1)).sendAsyncEmailNotification(eq(GraviteeContext.getExecutionContext()), any());
    }

    @Test
    public void shouldAddPrimaryOwnerApiGroupMembership() throws Exception {
        RoleEntity role = mock(RoleEntity.class);
        when(role.getScope()).thenReturn(RoleScope.API);
        when(role.getName()).thenReturn("PRIMARY_OWNER");
        when(role.getId()).thenReturn("API_PRIMARY_OWNER");
        when(roleService.findByScopeAndName(RoleScope.API, "PRIMARY_OWNER", GraviteeContext.getCurrentOrganization()))
            .thenReturn(Optional.of(role));

        UserEntity userEntity = new UserEntity();
        userEntity.setId("my name");
        userEntity.setEmail("me@mail.com");
        when(userService.findById(GraviteeContext.getExecutionContext(), userEntity.getId())).thenReturn(userEntity);

        Membership newMembership = new Membership();
        newMembership.setReferenceType(io.gravitee.repository.management.model.MembershipReferenceType.API);
        newMembership.setRoleId("API_PRIMARY_OWNER");
        newMembership.setReferenceId(API_ID);
        newMembership.setMemberId(GROUP_ID);
        newMembership.setMemberType(io.gravitee.repository.management.model.MembershipMemberType.GROUP);
        when(groupService.findById(GraviteeContext.getExecutionContext(), GROUP_ID)).thenReturn(mock(GroupEntity.class));
        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                userEntity.getId(),
                io.gravitee.repository.management.model.MembershipMemberType.USER,
                io.gravitee.repository.management.model.MembershipReferenceType.GROUP,
                GROUP_ID
            )
        )
            .thenReturn(Set.of(newMembership), Collections.emptySet());
        when(membershipRepository.create(any())).thenReturn(newMembership);
        when(parameterService.findAsBoolean(any(), eq(Key.TRIAL_INSTANCE), eq(ParameterReferenceType.SYSTEM))).thenReturn(false);

        membershipService.addRoleToMemberOnReference(
            GraviteeContext.getExecutionContext(),
            new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
            new MembershipService.MembershipMember("my name", null, MembershipMemberType.USER),
            new MembershipService.MembershipRole(RoleScope.API, "PRIMARY_OWNER")
        );

        verify(userService, times(1)).findById(GraviteeContext.getExecutionContext(), userEntity.getId());
        verify(membershipRepository, times(2))
            .findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                userEntity.getId(),
                io.gravitee.repository.management.model.MembershipMemberType.USER,
                io.gravitee.repository.management.model.MembershipReferenceType.GROUP,
                GROUP_ID
            );
        verify(membershipRepository, times(1)).create(any());
        verify(membershipRepository, never()).update(any());
        verify(emailService, times(1)).sendAsyncEmailNotification(eq(GraviteeContext.getExecutionContext()), any());
    }

    @Test
    public void shouldAddPrimaryOwnerApiGroupMembershipAndNotSendEmailForNonOptedInUserInTrialInstance() throws Exception {
        RoleEntity role = mock(RoleEntity.class);
        when(role.getScope()).thenReturn(RoleScope.API);
        when(role.getName()).thenReturn("PRIMARY_OWNER");
        when(role.getId()).thenReturn("API_PRIMARY_OWNER");
        when(roleService.findByScopeAndName(RoleScope.API, "PRIMARY_OWNER", GraviteeContext.getCurrentOrganization()))
            .thenReturn(Optional.of(role));

        UserEntity userEntity = new UserEntity();
        userEntity.setId("my name");
        userEntity.setEmail("me@mail.com");
        when(userService.findById(GraviteeContext.getExecutionContext(), userEntity.getId())).thenReturn(userEntity);

        Membership newMembership = new Membership();
        newMembership.setReferenceType(io.gravitee.repository.management.model.MembershipReferenceType.API);
        newMembership.setRoleId("API_PRIMARY_OWNER");
        newMembership.setReferenceId(API_ID);
        newMembership.setMemberId(GROUP_ID);
        newMembership.setMemberType(io.gravitee.repository.management.model.MembershipMemberType.GROUP);
        when(groupService.findById(GraviteeContext.getExecutionContext(), GROUP_ID)).thenReturn(mock(GroupEntity.class));
        when(
            membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                userEntity.getId(),
                io.gravitee.repository.management.model.MembershipMemberType.USER,
                io.gravitee.repository.management.model.MembershipReferenceType.GROUP,
                GROUP_ID
            )
        )
            .thenReturn(Set.of(newMembership), Collections.emptySet());
        when(membershipRepository.create(any())).thenReturn(newMembership);
        when(parameterService.findAsBoolean(any(), eq(Key.TRIAL_INSTANCE), eq(ParameterReferenceType.SYSTEM))).thenReturn(true);

        membershipService.addRoleToMemberOnReference(
            GraviteeContext.getExecutionContext(),
            new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
            new MembershipService.MembershipMember("my name", null, MembershipMemberType.USER),
            new MembershipService.MembershipRole(RoleScope.API, "PRIMARY_OWNER")
        );

        verify(userService, times(1)).findById(GraviteeContext.getExecutionContext(), userEntity.getId());
        verify(membershipRepository, times(2))
            .findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                userEntity.getId(),
                io.gravitee.repository.management.model.MembershipMemberType.USER,
                io.gravitee.repository.management.model.MembershipReferenceType.GROUP,
                GROUP_ID
            );
        verify(membershipRepository, times(1)).create(any());
        verify(membershipRepository, never()).update(any());
        verify(emailService, times(0)).sendAsyncEmailNotification(eq(GraviteeContext.getExecutionContext()), any());
    }

    @Test
    public void shouldDisallowAddUnknownRoleOnApi() throws Exception {
        when(roleService.findByScopeAndName(any(), any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                membershipService.addRoleToMemberOnReference(
                    GraviteeContext.getExecutionContext(),
                    new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                    new MembershipService.MembershipMember("xxxxx", null, MembershipMemberType.USER),
                    new MembershipService.MembershipRole(RoleScope.ENVIRONMENT, "name")
                )
            )
            .isInstanceOf(RoleNotFoundException.class);
    }

    @Test
    public void shouldDisallowAddUnknownRoleOnApplication() throws Exception {
        when(roleService.findByScopeAndName(any(), any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                membershipService.addRoleToMemberOnReference(
                    GraviteeContext.getExecutionContext(),
                    new MembershipService.MembershipReference(MembershipReferenceType.APPLICATION, APPLICATION_ID),
                    new MembershipService.MembershipMember("xxxxx", null, MembershipMemberType.USER),
                    new MembershipService.MembershipRole(RoleScope.ENVIRONMENT, "name")
                )
            )
            .isInstanceOf(RoleNotFoundException.class);
    }

    @Test
    public void shouldDisallowAddEnvironmentRoleOnGroup() throws Exception {
        RoleEntity role = mock(RoleEntity.class);
        when(role.getScope()).thenReturn(io.gravitee.rest.api.model.permissions.RoleScope.ENVIRONMENT);
        when(roleService.findByScopeAndName(any(), any(), any())).thenReturn(Optional.of(role));

        assertThatThrownBy(() ->
                membershipService.addRoleToMemberOnReference(
                    GraviteeContext.getExecutionContext(),
                    new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
                    new MembershipService.MembershipMember("xxxxx", null, MembershipMemberType.USER),
                    new MembershipService.MembershipRole(RoleScope.ENVIRONMENT, "name")
                )
            )
            .isInstanceOf(NotAuthorizedMembershipException.class);
    }

    @Test
    public void shouldDisallowAddAPIPrimaryOwnerRoleOnGroupIfAlreadyOnePrimaryOwner() throws Exception {
        RoleEntity role = mock(RoleEntity.class);
        when(role.getScope()).thenReturn(io.gravitee.rest.api.model.permissions.RoleScope.API);
        when(role.getName()).thenReturn("PRIMARY_OWNER");
        when(role.getId()).thenReturn("API_PRIMARY_OWNER");
        when(roleService.findByScopeAndName(any(), any(), any())).thenReturn(Optional.of(role));
        when(
            membershipRepository.findByReferenceAndRoleId(
                io.gravitee.repository.management.model.MembershipReferenceType.GROUP,
                GROUP_ID,
                "API_PRIMARY_OWNER"
            )
        )
            .thenReturn(Collections.singleton(mock(Membership.class)));

        assertThatThrownBy(() ->
                membershipService.addRoleToMemberOnReference(
                    GraviteeContext.getExecutionContext(),
                    new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
                    new MembershipService.MembershipMember("xxxxx", null, MembershipMemberType.USER),
                    new MembershipService.MembershipRole(RoleScope.API, "PRIMARY_OWNER")
                )
            )
            .isInstanceOf(NotAuthorizedMembershipException.class);
    }

    @Test
    public void shouldDisallowAddApplicationPrimaryOwnerRoleOnGroup() throws Exception {
        RoleEntity role = mock(RoleEntity.class);
        when(role.getScope()).thenReturn(io.gravitee.rest.api.model.permissions.RoleScope.APPLICATION);
        when(role.getName()).thenReturn("PRIMARY_OWNER");
        when(roleService.findByScopeAndName(any(), any(), any())).thenReturn(Optional.of(role));

        assertThatThrownBy(() ->
                membershipService.addRoleToMemberOnReference(
                    GraviteeContext.getExecutionContext(),
                    new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
                    new MembershipService.MembershipMember("xxxxx", null, MembershipMemberType.USER),
                    new MembershipService.MembershipRole(RoleScope.APPLICATION, "PRIMARY_OWNER")
                )
            )
            .isInstanceOf(NotAuthorizedMembershipException.class);
    }
}
