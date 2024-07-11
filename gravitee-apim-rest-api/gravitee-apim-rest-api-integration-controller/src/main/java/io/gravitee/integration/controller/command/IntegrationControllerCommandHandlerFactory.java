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
package io.gravitee.integration.controller.command;

import io.gravitee.apim.core.integration.use_case.CheckIntegrationUseCase;
import io.gravitee.apim.core.integration.use_case.IngestFederatedApisUseCase;
import io.gravitee.exchange.api.command.Command;
import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.exchange.api.command.Reply;
import io.gravitee.exchange.api.controller.ControllerCommandContext;
import io.gravitee.exchange.api.controller.ControllerCommandHandlersFactory;
import io.gravitee.integration.controller.command.hello.HelloCommandHandler;
import io.gravitee.integration.controller.command.ingest.IngestCommandHandler;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IntegrationControllerCommandHandlerFactory implements ControllerCommandHandlersFactory {

    private final CheckIntegrationUseCase checkIntegrationUseCase;
    private final IngestFederatedApisUseCase ingestFederatedApisUseCase;

    @Override
    public List<CommandHandler<? extends Command<?>, ? extends Reply<?>>> buildCommandHandlers(
        final ControllerCommandContext controllerCommandContext
    ) {
        return List.of(
            new HelloCommandHandler(checkIntegrationUseCase, (IntegrationCommandContext) controllerCommandContext),
            new IngestCommandHandler(ingestFederatedApisUseCase, (IntegrationCommandContext) controllerCommandContext)
        );
    }
}
