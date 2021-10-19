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
package io.gravitee.rest.api.service.cockpit.command.handler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import io.gravitee.cockpit.api.command.Command;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.designer.DeployModelCommand;
import io.gravitee.cockpit.api.command.designer.DeployModelPayload;
import io.gravitee.cockpit.api.command.designer.DeployModelReply;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.ApiServiceCockpit;
import io.gravitee.rest.api.service.UserService;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Julien GIOVARESCO (julien.giovaresco at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class DeployModelCommandHandlerTest {

    @Mock
    private UserService userService;

    @Mock
    private ApiServiceCockpit apiService;

    private DeployModelCommandHandler cut;

    @Before
    public void setUp() throws Exception {
        cut = new DeployModelCommandHandler(apiService, userService);
    }

    @Test
    public void handleType() {
        assertEquals(Command.Type.DEPLOY_MODEL_COMMAND, cut.handleType());
    }

    @Test
    public void handle() {
        DeployModelPayload payload = new DeployModelPayload();
        payload.setModelId("model#1");
        payload.setSwaggerDefinition("swagger-definition");
        payload.setUserId("cockpit_user#id");

        DeployModelCommand command = new DeployModelCommand(payload);

        UserEntity user = new UserEntity();
        user.setId("user#id");
        user.setSourceId(payload.getUserId());
        when(userService.findBySource("cockpit", payload.getUserId(), false)).thenReturn(user);

        when(apiService.createOrUpdateFromCockpit(payload.getModelId(), user.getId(), payload.getSwaggerDefinition()))
            .thenAnswer(
                i -> {
                    ApiEntity apiEntity = new ApiEntity();
                    apiEntity.setId(i.getArgument(0));
                    return apiEntity;
                }
            );

        TestObserver<DeployModelReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    @Test
    public void handleWithException() {
        DeployModelPayload payload = new DeployModelPayload();
        payload.setModelId("model#1");
        payload.setSwaggerDefinition("swagger-definition");
        payload.setUserId("user#id");

        DeployModelCommand command = new DeployModelCommand(payload);

        UserEntity user = new UserEntity();
        user.setId("user#id");
        user.setSourceId(payload.getUserId());
        when(userService.findBySource("cockpit", payload.getUserId(), false)).thenReturn(user);

        when(apiService.createOrUpdateFromCockpit(payload.getModelId(), payload.getUserId(), payload.getSwaggerDefinition()))
            .thenThrow(new RuntimeException("fake error"));

        TestObserver<DeployModelReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertNoErrors();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.ERROR));
    }
}
