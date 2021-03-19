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
import io.gravitee.rest.api.model.UpdateUserEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
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
    public void handleCreation() {

        UserPayload userPayload = new UserPayload();
        UserCommand command = new UserCommand(userPayload);

        final String sourceId = "user#1";
        userPayload.setId(sourceId);
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

        when(userService.findBySource("cockpit", sourceId, false)).thenThrow(new UserNotFoundException(sourceId));

        when(userService.create(
                argThat(newUser -> newUser.getSourceId().equals(userPayload.getId())
                        && newUser.getSource().equals("cockpit")
                        && newUser.getFirstname().equals(userPayload.getFirstName())
                        && newUser.getLastname().equals(userPayload.getLastName())
                        && newUser.getEmail().equals(userPayload.getEmail())
                        && newUser.getPicture().equals(userPayload.getPicture())
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
    public void handleUpdate() {

        UserPayload userPayload = new UserPayload();
        UserCommand command = new UserCommand(userPayload);

        final String sourceId = "user#1";
        userPayload.setId(sourceId);
        userPayload.setOrganizationId("orga#1");
        userPayload.setUsername("New Username");
        userPayload.setFirstName("New Firstname");
        userPayload.setLastName("New Lastname");
        userPayload.setPicture("https://gravitee.io/my-new-picture");
        userPayload.setEmail("my-new-email@gravitee.io");

        HashMap<String, Object> additionalInformation = new HashMap<>();
        additionalInformation.put("info1", "new_value1");
        additionalInformation.put("new_info3", "new_value3");
        userPayload.setAdditionalInformation(additionalInformation);

        UserEntity existingCockpitUser = new UserEntity();
        existingCockpitUser.setId("apim_user#1");
        existingCockpitUser.setSourceId(sourceId);
        existingCockpitUser.setSource("cockpit");
        existingCockpitUser.setFirstname("Firstname");
        existingCockpitUser.setLastname("Lastname");
        existingCockpitUser.setEmail("email@gravitee.io");
        existingCockpitUser.setPicture("https://gravitee.io/picture");
        HashMap<String, Object> customFields = new HashMap<>();
        customFields.put("info1", "value1");
        customFields.put("info2", "value2");
        existingCockpitUser.setCustomFields(customFields);

        when(userService.findBySource("cockpit", sourceId, false)).thenReturn(existingCockpitUser);

        when(userService.update(eq("apim_user#1"),
                argThat(updatedUser -> updatedUser.getFirstname().equals(userPayload.getFirstName())
                        && updatedUser.getLastname().equals(userPayload.getLastName())
                        && updatedUser.getEmail().equals(userPayload.getEmail())
                        && updatedUser.getPicture().equals(userPayload.getPicture())
                        && updatedUser.getCustomFields().get("info1").equals(additionalInformation.get("info1"))
                        && updatedUser.getCustomFields().get("new_info3").equals(additionalInformation.get("new_info3"))
                        && updatedUser.getCustomFields().get(PICTURE).equals(userPayload.getPicture())
                        && updatedUser.getCustomFields().get(SUB).equals(userPayload.getUsername()))
                ))
                .thenReturn(new UserEntity());

        TestObserver<UserReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    public void handleWithCreateException() {

        UserPayload userPayload = new UserPayload();
        UserCommand command = new UserCommand(userPayload);

        final String sourceId = "user#1";
        userPayload.setId(sourceId);
        userPayload.setOrganizationId("orga#1");

        when(userService.findBySource("cockpit", sourceId, false)).thenThrow(new UserNotFoundException(sourceId));
        when(userService.create(any(NewExternalUserEntity.class), eq(false)))
                .thenThrow(new RuntimeException("fake error"));

        TestObserver<UserReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.ERROR));
    }

    @Test
    public void handleWithUpdateException() {

        UserPayload userPayload = new UserPayload();
        UserCommand command = new UserCommand(userPayload);

        final String sourceId = "user#1";
        userPayload.setId(sourceId);
        userPayload.setOrganizationId("orga#1");

        when(userService.findBySource("cockpit", sourceId, false)).thenReturn(new UserEntity());
        when(userService.update(any(), any(UpdateUserEntity.class)))
                .thenThrow(new RuntimeException("fake error"));

        TestObserver<UserReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.ERROR));
    }
}