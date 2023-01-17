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
package io.gravitee.repository.bridge.server.handler;

import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.model.Command;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.WorkerExecutor;
import io.vertx.ext.web.RoutingContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CommandsHandler extends AbstractHandler {

    @Autowired
    private CommandRepository commandRepository;

    public CommandsHandler(WorkerExecutor bridgeWorkerExecutor) {
        super(bridgeWorkerExecutor);
    }

    public void create(RoutingContext ctx) {
        bridgeWorkerExecutor.executeBlocking(
            promise -> {
                try {
                    Command command = ctx.getBodyAsJson().mapTo(Command.class);
                    promise.complete(commandRepository.create(command));
                } catch (Exception ex) {
                    LOGGER.error("Unable to create a command", ex);
                    promise.fail(ex);
                }
            },
            false,
            (Handler<AsyncResult<Command>>) command -> handleResponse(ctx, command)
        );
    }
}
