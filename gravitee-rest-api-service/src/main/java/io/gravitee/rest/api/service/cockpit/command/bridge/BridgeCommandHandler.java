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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.cockpit.api.command.Command;
import io.gravitee.cockpit.api.command.CommandHandler;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.bridge.BridgeCommand;
import io.gravitee.cockpit.api.command.bridge.BridgeReply;
import io.gravitee.cockpit.api.command.bridge.BridgeSimpleReply;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.cockpit.command.bridge.operation.BridgeOperationHandler;
import io.gravitee.rest.api.service.cockpit.command.bridge.operation.ListEnvironmentOperationHandler;
import io.reactivex.Single;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class BridgeCommandHandler implements CommandHandler<BridgeCommand, BridgeReply>, InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(BridgeCommandHandler.class);

    List<BridgeOperationHandler> operationHandlers = new LinkedList<>();

    private EnvironmentService environmentService;
    private InstallationService installationService;
    private ObjectMapper objectMapper;

    public BridgeCommandHandler(EnvironmentService environmentService, InstallationService installationService, ObjectMapper objectMapper) {
        this.environmentService = environmentService;
        this.installationService = installationService;
        this.objectMapper = objectMapper;
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

    @Override
    public void afterPropertiesSet() {
        InstallationEntity installationEntity = installationService.getOrInitialize();
        operationHandlers.add(new ListEnvironmentOperationHandler(environmentService, installationEntity, objectMapper));
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
