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
package io.gravitee.rest.api.service.cockpit.command.bridge;

import static io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity.UserProfile.PICTURE;
import static io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity.UserProfile.SUB;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.cockpit.api.command.Command;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.bridge.BridgeCommand;
import io.gravitee.cockpit.api.command.bridge.BridgeMultiReply;
import io.gravitee.cockpit.api.command.bridge.BridgeReply;
import io.gravitee.cockpit.api.command.user.UserCommand;
import io.gravitee.cockpit.api.command.user.UserPayload;
import io.gravitee.cockpit.api.command.user.UserReply;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.rest.api.model.NewExternalUserEntity;
import io.gravitee.rest.api.model.UpdateUserEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.cockpit.command.bridge.operation.BridgeOperation;
import io.gravitee.rest.api.service.cockpit.command.bridge.operation.BridgeOperationHandler;
import io.gravitee.rest.api.service.cockpit.command.bridge.operation.ListEnvironmentOperationHandler;
import io.gravitee.rest.api.service.cockpit.command.handler.UserCommandHandler;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import java.util.HashMap;
import java.util.List;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class BridgeCommandHandlerTest {

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private InstallationService installationService;

    private ObjectMapper objectMapper = new GraviteeMapper();

    public BridgeCommandHandler cut;

    @Before
    public void before() {
        cut = new BridgeCommandHandler(environmentService, installationService, objectMapper);
    }

    @Test
    public void testAvailableOperationHandlers() {
        cut.afterPropertiesSet();

        final List<BridgeOperationHandler> operationHandlers = cut.operationHandlers;
        assertNotNull(operationHandlers);
        assertEquals(1, operationHandlers.size());
        assertEquals(ListEnvironmentOperationHandler.class, operationHandlers.get(0).getClass());
    }

    @Test
    public void handleType() {
        assertEquals(Command.Type.BRIDGE_COMMAND, cut.handleType());
    }

    @Test
    public void shouldNotHandleUnknownOperation() {
        BridgeCommand command = new BridgeCommand();
        command.setOperation("UNKWOWN_OPERATION");

        TestObserver<BridgeReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertValue(
            reply ->
                reply.getCommandId().equals(command.getId()) &&
                reply.getCommandStatus().equals(CommandStatus.ERROR) &&
                reply.getMessage().equals("No handler found for this operation: UNKWOWN_OPERATION")
        );
    }

    @Test
    public void shouldHandleListEnvironmentsOperation() {
        BridgeCommand command = new BridgeCommand();
        command.setOperation(BridgeOperation.LIST_ENVIRONMENT.name());

        cut.operationHandlers.clear();
        cut.operationHandlers.add(
            new BridgeOperationHandler() {
                @Override
                public boolean canHandle(String bridgeOperation) {
                    return true;
                }

                @Override
                public Single<BridgeReply> handle(BridgeCommand bridgeCommand) {
                    final BridgeMultiReply reply = new BridgeMultiReply();
                    reply.setCommandId(command.getId());
                    reply.setCommandStatus(CommandStatus.SUCCEEDED);
                    reply.setMessage("Fake operation handler");
                    return Single.just(reply);
                }
            }
        );

        TestObserver<BridgeReply> obs = cut.handle(command).test();

        obs.awaitTerminalEvent();
        obs.assertValue(
            reply ->
                reply.getCommandId().equals(command.getId()) &&
                reply.getCommandStatus().equals(CommandStatus.SUCCEEDED) &&
                reply.getMessage().equals("Fake operation handler")
        );
    }
}
