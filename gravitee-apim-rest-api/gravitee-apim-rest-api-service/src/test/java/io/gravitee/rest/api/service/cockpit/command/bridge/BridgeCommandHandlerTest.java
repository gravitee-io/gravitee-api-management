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
package io.gravitee.rest.api.service.cockpit.command.bridge;

import static org.junit.Assert.assertEquals;

import io.gravitee.cockpit.api.command.legacy.bridge.BridgeMultiReply;
import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeCommand;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeCommandPayload;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeReply;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeReplyPayload;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.rest.api.service.cockpit.command.bridge.operation.BridgeOperation;
import io.gravitee.rest.api.service.cockpit.command.bridge.operation.BridgeOperationHandler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class BridgeCommandHandlerTest {

    public BridgeCommandHandler cut;

    @Before
    public void before() {
        cut = new BridgeCommandHandler(Collections.emptyList());
    }

    @Test
    public void supportType() {
        assertEquals(CockpitCommandType.BRIDGE.name(), cut.supportType());
    }

    @Test
    public void shouldNotHandleUnknownOperation() throws InterruptedException {
        BridgeCommand command = new BridgeCommand(BridgeCommandPayload.builder().operation("UNKWOWN_OPERATION").build());

        TestObserver<BridgeReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply ->
            reply.getCommandId().equals(command.getId()) &&
            reply.getCommandStatus().equals(CommandStatus.ERROR) &&
            reply.getErrorDetails().equals("No handler found for this operation: UNKWOWN_OPERATION")
        );
    }

    @Test
    public void shouldHandleListEnvironmentsOperation() throws InterruptedException {
        BridgeCommand command = new BridgeCommand(
            BridgeCommandPayload.builder().operation(BridgeOperation.LIST_ENVIRONMENT.name()).build()
        );

        cut =
            new BridgeCommandHandler(
                Arrays.asList(new TestingFakeListEnvironmentOperationHandler(), new AnotherTestingFakeOperationHandler())
            );

        TestObserver<BridgeReply> obs = cut.handle(command).test();

        obs.await();
        obs.assertValue(reply -> reply.getCommandId().equals(command.getId()) && reply.getCommandStatus().equals(CommandStatus.SUCCEEDED));
    }

    static class TestingFakeListEnvironmentOperationHandler implements BridgeOperationHandler {

        @Override
        public boolean canHandle(String bridgeOperation) {
            return BridgeOperation.LIST_ENVIRONMENT.name().equals(bridgeOperation);
        }

        @Override
        public Single<BridgeReply> handle(BridgeCommand bridgeCommand) {
            final BridgeReply reply = new BridgeReply(bridgeCommand.getId(), new BridgeReplyPayload(List.of()));
            return Single.just(reply);
        }
    }

    static class AnotherTestingFakeOperationHandler implements BridgeOperationHandler {

        @Override
        public boolean canHandle(String bridgeOperation) {
            return "ANOTHER_OPERATION".equals(bridgeOperation);
        }

        @Override
        public Single<BridgeReply> handle(BridgeCommand bridgeCommand) {
            final BridgeReply reply = new BridgeReply(bridgeCommand.getId(), new BridgeReplyPayload(List.of()));
            return Single.just(reply);
        }
    }
}
