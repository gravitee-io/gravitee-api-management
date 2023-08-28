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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.v4api.V4ApiCommand;
import io.gravitee.cockpit.api.command.v4api.V4ApiPayload;
import io.gravitee.cockpit.api.command.v4api.V4ApiReply;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.cockpit.services.V4ApiServiceCockpit;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ashraful Hasan (ashraful.hasan at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class V4ApiCommandHandlerTest {

    @Mock
    private UserService userService;

    @Mock
    private V4ApiServiceCockpit v4ApiServiceCockpit;

    @Mock
    private UserEntity userEntity;

    private V4ApiCommandHandler commandHandler;
    private V4ApiCommand command;
    private ApiEntity apiEntity;

    @BeforeEach
    public void setUp() throws Exception {
        command = new V4ApiCommand();
        command.setId("test-id");
        final V4ApiPayload payload = new V4ApiPayload();
        payload.setUserId("123");
        payload.setApiDefinition("any-definition");
        command.setPayload(payload);

        apiEntity = new ApiEntity();
        apiEntity.setId("test-id");
        apiEntity.setName("test-name");
        apiEntity.setApiVersion("V4");

        when(userService.findBySource(eq(GraviteeContext.getExecutionContext()), eq("cockpit"), eq(payload.getUserId()), eq(true)))
            .thenReturn(userEntity);
        when(userEntity.getId()).thenReturn("user-id");

        commandHandler = new V4ApiCommandHandler(v4ApiServiceCockpit, userService);
    }

    @Test
    public void handleSuccessfulCommand() throws InterruptedException, JsonProcessingException {
        when(v4ApiServiceCockpit.createPublishApi(anyString(), anyString())).thenReturn(Single.just(apiEntity));

        TestObserver<V4ApiReply> observer = commandHandler.handle(command).test();
        observer.await();

        observer.assertValue(reply ->
            reply.getCommandId().equals(command.getId()) &&
            reply.getCommandStatus().equals(CommandStatus.SUCCEEDED) &&
            reply.getApiId().equals("test-id") &&
            reply.getApiName().equals("test-name") &&
            reply.getApiVersion().equals("V4")
        );
        verify(v4ApiServiceCockpit, times(1)).createPublishApi(anyString(), anyString());
    }

    @Test
    public void handleException() throws Exception {
        when(v4ApiServiceCockpit.createPublishApi(anyString(), anyString())).thenThrow(new JsonProcessingException("exception") {});

        TestObserver<V4ApiReply> observer = commandHandler.handle(command).test();

        observer.await();
        observer.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.FAILED)
        );
    }
}
