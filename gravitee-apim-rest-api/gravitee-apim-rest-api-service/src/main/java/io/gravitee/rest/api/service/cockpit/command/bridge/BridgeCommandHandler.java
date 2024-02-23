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

import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeCommand;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeReply;
import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.rest.api.service.cockpit.command.bridge.operation.BridgeOperationHandler;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BridgeCommandHandler implements CommandHandler<BridgeCommand, BridgeReply> {

    private List<BridgeOperationHandler> operationHandlers;

    public BridgeCommandHandler(List<BridgeOperationHandler> operationHandlers) {
        this.operationHandlers = operationHandlers;
    }

    @Override
    public String supportType() {
        return CockpitCommandType.BRIDGE.name();
    }

    @Override
    public Single<BridgeReply> handle(BridgeCommand command) {
        return operationHandlers
            .stream()
            .filter(handle -> handle.canHandle(command.getPayload().operation()))
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
            log.warn("No handler found for this operation {} ", command.getPayload().operation());
            return Single.just(
                new BridgeReply(command.getId(), "No handler found for this operation: " + command.getPayload().operation())
            );
        }
    };
}
