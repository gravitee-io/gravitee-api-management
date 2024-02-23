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
package io.gravitee.rest.api.service.cockpit.command.handler;

import static io.gravitee.rest.api.service.cockpit.command.handler.UserCommandHandler.COCKPIT_SOURCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.membership.MembershipCommand;
import io.gravitee.cockpit.api.command.v1.membership.MembershipCommandPayload;
import io.gravitee.cockpit.api.command.v1.membership.MembershipReply;
import io.gravitee.common.utils.UUID;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
    public void supportType() {
        assertEquals(CockpitCommandType.MEMBERSHIP.name(), cut.supportType());
    }

    @Test
    public void handleWithAdminRole() throws InterruptedException {
        MembershipCommandPayload membershipPayload = MembershipCommandPayload
            .builder()
            .userId("user#1")
            .organizationId("orga#1")
            .referenceType(MembershipReferenceType.ENVIRONMENT.name())
            .referenceId("env#1")
            .role("ENVIRONMENT_PRIMARY_OWNER")
            .build();

        MembershipCommand command = new MembershipCommand(membershipPayload);

        UserEntity user = new UserEntity();
        user.setId(UUID.random().toString());

        RoleEntity role = new RoleEntity();
        role.setId(UUID.random().toString());
        role.setScope(RoleScope.ENVIRONMENT);
        role.setName("ADMIN");

        when(userService.findBySource(any(), eq(COCKPIT_SOURCE), eq(membershipPayload.userId()), eq(false))).thenReturn(user);
        when(roleService.findByScopeAndName(RoleScope.ENVIRONMENT, "ADMIN", "orga#1")).thenReturn(Optional.of(role));

        TestObserver<MembershipReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertNoErrors();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));

        ArgumentCaptor<MembershipService.MembershipReference> membershipReference = ArgumentCaptor.forClass(
            MembershipService.MembershipReference.class
        );
        ArgumentCaptor<MembershipService.MembershipMember> membershipMember = ArgumentCaptor.forClass(
            MembershipService.MembershipMember.class
        );
        ArgumentCaptor<List<MembershipService.MembershipRole>> membershipRoles = ArgumentCaptor.forClass(List.class);

        verify(membershipService)
            .updateRolesToMemberOnReference(
                argThat(executionContext -> executionContext.getOrganizationId().equals("orga#1")),
                membershipReference.capture(),
                membershipMember.capture(),
                membershipRoles.capture(),
                eq(COCKPIT_SOURCE),
                eq(false)
            );

        assertEquals(MembershipReferenceType.ENVIRONMENT, membershipReference.getValue().getType());
        assertEquals(membershipPayload.referenceId(), membershipReference.getValue().getId());
        assertEquals(MembershipMemberType.USER, membershipMember.getValue().getMemberType());
        assertEquals(user.getId(), membershipMember.getValue().getMemberId());
        assertTrue(
            membershipRoles.getValue().size() == 1 &&
            membershipRoles
                .getValue()
                .stream()
                .allMatch(membershipRole -> membershipRole.getScope() == role.getScope() && membershipRole.getName().equals(role.getName()))
        );
    }

    @Test
    public void handleWithRole() throws InterruptedException {
        MembershipCommandPayload membershipPayload = MembershipCommandPayload
            .builder()
            .userId("user#1")
            .organizationId("orga#1")
            .referenceType(MembershipReferenceType.ORGANIZATION.name())
            .referenceId("orga#1")
            .role("ORGANIZATION_OWNER")
            .build();

        MembershipCommand command = new MembershipCommand(membershipPayload);

        UserEntity user = new UserEntity();
        user.setId(UUID.random().toString());

        RoleEntity role = new RoleEntity();
        role.setId(UUID.random().toString());
        role.setScope(RoleScope.ORGANIZATION);
        role.setName("ADMIN");

        when(userService.findBySource(any(), eq(COCKPIT_SOURCE), eq(membershipPayload.userId()), eq(false))).thenReturn(user);
        when(roleService.findByScopeAndName(RoleScope.ORGANIZATION, "ADMIN", "orga#1")).thenReturn(Optional.of(role));

        TestObserver<MembershipReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertNoErrors();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));

        ArgumentCaptor<MembershipService.MembershipReference> membershipReference = ArgumentCaptor.forClass(
            MembershipService.MembershipReference.class
        );
        ArgumentCaptor<MembershipService.MembershipMember> membershipMember = ArgumentCaptor.forClass(
            MembershipService.MembershipMember.class
        );
        ArgumentCaptor<List<MembershipService.MembershipRole>> membershipRoles = ArgumentCaptor.forClass(List.class);

        verify(membershipService)
            .updateRolesToMemberOnReference(
                argThat(executionContext -> executionContext.getOrganizationId().equals("orga#1")),
                membershipReference.capture(),
                membershipMember.capture(),
                membershipRoles.capture(),
                eq(COCKPIT_SOURCE),
                eq(false)
            );

        assertEquals(MembershipReferenceType.ORGANIZATION, membershipReference.getValue().getType());
        assertEquals(membershipPayload.referenceId(), membershipReference.getValue().getId());
        assertEquals(MembershipMemberType.USER, membershipMember.getValue().getMemberType());
        assertEquals(user.getId(), membershipMember.getValue().getMemberId());
        assertTrue(
            membershipRoles.getValue().size() == 1 &&
            membershipRoles
                .getValue()
                .stream()
                .allMatch(membershipRole -> membershipRole.getScope() == role.getScope() && membershipRole.getName().equals(role.getName()))
        );
    }

    @Test
    public void handleWithUserRole() throws InterruptedException {
        MembershipCommandPayload membershipPayload = MembershipCommandPayload
            .builder()
            .userId("user#1")
            .organizationId("orga#1")
            .referenceType(MembershipReferenceType.ENVIRONMENT.name())
            .referenceId("env#1")
            .role("ENVIRONMENT_USER")
            .build();

        MembershipCommand command = new MembershipCommand(membershipPayload);

        UserEntity user = new UserEntity();
        user.setId(UUID.random().toString());

        RoleEntity role = new RoleEntity();
        role.setId(UUID.random().toString());
        role.setScope(RoleScope.ENVIRONMENT);
        role.setName("USER");

        when(userService.findBySource(any(), eq(COCKPIT_SOURCE), eq(membershipPayload.userId()), eq(false))).thenReturn(user);
        when(roleService.findByScopeAndName(RoleScope.ENVIRONMENT, "USER", "orga#1")).thenReturn(Optional.of(role));

        TestObserver<MembershipReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertNoErrors();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));

        ArgumentCaptor<MembershipService.MembershipReference> membershipReference = ArgumentCaptor.forClass(
            MembershipService.MembershipReference.class
        );
        ArgumentCaptor<MembershipService.MembershipMember> membershipMember = ArgumentCaptor.forClass(
            MembershipService.MembershipMember.class
        );
        ArgumentCaptor<List<MembershipService.MembershipRole>> membershipRoles = ArgumentCaptor.forClass(List.class);

        verify(membershipService)
            .updateRolesToMemberOnReference(
                argThat(executionContext -> executionContext.getOrganizationId().equals("orga#1")),
                membershipReference.capture(),
                membershipMember.capture(),
                membershipRoles.capture(),
                eq(COCKPIT_SOURCE),
                eq(false)
            );

        assertEquals(MembershipReferenceType.ENVIRONMENT, membershipReference.getValue().getType());
        assertEquals(membershipPayload.referenceId(), membershipReference.getValue().getId());
        assertEquals(MembershipMemberType.USER, membershipMember.getValue().getMemberType());
        assertEquals(user.getId(), membershipMember.getValue().getMemberId());
        assertTrue(
            membershipRoles.getValue().size() == 1 &&
            membershipRoles
                .getValue()
                .stream()
                .allMatch(membershipRole -> membershipRole.getScope() == role.getScope() && membershipRole.getName().equals(role.getName()))
        );
    }

    @Test
    public void handleWithUnknownRole() throws InterruptedException {
        MembershipCommandPayload membershipPayload = MembershipCommandPayload
            .builder()
            .userId("user#1")
            .organizationId("orga#1")
            .referenceType(MembershipReferenceType.ENVIRONMENT.name())
            .referenceId("env#1")
            .role("UNKNOWN")
            .build();

        MembershipCommand command = new MembershipCommand(membershipPayload);

        UserEntity user = new UserEntity();
        user.setId(UUID.random().toString());

        RoleEntity role = new RoleEntity();
        role.setId(UUID.random().toString());

        when(userService.findBySource(any(), eq(COCKPIT_SOURCE), eq(membershipPayload.userId()), eq(false))).thenReturn(user);

        TestObserver<MembershipReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertNoErrors();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.ERROR));

        verifyNoInteractions(membershipService);
    }

    @Test
    public void handleWithUnknownUser() throws InterruptedException {
        MembershipCommandPayload membershipPayload = MembershipCommandPayload
            .builder()
            .userId("user#1")
            .organizationId("orga#1")
            .referenceType(MembershipReferenceType.ENVIRONMENT.name())
            .referenceId("env#1")
            .role("UNKNOWN")
            .build();

        MembershipCommand command = new MembershipCommand(membershipPayload);

        when(userService.findBySource(any(), eq(COCKPIT_SOURCE), eq(membershipPayload.userId()), eq(false)))
            .thenThrow(new UserNotFoundException(membershipPayload.userId()));

        TestObserver<MembershipReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertNoErrors();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.ERROR));

        verifyNoInteractions(roleService);
        verifyNoInteractions(membershipService);
    }
}
