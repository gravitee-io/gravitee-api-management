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
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.membership.DeleteMembershipCommand;
import io.gravitee.cockpit.api.command.v1.membership.DeleteMembershipCommandPayload;
import io.gravitee.cockpit.api.command.v1.membership.DeleteMembershipReply;
import io.gravitee.common.utils.UUID;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DeleteMembershipCommandHandlerTest {

    @Mock
    private MembershipService membershipService;

    @Mock
    private UserService userService;

    public DeleteMembershipCommandHandler cut;

    @Before
    public void before() {
        cut = new DeleteMembershipCommandHandler(membershipService, userService);
    }

    @Test
    public void supportType() {
        assertEquals(CockpitCommandType.DELETE_MEMBERSHIP.name(), cut.supportType());
    }

    @Test
    public void should_delete_reference_member() throws InterruptedException {
        UserEntity user = new UserEntity();
        user.setId(UUID.random().toString());
        DeleteMembershipCommandPayload membershipPayload = DeleteMembershipCommandPayload
            .builder()
            .userId("cockpit-user#1")
            .organizationId("orga#1")
            .referenceType(MembershipReferenceType.ENVIRONMENT.name())
            .referenceId("env#1")
            .build();
        when(userService.findBySource(membershipPayload.organizationId(), COCKPIT_SOURCE, membershipPayload.userId(), false))
            .thenReturn(user);

        DeleteMembershipCommand command = new DeleteMembershipCommand(membershipPayload);

        TestObserver<DeleteMembershipReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertNoErrors();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));

        verify(membershipService)
            .deleteReferenceMember(
                argThat(executionContext -> executionContext.getOrganizationId().equals("orga#1")),
                eq(MembershipReferenceType.ENVIRONMENT),
                eq("env#1"),
                eq(MembershipMemberType.USER),
                eq(user.getId())
            );
    }

    @Test
    public void should_handle_with_unknown_user() throws InterruptedException {
        DeleteMembershipCommandPayload membershipPayload = DeleteMembershipCommandPayload
            .builder()
            .userId("cockpit-user#1")
            .organizationId("orga#1")
            .referenceType(MembershipReferenceType.ENVIRONMENT.name())
            .referenceId("env#1")
            .build();

        when(userService.findBySource(membershipPayload.organizationId(), COCKPIT_SOURCE, membershipPayload.userId(), false))
            .thenThrow(new UserNotFoundException(membershipPayload.userId()));

        DeleteMembershipCommand command = new DeleteMembershipCommand(membershipPayload);

        TestObserver<DeleteMembershipReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertNoErrors();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.ERROR));

        verifyNoInteractions(membershipService);
    }

    @Test
    public void should_handle_with_wrong_reference_type() throws InterruptedException {
        DeleteMembershipCommandPayload membershipPayload = DeleteMembershipCommandPayload
            .builder()
            .userId("cockpit-user#1")
            .organizationId("orga#1")
            .referenceType("BAD_REF_TYPE")
            .referenceId("env#1")
            .build();

        DeleteMembershipCommand command = new DeleteMembershipCommand(membershipPayload);

        TestObserver<DeleteMembershipReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertNoErrors();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.ERROR));

        verifyNoInteractions(userService);
        verifyNoInteractions(membershipService);
    }
}
