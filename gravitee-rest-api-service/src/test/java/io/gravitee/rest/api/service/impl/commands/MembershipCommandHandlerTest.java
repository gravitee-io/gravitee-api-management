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
package io.gravitee.rest.api.service.impl.commands;

import io.gravitee.cockpit.api.command.Command;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.membership.MembershipCommand;
import io.gravitee.cockpit.api.command.membership.MembershipPayload;
import io.gravitee.cockpit.api.command.membership.MembershipReply;
import io.gravitee.common.utils.UUID;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Optional;

import static io.gravitee.rest.api.service.impl.commands.UserCommandHandler.COCKPIT_SOURCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class MembershipCommandHandlerTest {

    @Mock
    private UserService userService;

    @Mock
    private RoleService roleService;

    @Mock
    private MembershipService membershipService;

    public MembershipCommandHandler cut;

    @Before
    public void before() {
        cut = new MembershipCommandHandler(userService, roleService, membershipService);
    }

    @Test
    public void handleType() {
        assertEquals(Command.Type.MEMBERSHIP_COMMAND, cut.handleType());
    }

    @Test
    public void handleWithAdminRole() {

        MembershipPayload membershipPayload = new MembershipPayload();
        membershipPayload.setUserId("user#1");
        membershipPayload.setOrganizationId("orga#1");
        membershipPayload.setReferenceType(MembershipReferenceType.ENVIRONMENT.name());
        membershipPayload.setReferenceId("env#1");
        membershipPayload.setRole("ENVIRONMENT_PRIMARY_OWNER");

        MembershipCommand command = new MembershipCommand(membershipPayload);

        UserEntity user = new UserEntity();
        user.setId(UUID.random().toString());

        RoleEntity role = new RoleEntity();
        role.setId(UUID.random().toString());
        role.setScope(RoleScope.ENVIRONMENT);
        role.setName("ADMIN");

        when(userService.findBySource(COCKPIT_SOURCE, membershipPayload.getUserId(), false)).thenReturn(user);
        when(roleService.findByScopeAndName(RoleScope.ENVIRONMENT, "ADMIN")).thenReturn(Optional.of(role));

        TestObserver<MembershipReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertNoErrors();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));

        ArgumentCaptor<MembershipService.MembershipReference> membershipReference = ArgumentCaptor.forClass(MembershipService.MembershipReference.class);
        ArgumentCaptor<MembershipService.MembershipMember> membershipMember = ArgumentCaptor.forClass(MembershipService.MembershipMember.class);
        ArgumentCaptor<List<MembershipService.MembershipRole>> membershipRoles = ArgumentCaptor.forClass(List.class);

        verify(membershipService).updateRolesToMemberOnReference(membershipReference.capture(), membershipMember.capture(), membershipRoles.capture(), eq(COCKPIT_SOURCE), eq(false));

        assertEquals(MembershipReferenceType.ENVIRONMENT, membershipReference.getValue().getType());
        assertEquals(membershipPayload.getReferenceId(), membershipReference.getValue().getId());
        assertEquals(MembershipMemberType.USER, membershipMember.getValue().getMemberType());
        assertEquals(user.getId(), membershipMember.getValue().getMemberId());
        assertTrue(membershipRoles.getValue().size() == 1 && membershipRoles.getValue().stream().allMatch(membershipRole -> membershipRole.getScope() == role.getScope() && membershipRole.getName().equals(role.getName())));
    }

    @Test
    public void handleWithRole() {

        MembershipPayload membershipPayload = new MembershipPayload();
        membershipPayload.setUserId("user#1");
        membershipPayload.setOrganizationId("orga#1");
        membershipPayload.setReferenceType(MembershipReferenceType.ORGANIZATION.name());
        membershipPayload.setReferenceId("orga#1");
        membershipPayload.setRole("ORGANIZATION_OWNER");

        MembershipCommand command = new MembershipCommand(membershipPayload);

        UserEntity user = new UserEntity();
        user.setId(UUID.random().toString());

        RoleEntity role = new RoleEntity();
        role.setId(UUID.random().toString());
        role.setScope(RoleScope.ORGANIZATION);
        role.setName("ADMIN");

        when(userService.findBySource(COCKPIT_SOURCE, membershipPayload.getUserId(), false)).thenReturn(user);
        when(roleService.findByScopeAndName(RoleScope.ORGANIZATION, "ADMIN")).thenReturn(Optional.of(role));

        TestObserver<MembershipReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertNoErrors();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));

        ArgumentCaptor<MembershipService.MembershipReference> membershipReference = ArgumentCaptor.forClass(MembershipService.MembershipReference.class);
        ArgumentCaptor<MembershipService.MembershipMember> membershipMember = ArgumentCaptor.forClass(MembershipService.MembershipMember.class);
        ArgumentCaptor<List<MembershipService.MembershipRole>> membershipRoles = ArgumentCaptor.forClass(List.class);

        verify(membershipService).updateRolesToMemberOnReference(membershipReference.capture(), membershipMember.capture(), membershipRoles.capture(), eq(COCKPIT_SOURCE), eq(false));

        assertEquals(MembershipReferenceType.ORGANIZATION, membershipReference.getValue().getType());
        assertEquals(membershipPayload.getReferenceId(), membershipReference.getValue().getId());
        assertEquals(MembershipMemberType.USER, membershipMember.getValue().getMemberType());
        assertEquals(user.getId(), membershipMember.getValue().getMemberId());
        assertTrue(membershipRoles.getValue().size() == 1 && membershipRoles.getValue().stream().allMatch(membershipRole -> membershipRole.getScope() == role.getScope() && membershipRole.getName().equals(role.getName())));
    }

    @Test
    public void handleWithUserRole() {

        MembershipPayload membershipPayload = new MembershipPayload();
        membershipPayload.setUserId("user#1");
        membershipPayload.setOrganizationId("orga#1");
        membershipPayload.setReferenceType(MembershipReferenceType.ENVIRONMENT.name());
        membershipPayload.setReferenceId("env#1");
        membershipPayload.setRole("ENVIRONMENT_USER");

        MembershipCommand command = new MembershipCommand(membershipPayload);

        UserEntity user = new UserEntity();
        user.setId(UUID.random().toString());

        RoleEntity role = new RoleEntity();
        role.setId(UUID.random().toString());
        role.setScope(RoleScope.ENVIRONMENT);
        role.setName("USER");

        when(userService.findBySource(COCKPIT_SOURCE, membershipPayload.getUserId(), false)).thenReturn(user);
        when(roleService.findByScopeAndName(RoleScope.ENVIRONMENT, "USER")).thenReturn(Optional.of(role));

        TestObserver<MembershipReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertNoErrors();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));

        ArgumentCaptor<MembershipService.MembershipReference> membershipReference = ArgumentCaptor.forClass(MembershipService.MembershipReference.class);
        ArgumentCaptor<MembershipService.MembershipMember> membershipMember = ArgumentCaptor.forClass(MembershipService.MembershipMember.class);
        ArgumentCaptor<List<MembershipService.MembershipRole>> membershipRoles = ArgumentCaptor.forClass(List.class);

        verify(membershipService).updateRolesToMemberOnReference(membershipReference.capture(), membershipMember.capture(), membershipRoles.capture(), eq(COCKPIT_SOURCE), eq(false));

        assertEquals(MembershipReferenceType.ENVIRONMENT, membershipReference.getValue().getType());
        assertEquals(membershipPayload.getReferenceId(), membershipReference.getValue().getId());
        assertEquals(MembershipMemberType.USER, membershipMember.getValue().getMemberType());
        assertEquals(user.getId(), membershipMember.getValue().getMemberId());
        assertTrue(membershipRoles.getValue().size() == 1 && membershipRoles.getValue().stream().allMatch(membershipRole -> membershipRole.getScope() == role.getScope() && membershipRole.getName().equals(role.getName())));
    }

    @Test
    public void handleWithUnknownRole() {

        MembershipPayload membershipPayload = new MembershipPayload();
        membershipPayload.setUserId("user#1");
        membershipPayload.setOrganizationId("orga#1");
        membershipPayload.setReferenceType(MembershipReferenceType.ENVIRONMENT.name());
        membershipPayload.setReferenceId("env#1");
        membershipPayload.setRole("UNKNOWN");

        MembershipCommand command = new MembershipCommand(membershipPayload);

        UserEntity user = new UserEntity();
        user.setId(UUID.random().toString());

        RoleEntity role = new RoleEntity();
        role.setId(UUID.random().toString());

        when(userService.findBySource(COCKPIT_SOURCE, membershipPayload.getUserId(), false)).thenReturn(user);
        when(roleService.findByScopeAndName(RoleScope.ENVIRONMENT, "UNKNOWN")).thenReturn(Optional.empty());

        TestObserver<MembershipReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertNoErrors();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.ERROR));

        verifyZeroInteractions(membershipService);
    }

    @Test
    public void handleWithUnknownUser() {

        MembershipPayload membershipPayload = new MembershipPayload();
        membershipPayload.setUserId("user#1");
        membershipPayload.setOrganizationId("orga#1");
        membershipPayload.setReferenceType(MembershipReferenceType.ENVIRONMENT.name());
        membershipPayload.setReferenceId("env#1");
        membershipPayload.setRole("UNKNOWN");

        MembershipCommand command = new MembershipCommand(membershipPayload);

        when(userService.findBySource(COCKPIT_SOURCE, membershipPayload.getUserId(), false)).thenThrow(new UserNotFoundException(membershipPayload.getUserId()));

        TestObserver<MembershipReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertNoErrors();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.ERROR));

        verifyZeroInteractions(roleService);
        verifyZeroInteractions(membershipService);
    }
}