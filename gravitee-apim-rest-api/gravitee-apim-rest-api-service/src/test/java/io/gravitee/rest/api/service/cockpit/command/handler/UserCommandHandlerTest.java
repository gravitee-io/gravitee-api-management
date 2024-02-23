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

import static io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity.UserProfile.PICTURE;
import static io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity.UserProfile.SUB;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.user.UserCommand;
import io.gravitee.cockpit.api.command.v1.user.UserCommandPayload;
import io.gravitee.cockpit.api.command.v1.user.UserReply;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.rest.api.model.NewExternalUserEntity;
import io.gravitee.rest.api.model.UpdateUserEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
    public void supportType() {
        assertEquals(CockpitCommandType.USER.name(), cut.supportType());
    }

    @Test
    public void handleCreation() throws InterruptedException {
        final String sourceId = "user#1";
        HashMap<String, Object> additionalInformation = new HashMap<>();
        additionalInformation.put("info1", "value1");
        additionalInformation.put("info2", "value2");
        UserCommandPayload userPayload = UserCommandPayload
            .builder()
            .id(sourceId)
            .organizationId("orga#1")
            .username("Username")
            .firstName("Firstname")
            .lastName("Lastname")
            .picture("https://gravitee.io/my-picture")
            .email("email@gravitee.io")
            .additionalInformation(additionalInformation)
            .build();

        UserCommand command = new UserCommand(userPayload);

        when(userService.findBySource(any(), eq("cockpit"), eq(sourceId), eq(false))).thenThrow(new UserNotFoundException(sourceId));

        when(
            userService.create(
                any(),
                argThat(newUser ->
                    newUser.getSourceId().equals(userPayload.id()) &&
                    newUser.getSource().equals("cockpit") &&
                    newUser.getFirstname().equals(userPayload.firstName()) &&
                    newUser.getLastname().equals(userPayload.lastName()) &&
                    newUser.getEmail().equals(userPayload.email()) &&
                    newUser.getPicture().equals(userPayload.picture()) &&
                    newUser.getCustomFields().get("info1").equals(additionalInformation.get("info1")) &&
                    newUser.getCustomFields().get("info2").equals(additionalInformation.get("info2")) &&
                    newUser.getCustomFields().get(PICTURE).equals(userPayload.picture()) &&
                    newUser.getCustomFields().get(SUB).equals(userPayload.username())
                ),
                eq(false)
            )
        )
            .thenReturn(new UserEntity());

        TestObserver<UserReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    public void handleUpdate() throws InterruptedException {
        final String sourceId = "user#1";
        HashMap<String, Object> additionalInformation = new HashMap<>();
        additionalInformation.put("info1", "new_value1");
        additionalInformation.put("new_info3", "new_value3");
        UserCommandPayload userPayload = UserCommandPayload
            .builder()
            .id(sourceId)
            .organizationId("orga#1")
            .username("New Username")
            .firstName("New Firstname")
            .lastName("New Lastname")
            .picture("https://gravitee.io/my-new-picture")
            .email("my-new-email@gravitee.io")
            .additionalInformation(additionalInformation)
            .build();
        UserCommand command = new UserCommand(userPayload);

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

        when(userService.findBySource(any(), eq("cockpit"), eq(sourceId), eq(false))).thenReturn(existingCockpitUser);

        when(
            userService.update(
                any(),
                eq("apim_user#1"),
                argThat(updatedUser ->
                    updatedUser.getFirstname().equals(userPayload.firstName()) &&
                    updatedUser.getLastname().equals(userPayload.lastName()) &&
                    updatedUser.getEmail().equals(userPayload.email()) &&
                    updatedUser.getPicture().equals(userPayload.picture()) &&
                    updatedUser.getCustomFields().get("info1").equals(additionalInformation.get("info1")) &&
                    updatedUser.getCustomFields().get("new_info3").equals(additionalInformation.get("new_info3")) &&
                    updatedUser.getCustomFields().get(PICTURE).equals(userPayload.picture()) &&
                    updatedUser.getCustomFields().get(SUB).equals(userPayload.username())
                )
            )
        )
            .thenReturn(new UserEntity());

        TestObserver<UserReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    public void handleWithCreateException() throws InterruptedException {
        final String sourceId = "user#1";
        UserCommandPayload userPayload = UserCommandPayload.builder().id(sourceId).organizationId("orga#1").build();
        UserCommand command = new UserCommand(userPayload);

        when(userService.findBySource(any(), eq("cockpit"), eq(sourceId), eq(false))).thenThrow(new UserNotFoundException(sourceId));
        when(userService.create(any(), any(NewExternalUserEntity.class), eq(false))).thenThrow(new RuntimeException("fake error"));

        TestObserver<UserReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.ERROR));
    }

    @Test
    public void handleWithUpdateException() throws InterruptedException {
        final String sourceId = "user#1";
        UserCommandPayload userPayload = UserCommandPayload.builder().id(sourceId).organizationId("orga#1").build();
        UserCommand command = new UserCommand(userPayload);

        when(userService.findBySource(any(), eq("cockpit"), eq(sourceId), eq(false))).thenReturn(new UserEntity());
        when(userService.update(any(), any(), any(UpdateUserEntity.class))).thenThrow(new RuntimeException("fake error"));

        TestObserver<UserReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.ERROR));
    }
}
