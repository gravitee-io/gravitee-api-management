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

import io.gravitee.cockpit.api.command.Command;
import io.gravitee.cockpit.api.command.CommandHandler;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.bridge.BridgeCommand;
import io.gravitee.cockpit.api.command.bridge.BridgeReply;
import io.gravitee.cockpit.api.command.bridge.BridgeSimpleReply;
import io.gravitee.rest.api.service.cockpit.command.bridge.operation.BridgeOperationHandler;
import io.reactivex.Single;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class BridgeCommandHandler implements CommandHandler<BridgeCommand, BridgeReply> {

    private final Logger logger = LoggerFactory.getLogger(BridgeCommandHandler.class);

    private List<BridgeOperationHandler> operationHandlers;

    public BridgeCommandHandler(List<BridgeOperationHandler> operationHandlers) {
        this.operationHandlers = operationHandlers;
    }

    @Override
    public Command.Type handleType() {
        return Command.Type.BRIDGE_COMMAND;
    }

    @Override
    public Single<BridgeReply> handle(BridgeCommand command) {
        return operationHandlers
            .stream()
            .filter(handle -> handle.canHandle(command.getOperation()))
            .findFirst()
            .orElse(noOperationHandler)
            .handle(command);
    }

    private final BridgeOperationHandler noOperationHandler = new BridgeOperationHandler() {
        @Override
        public boolean canHandle(String bridgeOperation) {
            return true;
        }

        @Override
        public Single<BridgeReply> handle(BridgeCommand command) {
            logger.warn("No handler found for this operation {} ", command.getOperation());
            return Single.just(
                new BridgeSimpleReply(
                    command.getId(),
                    CommandStatus.ERROR,
                    "No handler found for this operation: " + command.getOperation()
                )
            );
        }
    };
}
