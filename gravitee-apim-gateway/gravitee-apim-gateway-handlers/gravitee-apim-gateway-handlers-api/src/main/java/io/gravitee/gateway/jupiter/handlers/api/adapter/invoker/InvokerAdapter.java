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
package io.gravitee.gateway.jupiter.handlers.api.adapter.invoker;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.api.invoker.Invoker;
import io.gravitee.gateway.jupiter.policy.adapter.context.ExecutionContextAdapter;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specific implementation of {@link Invoker} that adapt the behavior of an existing {@link io.gravitee.gateway.api.Invoker}
 * to make it work in a reactive chain.
 * The adapter implements both {@link Invoker} and {@link io.gravitee.gateway.api.Invoker} to keep cross compatability between jupiter and v3 policies
 * that expect v3 {@link io.gravitee.gateway.api.Invoker} type.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class InvokerAdapter implements Invoker, io.gravitee.gateway.api.Invoker {

    private final Logger log = LoggerFactory.getLogger(InvokerAdapter.class);

    private final io.gravitee.gateway.api.Invoker legacyInvoker;
    private final String id;

    public InvokerAdapter(io.gravitee.gateway.api.Invoker legacyInvoker) {
        this.legacyInvoker = legacyInvoker;
        this.id = legacyInvoker.getClass().getSimpleName().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Completable invoke(RequestExecutionContext ctx) {
        final ExecutionContextAdapter adaptedCtx = ExecutionContextAdapter.create(ctx);
        return Completable
            .create(
                nextEmitter -> {
                    log.debug("Executing invoker {}", id);

                    // Http status set to 0 to reflect the fact we are waiting for the backend http status.
                    ctx.response().status(0);

                    // Stream adapter allowing to write the request content to the upstream.
                    final ReadWriteStreamAdapter streamAdapter = new ReadWriteStreamAdapter(adaptedCtx, nextEmitter);

                    // Connection handler adapter to receive the response from the invoker.
                    final ConnectionHandlerAdapter connectionHandlerAdapter = new ConnectionHandlerAdapter(ctx, nextEmitter);

                    // Assign the chunks from the connection handler to the response.
                    ctx.response().chunks(connectionHandlerAdapter.getChunks());

                    try {
                        // Invoke to make the connection happen.
                        legacyInvoker.invoke(adaptedCtx, streamAdapter, connectionHandlerAdapter);
                    } catch (Throwable t) {
                        nextEmitter.tryOnError(new Exception("An error occurred while trying to execute invoker " + id, t));
                    }
                }
            )
            .doFinally(adaptedCtx::restore)
            .onErrorResumeNext(
                throwable -> {
                    // In case of any error, make sure to reset the response content.
                    ctx.response().chunks(Flowable.empty());
                    return ctx.interruptWith(new ExecutionFailure(HttpStatusCode.BAD_GATEWAY_502));
                }
            );
    }

    @Override
    public void invoke(ExecutionContext context, ReadStream<Buffer> stream, Handler<ProxyConnection> connectionHandler) {
        legacyInvoker.invoke(context, stream, connectionHandler);
    }
}
