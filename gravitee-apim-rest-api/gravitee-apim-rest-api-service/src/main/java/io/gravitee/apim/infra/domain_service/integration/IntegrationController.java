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
import io.gravitee.integration.api.DeploymentType;
import io.gravitee.integration.api.command.Command;
import io.gravitee.integration.api.command.CommandHandler;
import io.gravitee.integration.api.command.Reply;
import io.gravitee.integration.api.command.ignored.IgnoredReply;
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

    private final Map<Command.Type, CommandHandler<Command<?>, Reply>> integrationCommandHandlers;

    public IntegrationController(Map<Command.Type, CommandHandler<Command<?>, Reply>> integrationCommandHandlers) {
        this.integrationCommandHandlers = integrationCommandHandlers;
    }

    @Override
    public void doStart() throws Exception {
        super.doStart();

        log.info("IntegrationController started");
    }

    public Maybe<Reply> sendCommand(Command<?> command, String integrationId, DeploymentType deploymentType) {
        //Check deployment type (embedded or remote)
        if (deploymentType == DeploymentType.EMBEDDED) {
            return sendEmbedded(command, integrationId);
        } else {
            return sendRemote(command, integrationId);
        }
    }

    private Maybe<Reply> sendRemote(Command<?> command, String integrationId) {
        //Get channel manager: channelManager.send(command, integrationId);
        return Maybe.empty();
    }

    private Maybe<Reply> sendEmbedded(Command<?> command, String integrationId) {
        CommandHandler<Command<?>, Reply> commandHandler = integrationCommandHandlers.get(command.getType());
        if (commandHandler != null) {
            return commandHandler.handle(command).toMaybe();
        } else {
            log.info("No handler found for command type {}. Ignoring.", command.getType());
            return Maybe.just(new IgnoredReply(command.getId()));
        }
    }

    public void handleClusteredCommand() {}

    private Maybe<Reply> reply(Reply reply) {
        return Maybe.empty();
    }
}
