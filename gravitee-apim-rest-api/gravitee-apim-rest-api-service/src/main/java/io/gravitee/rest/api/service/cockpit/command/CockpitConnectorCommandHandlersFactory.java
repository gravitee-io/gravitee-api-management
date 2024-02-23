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
package io.gravitee.rest.api.service.cockpit.command;

import io.gravitee.exchange.api.command.Command;
import io.gravitee.exchange.api.command.CommandAdapter;
import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.exchange.api.command.Reply;
import io.gravitee.exchange.api.command.ReplyAdapter;
import io.gravitee.exchange.api.connector.ConnectorCommandContext;
import io.gravitee.exchange.api.connector.ConnectorCommandHandlersFactory;
import io.gravitee.exchange.api.websocket.protocol.ProtocolVersion;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component("cockpitConnectorCommandHandlersFactory")
@RequiredArgsConstructor
public class CockpitConnectorCommandHandlersFactory implements ConnectorCommandHandlersFactory {

    private final List<CommandHandler<? extends Command<?>, ? extends Reply<?>>> commandHandlers;
    private final List<CommandAdapter<? extends Command<?>, ? extends Command<?>, ? extends Reply<?>>> commandAdapters;
    private final List<ReplyAdapter<? extends Reply<?>, ? extends Reply<?>>> replyAdapters;

    @Override
    public List<CommandHandler<? extends Command<?>, ? extends Reply<?>>> buildCommandHandlers(
        final ConnectorCommandContext connectorCommandContext
    ) {
        return commandHandlers;
    }

    @Override
    public List<CommandAdapter<? extends Command<?>, ? extends Command<?>, ? extends Reply<?>>> buildCommandAdapters(
        final ConnectorCommandContext connectorCommandContext,
        final ProtocolVersion protocolVersion
    ) {
        return commandAdapters;
    }

    @Override
    public List<ReplyAdapter<? extends Reply<?>, ? extends Reply<?>>> buildReplyAdapters(
        final ConnectorCommandContext connectorCommandContext,
        final ProtocolVersion protocolVersion
    ) {
        return replyAdapters;
    }
}
