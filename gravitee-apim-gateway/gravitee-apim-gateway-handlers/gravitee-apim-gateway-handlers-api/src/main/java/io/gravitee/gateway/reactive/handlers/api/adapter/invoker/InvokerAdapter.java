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
package io.gravitee.gateway.reactive.handlers.api.adapter.invoker;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.invoker.HttpInvoker;
import io.gravitee.gateway.reactive.api.invoker.Invoker;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionFailureException;
import io.gravitee.gateway.reactive.policy.adapter.context.ExecutionContextAdapter;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.Action;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specific implementation of {@link Invoker} that adapt the behavior of an existing {@link io.gravitee.gateway.api.Invoker}
 * to make it work in a reactive chain.
 * The adapter implements both {@link Invoker} and {@link io.gravitee.gateway.api.Invoker} to keep cross compatability between v4 emulation and v3 policies
 * that expect v3 {@link io.gravitee.gateway.api.Invoker} type.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class InvokerAdapter implements HttpInvoker, Invoker, io.gravitee.gateway.api.Invoker {

    private static final Logger log = LoggerFactory.getLogger(InvokerAdapter.class);
    static final String GATEWAY_CLIENT_CONNECTION_ERROR = "GATEWAY_CLIENT_CONNECTION_ERROR";
    static final String CLIENT_ABORTED_DURING_RESPONSE_ERROR = "CLIENT_ABORTED_DURING_RESPONSE_ERROR";
    static final String CLIENT_ABORTED_DURING_RESPONSE_ERROR_MESSAGE =
        "The response cannot be sent to the client because the client has aborted";

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
    public Completable invoke(ExecutionContext executionContext) {
        return invoke((HttpExecutionContext) executionContext);
    }

    @Override
    public Completable invoke(HttpExecutionContext ctx) {
        final ExecutionContextAdapter adaptedCtx = ExecutionContextAdapter.create(ctx);

        // APIM-14749: restore must land BEFORE the terminal event reaches the downstream (doOnTerminate, not doFinally),
        // as the adapted context is shared with the v3 policies of the chain. The two legs below do not guard each
        // other like doFinally does, so make sure a dispose arriving after termination does not restore a second time.
        final AtomicBoolean restored = new AtomicBoolean();
        final Action restoreOnce = () -> {
            if (restored.compareAndSet(false, true)) {
                adaptedCtx.restore();
            }
        };
        return Completable.create(nextEmitter -> {
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
                invoke(adaptedCtx, streamAdapter, connectionHandlerAdapter);
            } catch (Throwable t) {
                nextEmitter.tryOnError(new Exception("An error occurred while trying to execute invoker " + id, t));
            }
        })
            .doOnTerminate(restoreOnce)
            .doOnDispose(() -> {
                if (ctx.response().status() == 0) {
                    ctx.response().status(499);
                    ctx.metrics().setErrorKey(CLIENT_ABORTED_DURING_RESPONSE_ERROR);
                    ctx.metrics().setErrorMessage(CLIENT_ABORTED_DURING_RESPONSE_ERROR_MESSAGE);
                }
                restoreOnce.run();
            })
            .onErrorResumeNext(throwable -> {
                // In case of any error, make sure to reset the response content.
                ctx.response().chunks(Flowable.empty());

                if (throwable instanceof InterruptionFailureException) {
                    return ctx.interruptWith(((InterruptionFailureException) throwable).getExecutionFailure());
                } else {
                    log.error("An error occurred when invoking the backend.", throwable);
                    return ctx.interruptWith(
                        new ExecutionFailure(HttpStatusCode.BAD_GATEWAY_502).key(GATEWAY_CLIENT_CONNECTION_ERROR).cause(throwable)
                    );
                }
            });
    }

    @Override
    public void invoke(
        io.gravitee.gateway.api.ExecutionContext context,
        ReadStream<Buffer> stream,
        Handler<ProxyConnection> connectionHandler
    ) {
        legacyInvoker.invoke(context, stream, connectionHandler);
    }
}
