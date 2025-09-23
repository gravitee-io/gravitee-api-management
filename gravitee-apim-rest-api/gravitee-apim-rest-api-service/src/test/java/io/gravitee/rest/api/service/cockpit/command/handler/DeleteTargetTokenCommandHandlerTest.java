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

import static io.gravitee.rest.api.service.cockpit.command.handler.TargetTokenCommandHandler.CLOUD_TOKEN_SOURCE;
import static java.util.Objects.nonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import io.gravitee.cockpit.api.command.v1.targettoken.DeleteTargetTokenCommand;
import io.gravitee.cockpit.api.command.v1.targettoken.DeleteTargetTokenCommandPayload;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteTargetTokenCommandHandlerTest {

    private static final String ORGANISATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final String COMMAND_ID = "command-id";

    @Mock
    private UserService userService;

    @InjectMocks
    private DeleteTargetTokenCommandHandler deleteTargetTokenCommandHandler;

    private DeleteTargetTokenCommand command;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(USER_ID);
    }

    @Test
    void should_delete_user() {
        command = new DeleteTargetTokenCommand(generatePayload());

        when(userService.findBySource(ORGANISATION_ID, CLOUD_TOKEN_SOURCE, COMMAND_ID, false)).thenReturn(user);

        deleteTargetTokenCommandHandler
            .handle(command)
            .test()
            .awaitDone(2, SECONDS)
            .assertComplete()
            .assertNoErrors()
            .assertValue(reply -> nonNull(reply.getCommandId()))
            .assertValue(reply -> CommandStatus.SUCCEEDED.equals(reply.getCommandStatus()));
    }

    @Test
    void should_not_failed_if_user_not_found() {
        command = new DeleteTargetTokenCommand(generatePayload());

        when(userService.findBySource(ORGANISATION_ID, CLOUD_TOKEN_SOURCE, COMMAND_ID, false)).thenThrow(
            new UserNotFoundException(user.getId())
        );

        deleteTargetTokenCommandHandler
            .handle(command)
            .test()
            .awaitDone(2, SECONDS)
            .assertComplete()
            .assertNoErrors()
            .assertValue(reply -> nonNull(reply.getCommandId()))
            .assertValue(reply -> CommandStatus.SUCCEEDED.equals(reply.getCommandStatus()));
    }

    @Test
    void should_failed_if_error_when_delete_user() {
        command = new DeleteTargetTokenCommand(generatePayload());

        when(userService.findBySource(ORGANISATION_ID, CLOUD_TOKEN_SOURCE, COMMAND_ID, false)).thenReturn(user);
        doThrow(new RuntimeException("Error during deletion"))
            .when(userService)
            .delete(new ExecutionContext(ORGANISATION_ID, ENVIRONMENT_ID), user.getId());

        deleteTargetTokenCommandHandler
            .handle(command)
            .test()
            .awaitDone(2, SECONDS)
            .assertComplete()
            .assertNoErrors()
            .assertValue(reply -> nonNull(reply.getCommandId()))
            .assertValue(reply -> CommandStatus.ERROR.equals(reply.getCommandStatus()));
    }

    private static DeleteTargetTokenCommandPayload generatePayload() {
        return DeleteTargetTokenCommandPayload.builder()
            .organizationId(ORGANISATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .id(COMMAND_ID)
            .build();
    }
}
