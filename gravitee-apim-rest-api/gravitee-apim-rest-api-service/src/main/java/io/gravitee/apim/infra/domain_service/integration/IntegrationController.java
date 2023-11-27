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
package io.gravitee.apim.infra.domain_service.integration;

import io.gravitee.common.service.AbstractService;
import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.integration.api.DeploymentType;
import io.gravitee.integration.api.command.IntegrationCommand;
import io.gravitee.integration.api.command.IntegrationCommandType;
import io.gravitee.integration.api.command.IntegrationReply;
import io.gravitee.integration.api.command.unknown.UnknownReply;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Service
public class IntegrationController extends AbstractService<IntegrationController> {

    private final Map<IntegrationCommandType, CommandHandler<IntegrationCommand<?>, IntegrationReply>> integrationCommandHandlers;

    public IntegrationController(Map<IntegrationCommandType, CommandHandler<IntegrationCommand<?>, IntegrationReply>> integrationCommandHandlers) {
        this.integrationCommandHandlers = integrationCommandHandlers;
    }

    @Override
    public void doStart() throws Exception {
        super.doStart();

        log.info("IntegrationController started");
    }

    public Maybe<IntegrationReply> sendCommand(IntegrationCommand<?> command, String integrationId, DeploymentType deploymentType) {
        //Check deployment type (embedded or remote)
        if (deploymentType == DeploymentType.EMBEDDED) {
            return sendEmbedded(command, integrationId);
        } else {
            return sendRemote(command, integrationId);
        }
    }

    private Maybe<IntegrationReply> sendRemote(IntegrationCommand<?> command, String integrationId) {
        //Get channel manager: channelManager.send(command, integrationId);
        return Maybe.empty();
    }

    private Maybe<IntegrationReply> sendEmbedded(IntegrationCommand<?> command, String integrationId) {
        IntegrationCommandType commandType = IntegrationCommandType.valueOf(command.getType());
        CommandHandler<IntegrationCommand<?>, IntegrationReply> commandHandler = integrationCommandHandlers.get(commandType);
        if (commandHandler != null) {
            return commandHandler.handle(command).toMaybe();
        } else {
            log.info("No handler found for command type {}. Ignoring.", command.getType());
            return Maybe.just(new UnknownReply(command.getId()));
        }
    }

    public void handleClusteredCommand() {}

    private Maybe<IntegrationReply> reply(IntegrationReply integrationReply) {
        return Maybe.empty();
    }
}
