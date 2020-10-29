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
import io.gravitee.cockpit.api.command.user.UserCommand;
import io.gravitee.cockpit.api.command.user.UserPayload;
import io.gravitee.cockpit.api.command.user.UserReply;
import io.gravitee.rest.api.model.NewExternalUserEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.UserService;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;

import static io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity.UserProfile.PICTURE;
import static io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity.UserProfile.SUB;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class UserCommandHandlerTest {

    @Mock
    private UserService userService;

    public UserCommandHandler cut;

    @Before
    public void before() {
        cut = new UserCommandHandler(userService);
    }

    @Test
    public void handleType() {
        assertEquals(Command.Type.USER_COMMAND, cut.handleType());
    }

    @Test
    public void handle() {

        UserPayload userPayload = new UserPayload();
        UserCommand command = new UserCommand(userPayload);

        userPayload.setId("user#1");
        userPayload.setOrganizationId("orga#1");
        userPayload.setUsername("Username");
        userPayload.setFirstName("Firstname");
        userPayload.setLastName("Lastname");
        userPayload.setPicture("https://gravitee.io/my-picture");
        userPayload.setEmail("email@gravitee.io");

        HashMap<String, Object> additionalInformation = new HashMap<>();
        additionalInformation.put("info1", "value1");
        additionalInformation.put("info2", "value2");
        userPayload.setAdditionalInformation(additionalInformation);

        when(userService.create(
                argThat(newUser -> newUser.getSourceId().equals(userPayload.getId())
                        && newUser.getSource().equals("cockpit")
                        && newUser.getFirstname().equals(userPayload.getFirstName())
                        && newUser.getLastname().equals(userPayload.getLastName())
                        && newUser.getEmail().equals(userPayload.getEmail())
                        && newUser.getCustomFields().get("info1").equals(additionalInformation.get("info1"))
                        && newUser.getCustomFields().get("info2").equals(additionalInformation.get("info2"))
                        && newUser.getCustomFields().get(PICTURE).equals(userPayload.getPicture())
                        && newUser.getCustomFields().get(SUB).equals(userPayload.getUsername())),
                eq(false)))
                .thenReturn(new UserEntity());

        TestObserver<UserReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    public void handleWithException() {

        UserPayload userPayload = new UserPayload();
        UserCommand command = new UserCommand(userPayload);

        userPayload.setId("user#1");
        userPayload.setOrganizationId("orga#1");

        when(userService.create(any(NewExternalUserEntity.class), eq(false)))
                .thenThrow(new RuntimeException("fake error"));

        TestObserver<UserReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.ERROR));
    }
}