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
package io.gravitee.gateway.reactive.handlers.api.adapter.invoker;

import io.gravitee.gateway.reactive.api.context.sync.SyncExecutionContext;
import io.gravitee.gateway.reactive.api.invoker.Invoker;
import io.gravitee.gateway.reactive.policy.adapter.context.ExecutionContextAdapter;
import io.reactivex.Completable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specific implementation of {@link Invoker} that adapt the behavior of an existing {@link io.gravitee.gateway.api.Invoker}
 * to make it work in a reactive chain.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class InvokerAdapter implements Invoker {

    private final Logger log = LoggerFactory.getLogger(InvokerAdapter.class);

    private final io.gravitee.gateway.api.Invoker legacyInvoker;
    private final String id;

    public InvokerAdapter(io.gravitee.gateway.api.Invoker legacyInvoker) {
        this.legacyInvoker = legacyInvoker;
        this.id = legacyInvoker.getClass().getSimpleName();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Completable invoke(SyncExecutionContext ctx) {
        return Completable.create(
            nextEmitter -> {
                log.debug("Executing invoker {}", id);
                final ExecutionContextAdapter ctxAdapter = ExecutionContextAdapter.create(ctx);

                // Stream adapter allowing to write the request content to the upstream.
                final ReadWriteStreamAdapter streamAdapter = new ReadWriteStreamAdapter(ctxAdapter, nextEmitter);

                // Connection handler adapter to receive the response from the invoker.
                final ConnectionHandlerAdapter connectionHandlerAdapter = new ConnectionHandlerAdapter(ctx, nextEmitter);

                // Assign the chunks from the connection handler to the response.
                ctx.response().setChunkedBody(connectionHandlerAdapter.getChunks());

                // Invoke to make the connection happen.
                legacyInvoker.invoke(ctxAdapter, streamAdapter, connectionHandlerAdapter);
            }
        );
    }
}
